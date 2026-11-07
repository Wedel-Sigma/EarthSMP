package io.github.wedelsigma.earthsmp.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.title.Title;
import java.time.Duration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

/** War respawn timer starts after the Respawn button, pauses offline, survives restart.
 * Only called on the main thread. Does not alter drops/keepInventory.
 */
public final class WarRespawnListener implements Listener {
    private final EarthSmpCorePlugin plugin;
    private final Path stateFile;
    private final Map<UUID, Integer> pending = new HashMap<>();
    private final Map<UUID, Integer> remaining = new HashMap<>();
    // Native Minecraft Title: large seconds in the centre and a label below.
    // No resource pack or client mod required.
    private static final Title.Times TITLE_TIMES = Title.Times.times(
            Duration.ZERO, Duration.ofMillis(1400), Duration.ZERO);


    public WarRespawnListener(EarthSmpCorePlugin plugin) throws IOException {
        this.plugin = plugin;
        this.stateFile = plugin.getDataFolder().toPath().resolve("war-respawn-locks.properties");
        load();
    }

    private void load() throws IOException {
        if (!Files.exists(stateFile)) return;
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(stateFile)) { props.load(in); }
        for (String key : props.stringPropertyNames()) {
            try {
                if (key.length() < 3) continue;
                UUID uuid = UUID.fromString(key.substring(2));
                int seconds = Integer.parseInt(props.getProperty(key));
                if (seconds < 1 || seconds > 50) continue;
                if (key.startsWith("p.")) pending.put(uuid, seconds);
                if (key.startsWith("l.")) remaining.put(uuid, seconds);
            } catch (IllegalArgumentException ignored) { // old/corrupt entries are ignored
            }
        }
    }

    private void save() {
        try {
            Files.createDirectories(stateFile.getParent());
            Properties props = new Properties();
            pending.forEach((id, seconds) -> props.setProperty("p." + id, Integer.toString(seconds)));
            remaining.forEach((id, seconds) -> props.setProperty("l." + id, Integer.toString(seconds)));
            Path tmp = Files.createTempFile(stateFile.getParent(), ".war-respawn-", ".tmp");
            try {
                try (OutputStream output = Files.newOutputStream(tmp)) {
                    props.store(output, "Kabina Narodow 2.0 - war respawn locks; do not edit while Paper is running");
                }
                try {
                    Files.move(tmp, stateFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                    Files.move(tmp, stateFile, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally { Files.deleteIfExists(tmp); }
        } catch (IOException error) {
            plugin.getLogger().log(Level.SEVERE, "Cannot persist war respawn timers", error);
        }
    }

    public boolean isLocked(Player player) {
        return remaining.containsKey(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID id = player.getUniqueId();
        if (isLocked(player)) clearOverlay(player);
        remaining.remove(id);
        pending.remove(id);
        try {
            var membership = plugin.getNationDatabase().findMembership(id);
            if (membership.isPresent() && plugin.getWars().nationAtWar(membership.get().nationId())) {
                Set<UUID> members = new HashSet<>();
                for (var member : plugin.getNationDatabase().findMembers(membership.get().nationId()))
                    members.add(member.playerUuid());
                int online = 0;
                for (Player p : Bukkit.getOnlinePlayers()) if (members.contains(p.getUniqueId())) online++;
                pending.put(id, WarRespawnRules.secondsFor(online));
            }
        } catch (SQLException error) {
            // Fail closed: DB errors must not let the player skip a possible war lock.
            pending.put(id, 50);
            plugin.getLogger().log(Level.SEVERE, "Could not determine war respawn delay; using 50s", error);
        }
        save();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        Integer seconds = pending.remove(id);
        if (seconds == null) return;
        remaining.put(id, seconds); // immediately prevent movement/interactions after respawn
        save();
        Bukkit.getScheduler().runTask(plugin, () -> show(event.getPlayer()));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (pending.containsKey(id) && !player.isDead()) {
            remaining.put(id, pending.remove(id)); // server may have restarted before respawn was acknowledged
            save();
        }
        if (remaining.containsKey(id)) Bukkit.getScheduler().runTask(plugin, () -> show(player));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (isLocked(event.getPlayer())) clearOverlay(event.getPlayer());
        // Offline players are not decremented by tick(); their remaining time is frozen.
        save();
    }

    private void show(Player player) {
        UUID id = player.getUniqueId();
        if (!player.isOnline() || player.isDead()) return;
        Integer seconds = remaining.get(id);
        if (seconds == null) return;
        // Title takes full centre screen. The number is intentionally in TITLE,
        // because Minecraft renders the subtitle with a smaller font.
        player.showTitle(Title.title(
                Component.text(Integer.toString(seconds), NamedTextColor.RED),
                Component.text("Odrodzisz się za:", NamedTextColor.GOLD),
                TITLE_TIMES));
        player.sendActionBar(Component.text(
                "Blokada odrodzenia: nie możesz jeszcze działać", NamedTextColor.RED));
    }

    private void clearOverlay(Player player) {
        // Do not clear all titles on join/quit if nothing was pending for this player.
        player.clearTitle();
    }

    /** Run every 20 server ticks. Timers only count down for online, spawned players. */
    public void tick() {
        boolean changed = false;
        for (var entry : Map.copyOf(remaining).entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline() || player.isDead()) continue;
            int seconds = entry.getValue();
            if (seconds <= 1) {
                remaining.remove(entry.getKey());
                clearOverlay(player);
                player.showTitle(Title.title(
                        Component.text("GO!", NamedTextColor.GREEN),
                        Component.text("Możesz już działać", NamedTextColor.YELLOW),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(750), Duration.ofMillis(200))));
            } else {
                int next = seconds - 1;
                remaining.put(entry.getKey(), next);
                show(player);
            }
            changed = true;
        }
        if (changed) save();
    }

    public void shutdown() {
        for (Player p : Bukkit.getOnlinePlayers()) if (isLocked(p)) clearOverlay(p);
        save();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnyDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player p && isLocked(p)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAttacking(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p && isLocked(p)) event.setCancelled(true);
        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player p && isLocked(p)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectile(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player p && isLocked(p)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!isLocked(event.getPlayer()) || event.getTo() == null) return;
        var from = event.getFrom();
        var to = event.getTo();
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())
            event.setTo(from);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (isLocked(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (isLocked(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Component.text("Poczekaj na koniec odrodzenia.", NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) { if (isLocked(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) { if (isLocked(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) { if (isLocked(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player p && isLocked(p)) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player p && isLocked(p)) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player p && isLocked(p)) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) { if (isLocked(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) { if (isLocked(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) { if (isLocked(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player p && isLocked(p)) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) { if (isLocked(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) { if (isLocked(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHeld(PlayerItemHeldEvent event) { if (isLocked(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) { if (isLocked(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBed(PlayerBedEnterEvent event) { if (isLocked(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicle(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player p && isLocked(p)) event.setCancelled(true);
    }
}
