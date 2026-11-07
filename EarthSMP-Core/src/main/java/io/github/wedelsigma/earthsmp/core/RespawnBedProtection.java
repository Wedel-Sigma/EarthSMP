package io.github.wedelsigma.earthsmp.core;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.block.Action;

/** During a siege, only beds actually assigned as a player's respawn point
 * (and both supporting blocks) are immune to breaking and physical destruction.
 *
 * Source of truth is vanilla playerdata, not a second persistent database.
 * The offline players' respawn positions are loaded on plugin startup, so
 * offline players retain protection through restarts.
 */
public final class RespawnBedProtection implements Listener {
    private record BedPoint(String world, int x, int y, int z) {}

    private final EarthSmpCorePlugin plugin;
    private final Map<UUID, BedPoint> respawns = new HashMap<>();

    RespawnBedProtection(EarthSmpCorePlugin plugin) {
        this.plugin = plugin;
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) refresh(offline);
    }

    private void refresh(OfflinePlayer player) {
        try {
            Location at = player.getRespawnLocation(false); // no chunk loading for offline users
            UUID uuid = player.getUniqueId();
            if (at == null || at.getWorld() == null) {
                respawns.remove(uuid);
            } else {
                respawns.put(uuid, new BedPoint(at.getWorld().getName(),
                        at.getBlockX(), at.getBlockY(), at.getBlockZ()));
            }
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, "Cannot read player bed respawn location", ex);
        }
    }

    private void refreshNextTick(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> { if (player.isOnline()) refresh(player); });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) { refreshNextTick(event.getPlayer()); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBedEnter(PlayerBedEnterEvent event) { refreshNextTick(event.getPlayer()); }

    // The spawn point can be updated even when the player is not allowed to sleep.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBedUse(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null
                && event.getClickedBlock().getBlockData() instanceof Bed) {
            refreshNextTick(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) { refreshNextTick(event.getPlayer()); }

    /** True only when a registered spawn bed is inside its OWN nation's core during siege. */
    private boolean isProtected(Block target) {
        // Cheap lookup first: physics events are extremely frequent. A normal block
        // far from a registered respawn bed must not scan all protection cores.
        var relevant = respawns.entrySet().stream().filter(assigned -> {
            BedPoint p = assigned.getValue();
            return p.world.equals(target.getWorld().getName())
                    && Math.abs(p.x - target.getX()) <= 2
                    && Math.abs(p.z - target.getZ()) <= 2
                    && (target.getY() == p.y || target.getY() == p.y - 1);
        }).toList();
        if (relevant.isEmpty()) return false;
        var core = plugin.getCoreProtection().findAt(target);
        if (core.isEmpty() || !plugin.getSieges().isActiveForCore(core.get())) return false;
        for (var assigned : relevant) {
            BedPoint point = assigned.getValue();
            Block first = target.getWorld().getBlockAt(point.x, point.y, point.z);
            if (!(first.getBlockData() instanceof Bed bed)) continue;
            BlockFace towardsOther = bed.getPart() == Bed.Part.HEAD
                    ? bed.getFacing().getOppositeFace() : bed.getFacing();
            Block second = first.getRelative(towardsOther);
            if (!(second.getBlockData() instanceof Bed secondBed)
                    || secondBed.getPart() == bed.getPart()) continue;
            if (!core.get().contains(first.getX(), first.getZ())
                    || !core.get().contains(second.getX(), second.getZ())) continue;
            if (!RespawnBedRules.protects(target.getX(), target.getY(), target.getZ(),
                    first.getX(), first.getY(), first.getZ(),
                    second.getX(), second.getY(), second.getZ())) continue;
            try {
                var owner = plugin.getNationDatabase().findMembership(assigned.getKey());
                if (owner.isPresent() && core.get().nationId().equals(owner.get().nationId())) return true;
            } catch (SQLException exception) {
                // A DB failure must not permit destroying an assigned bed.
                plugin.getLogger().log(Level.SEVERE, "Bed protection ownership lookup failed", exception);
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!isProtected(event.getBlock())) return;
        event.setCancelled(true);
        event.getPlayer().sendActionBar(Component.text(
                "To łóżko odrodzenia jest chronione podczas oblężenia.", NamedTextColor.RED));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isProtected);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isProtected);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block ->
                isProtected(block) || isProtected(block.getRelative(event.getDirection()))))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block ->
                isProtected(block) || isProtected(block.getRelative(event.getDirection()))))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (isProtected(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent event) {
        if (isProtected(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChange(EntityChangeBlockEvent event) {
        if (isProtected(event.getBlock())) event.setCancelled(true);
    }
}
