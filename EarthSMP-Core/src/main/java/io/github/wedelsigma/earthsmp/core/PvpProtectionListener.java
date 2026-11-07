package io.github.wedelsigma.earthsmp.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;

/** Peace protection is checked on the MAIN Paper thread using current membership/war state. */
public final class PvpProtectionListener implements Listener {
    private record Shield(long activatedAt, long until) {}
    private final EarthSmpCorePlugin plugin;
    private final Path stateFile;
    private final Map<UUID, Shield> shields = new HashMap<>();
    private final Map<UUID, Long> lastActivation = new HashMap<>();

    PvpProtectionListener(EarthSmpCorePlugin plugin) throws IOException {
        this.plugin = plugin;
        this.stateFile = plugin.getDataFolder().toPath().resolve("pvp-shields.properties");
        load();
    }

    private void load() throws IOException {
        if (!Files.exists(stateFile)) return;
        Properties props = new Properties();
        try (InputStream stream = Files.newInputStream(stateFile)) { props.load(stream); }
        long now = System.currentTimeMillis();
        for (String key : props.stringPropertyNames()) {
            try {
                if (key.length() < 3) continue;
                UUID id = UUID.fromString(key.substring(2));
                long millis = Long.parseLong(props.getProperty(key));
                if (key.startsWith("a.") && millis + PvpRules.COOLDOWN_MILLIS > now)
                    lastActivation.put(id, millis);
                if (key.startsWith("s.") && millis > now)
                    shields.put(id, new Shield(millis - PvpRules.SHIELD_MILLIS, millis));
            } catch (IllegalArgumentException ignored) { }
        }
    }

