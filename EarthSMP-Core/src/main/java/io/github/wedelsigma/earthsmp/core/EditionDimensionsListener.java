package io.github.wedelsigma.earthsmp.core;

import java.sql.SQLException;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/** All routes into locked dimensions, not just Nether and End portal events. */
public final class EditionDimensionsListener implements Listener {
    private final EarthSmpCorePlugin plugin;

    public EditionDimensionsListener(EarthSmpCorePlugin plugin) { this.plugin = plugin; }

    private int currentDay() {
        try {
            return plugin.getServerClock().day(plugin.getNationDatabase().editionStart());
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "Edition day unavailable: dimension access denied", exception);
            return 0; // Fail closed
        }
    }

    private int netherUnlockDay() {
        return Math.max(1, plugin.getConfig().getInt("progression.nether-unlock-day", 10));
    }

    private boolean permitted(World world) {
        return world != null && EditionDimensionRules.allowed(
                world.getEnvironment().name(), currentDay(), netherUnlockDay());
    }

    private void blocked(Player player, World target) {
        String reason = target.getEnvironment() == World.Environment.THE_END
                ? "End jest zamkniety przez cala edycje."
                : "Nether jest zablokowany do dnia " + netherUnlockDay() + " (zmiana dnia: 06:00 PL).";
        player.sendActionBar(Component.text(reason, NamedTextColor.RED));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Location target = event.getTo();
        if (target == null || target.getWorld() == null) return;
        if (!permitted(target.getWorld())) {
            event.setCancelled(true);
            blocked(event.getPlayer(), target.getWorld());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        // Some portal handlers do not resolve the target until the portal event is processed.
        var cause = event.getCause();
        boolean fromNormal = event.getFrom().getWorld() != null &&
                event.getFrom().getWorld().getEnvironment() == World.Environment.NORMAL;
        if (fromNormal && cause == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Component.text("End jest zamknięty przez całą edycję.", NamedTextColor.RED));
            return;
        }
        if (fromNormal && cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL &&
                !EditionDimensionRules.allowed("NETHER", currentDay(), netherUnlockDay())) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Component.text("Nether od dnia " + netherUnlockDay() + " (06:00 PL).",
                    NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Location respawn = event.getRespawnLocation();
        if (!permitted(respawn.getWorld())) {
            Location fallback = safeOverworld();
            if (fallback != null) event.setRespawnLocation(fallback);
        }
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) { scheduleReturn(event.getPlayer()); }
    @EventHandler public void onWorldChanged(PlayerChangedWorldEvent event) { scheduleReturn(event.getPlayer()); }

    private void scheduleReturn(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || permitted(player.getWorld())) return;
            Location fallback = safeOverworld();
            if (fallback == null) {
                plugin.getLogger().severe("No normal-world spawn for locked-dimension return: " + player.getName());
                player.kick(Component.text("Nie mozna znalezc swiata glownego. Powiadom administracje.", NamedTextColor.RED));
                return;
            }
            // A locked dimension must never be usable after a restart or admin teleport.
            player.teleport(fallback);
            player.sendMessage(Component.text("Powrot na Overworld: ten wymiar jest zablokowany.", NamedTextColor.YELLOW));
        });
    }

    private Location safeOverworld() {
        World earth = Bukkit.getWorld(plugin.getConfig().getString("world-name", "Earth"));
        if (earth != null && earth.getEnvironment() == World.Environment.NORMAL) return earth.getSpawnLocation();
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.NORMAL) return world.getSpawnLocation();
        }
        return null;
    }
}
