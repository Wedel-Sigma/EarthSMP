package io.github.wedelsigma.earthsmp.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Global PvP combat tagging. Only accepted, positive player-caused damage starts or renews a tag.
 * Quitting while tagged is a death, not a prevented connection close (which vanilla cannot do).
 * A durable pending penalty is a fallback if the death event cannot run during PlayerQuitEvent.
 * All callbacks and state changes happen on the Paper main thread.
 */
public final class CombatTagListener implements Listener {
    private final EarthSmpCorePlugin plugin;
    private final Path pendingFile;
    private final Map<UUID, Long> tagged = new HashMap<>();
    private final Set<UUID> pendingDeaths = new HashSet<>();

    CombatTagListener(EarthSmpCorePlugin plugin) throws IOException {
        this.plugin = plugin;
        this.pendingFile = plugin.getDataFolder().toPath().resolve("combat-logout-pending.properties");
        readPending();
    }

    private void readPending() throws IOException {
        if (!Files.exists(pendingFile)) return;
        Properties entries = new Properties();
        try (InputStream input = Files.newInputStream(pendingFile)) { entries.load(input); }
        for (String uuid : entries.stringPropertyNames()) {
            try { if ("death".equals(entries.getProperty(uuid))) pendingDeaths.add(UUID.fromString(uuid)); }
            catch (IllegalArgumentException ignored) { }
        }
    }

    private void persistPending() {
        try {
            Files.createDirectories(pendingFile.getParent());
            Properties entries = new Properties();
            for (UUID id : pendingDeaths) entries.setProperty(id.toString(), "death");
            Path temporary = Files.createTempFile(pendingFile.getParent(), ".combat-log-", ".tmp");
            try {
                try (OutputStream output = Files.newOutputStream(temporary)) {
                    entries.store(output, "Kabina Narodow 2.0 - outstanding combat logout deaths");
                }
                try {
                    Files.move(temporary, pendingFile, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary, pendingFile, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally { Files.deleteIfExists(temporary); }
        } catch (IOException error) {
            plugin.getLogger().log(Level.SEVERE, "Cannot persist combat logout death penalty", error);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = PvpProtectionListener.attackingPlayer(event.getDamager());
        if (attacker == null || attacker.equals(victim) || event.getFinalDamage() <= 0.0) return;
        long until = CombatTagRules.expiresAt(System.currentTimeMillis());
        tagged.put(attacker.getUniqueId(), until);
        tagged.put(victim.getUniqueId(), until);
        attacker.sendActionBar(Component.text("Walka PvP: nie wychodź przez 60 s!", NamedTextColor.RED));
        victim.sendActionBar(Component.text("Walka PvP: nie wychodź przez 60 s!", NamedTextColor.RED));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        UUID id = event.getEntity().getUniqueId();
        tagged.remove(id);
        // Normal death or a forced quit-death completes the penalty; never punish twice on join.
        if (pendingDeaths.remove(id)) persistPending();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        Long until = tagged.remove(id);
        // Administrative /stop must not kill an entire army just because the server closed.
        if (Bukkit.isStopping() || !plugin.isEnabled() || until == null
                || !CombatTagRules.isTagged(System.currentTimeMillis(), until) || player.isDead()) return;
        if (pendingDeaths.add(id)) persistPending(); // write before trying to kill; safe across crashes
        plugin.getLogger().warning("Combat logout: " + player.getName() + " disconnected while PvP-tagged; death penalty");
        // Minecraft cannot prevent disconnect. Trigger normal death/drop handling if the entity
        // is still active; pendingDeaths guarantees it will happen on next join otherwise.
        try {
            if (player.getHealth() > 0.0) player.setHealth(0.0);
        } catch (RuntimeException error) {
            plugin.getLogger().log(Level.WARNING,
                    "Could not complete combat death on quit; it will be applied on next join", error);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!pendingDeaths.contains(player.getUniqueId())) return;
        player.sendMessage(Component.text("Rozłączyłeś się w trakcie PvP. Kara: śmierć i utrata przedmiotów.", NamedTextColor.RED));
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || !pendingDeaths.contains(player.getUniqueId())) return;
            if (player.isDead()) {
                // The previous death completed while disconnecting; don't kill twice.
                pendingDeaths.remove(player.getUniqueId());
                persistPending();
            } else {
                // PlayerDeathEvent clears pendingDeaths after a successful fallback death.
                player.setHealth(0.0);
            }
        });
    }

    /** Every 20 ticks. Players see a countdown, but expired tags are not saved or enforced. */
    public void tick() {
        long now = System.currentTimeMillis();
        tagged.entrySet().removeIf(entry -> !CombatTagRules.isTagged(now, entry.getValue()));
        for (var entry : tagged.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline() && !player.isDead()) {
                int seconds = CombatTagRules.remainingSeconds(now, entry.getValue());
                player.sendActionBar(Component.text("PvP: " + seconds + " s | Nie wychodź z serwera!",
                        NamedTextColor.RED));
            }
        }
    }

    public void shutdown() {
        tagged.clear();
        persistPending();
    }
}