    private void save() {
        try {
            Files.createDirectories(stateFile.getParent());
            Properties props = new Properties();
            long now = System.currentTimeMillis();
            lastActivation.entrySet().removeIf(e -> e.getValue() + PvpRules.COOLDOWN_MILLIS <= now);
            lastActivation.forEach((id, millis) -> props.setProperty("a." + id, Long.toString(millis)));
            shields.forEach((id, shield) -> {
                if (shield.until() > now) props.setProperty("s." + id, Long.toString(shield.until()));
            });
            Path tmp = Files.createTempFile(stateFile.getParent(), ".pvp-shield-", ".tmp");
            try {
                try (OutputStream stream = Files.newOutputStream(tmp)) {
                    props.store(stream, "Kabina Narodow 2.0 - PvP shield cooldowns");
                }
                try { Files.move(tmp, stateFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE); }
                catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                    Files.move(tmp, stateFile, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally { Files.deleteIfExists(tmp); }
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Cannot save PvP shield cooldowns", ex);
        }
    }

    static Player attackingPlayer(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) return player;
        }
        if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player player) return player;
        if (damager instanceof Tameable pet && pet.getOwner() instanceof Player player) return player;
        return null;
    }

    private static boolean inOwnCore(Location at, String nation, EarthSmpCorePlugin plugin) {
        if (nation == null) return false;
        return plugin.getCoreProtection().findAt(at)
                .map(c -> nation.equals(c.nationId())).orElse(false);
    }

    private boolean onOwnClaim(Location at, String nation) throws SQLException {
        if (nation == null) return false;
        return plugin.getNationDatabase().findTerritory(at.getWorld().getName(),
                Math.floorDiv(at.getBlockX(), 16), Math.floorDiv(at.getBlockZ(), 16))
                .map(t -> nation.equals(t.nationId())).orElse(false);
    }

    private boolean active(UUID uuid, long now) {
        Shield shield = shields.get(uuid);
        if (shield == null) return false;
        if (!PvpRules.shieldActive(now, shield.until())) {
            shields.remove(uuid);
            return false;
        }
        return true;
    }

    /** True means hostile action must be cancelled; this method also manages 8s shield state. */
    boolean deny(Player attacker, Player victim) {
        if (attacker.equals(victim)) return false;
        long now = System.currentTimeMillis();
        try {
            String attackerNation = plugin.getNationDatabase().findMembership(attacker.getUniqueId())
                    .map(NationDatabase.Membership::nationId).orElse(null);
            String victimNation = plugin.getNationDatabase().findMembership(victim.getUniqueId())
                    .map(NationDatabase.Membership::nationId).orElse(null);
            // Only active, opposing sides of the SAME war bypass protection.
            boolean opposed = attackerNation != null && victimNation != null
                    && plugin.getWars().opposed(attackerNation, victimNation);
            if (opposed) {
                Shield removedAttacker = shields.remove(attacker.getUniqueId());
                Shield removedVictim = shields.remove(victim.getUniqueId());
                // Do not delete cooldown: it must still apply if war ends during its duration.
                if (removedAttacker != null || removedVictim != null) save();
                return false;
            }
            if (active(attacker.getUniqueId(), now)) {
                shields.remove(attacker.getUniqueId());
                save();
                attacker.sendActionBar(Component.text("Tarcza zakończona: zaatakowałeś gracza.", NamedTextColor.YELLOW));
                return true;
            }
            if (inOwnCore(victim.getLocation(), victimNation, plugin)
                    || inOwnCore(attacker.getLocation(), attackerNation, plugin)) return true;
            if (active(victim.getUniqueId(), now)) return true;
            if (onOwnClaim(victim.getLocation(), victimNation)
                    && PvpRules.mayActivate(now, lastActivation.getOrDefault(victim.getUniqueId(), Long.MIN_VALUE))) {
                lastActivation.put(victim.getUniqueId(), now);
                shields.put(victim.getUniqueId(), new Shield(now, PvpRules.activeUntil(now)));
                save();
                victim.sendActionBar(Component.text("Atak! Tarcza terytorialna: 8 s.", NamedTextColor.GOLD));
                attacker.sendActionBar(Component.text("Trafienie zablokowane: tarcza terytorialna.", NamedTextColor.YELLOW));
                return true;
            }
            return false;
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "PvP access check failed; cancelling hit", exception);
            return true; // deny on DB errors
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = attackingPlayer(event.getDamager());
        if (attacker != null && deny(attacker, victim)) event.setCancelled(true);
    }

    /** Shooting while shielded immediately ends the shield, not at arrow impact time. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        // Ender pearls are travel, and beneficial potions are support, not a PvP attack.
        if (event.getEntity() instanceof EnderPearl) return;
        if (event.getEntity() instanceof ThrownPotion potion
                && potion.getEffects().stream().noneMatch(effect -> harmfulEffect(effect.getType().getKey().getKey()))) return;
        if (!(event.getEntity().getShooter() instanceof Player shooter)) return;
        UUID id = shooter.getUniqueId();
        if (!active(id, System.currentTimeMillis())) return;
        shields.remove(id);
        save();
        event.setCancelled(true);
        shooter.sendActionBar(Component.text("Tarcza zakończona: wystrzeliłeś pocisk.", NamedTextColor.YELLOW));
    }

    /** Prevent splash damage/debuffs circumventing peace protection. Exempt beneficial effects. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSplash(PotionSplashEvent event) {
        if (!(event.getPotion().getShooter() instanceof Player attacker)) return;
        boolean harmful = event.getPotion().getEffects().stream()
                .anyMatch(effect -> harmfulEffect(effect.getType().getKey().getKey()));
        if (!harmful) return;
        for (var affected : event.getAffectedEntities()) {
            if (affected instanceof Player player && deny(attacker, player)) {
                event.setIntensity(player, 0.0);
            }
        }
    }

    private static boolean harmfulEffect(String type) {
        return switch (type) {
            case "instant_damage", "poison", "wither", "weakness", "slowness", "blindness",
                    "mining_fatigue", "nausea", "darkness", "levitation", "hunger" -> true;
            default -> false;
        };
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLingering(AreaEffectCloudApplyEvent event) {
        if (!(event.getEntity().getSource() instanceof Player attacker)) return;
        boolean customHarmful = event.getEntity().getCustomEffects().stream()
                .anyMatch(effect -> harmfulEffect(effect.getType().getKey().getKey()));
        var base = event.getEntity().getBasePotionType();
        boolean baseHarmful = base != null && base.getPotionEffects().stream()
                .anyMatch(effect -> harmfulEffect(effect.getType().getKey().getKey()));
        if (!customHarmful && !baseHarmful) return;
        event.getAffectedEntities().removeIf(target -> target instanceof Player player && deny(attacker, player));
    }

    public void shutdown() {
        save();
        shields.clear();
        lastActivation.clear();
    }
}
