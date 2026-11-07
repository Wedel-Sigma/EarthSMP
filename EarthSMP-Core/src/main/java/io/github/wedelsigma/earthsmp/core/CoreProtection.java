package io.github.wedelsigma.earthsmp.core;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.event.block.Action;

/** Preliminary protection events. Siege and advanced bypass checks will be added later. */
public final class CoreProtection implements Listener {
    private final EarthSmpCorePlugin plugin;
    private List<NationDatabase.ProtectionCore> cores = List.of();

    public CoreProtection(EarthSmpCorePlugin plugin) throws SQLException {
        this.plugin = plugin;
        reload();
    }

    public void reload() throws SQLException {
        cores = List.copyOf(plugin.getNationDatabase().allProtectionCores());
    }

    public List<NationDatabase.ProtectionCore> all() { return cores; }

    public Optional<NationDatabase.ProtectionCore> findAt(Block block) {
        return findAt(block.getLocation());
    }

    public Optional<NationDatabase.ProtectionCore> findAt(Location location) {
        String world = location.getWorld().getName();
        int x = location.getBlockX(), z = location.getBlockZ();
        for (NationDatabase.ProtectionCore core : cores) {
            if (core.world().equals(world) && core.contains(x, z)) return Optional.of(core);
        }
        return Optional.empty();
    }

    public Optional<NationDatabase.ProtectionCore> findAnchor(Block block) {
        return findAt(block).filter(core -> core.isAnchor(block.getX(), block.getY(), block.getZ()));
    }

    private boolean mayModify(Player player, Block block) {
        Optional<NationDatabase.ProtectionCore> found = findAt(block);
        if (found.isEmpty()) return true;
        if (found.get().isAnchor(block.getX(), block.getY(), block.getZ())) return false;
        if (player.hasPermission("earthsmp.admin")) return true;
        try {
            Optional<NationDatabase.Membership> member = plugin.getNationDatabase().findMembership(player.getUniqueId());
            return member.isPresent() && member.get().nationId().equals(found.get().nationId());
        } catch (SQLException error) {
            plugin.getLogger().log(Level.SEVERE, "Cannot check core protection, denying action", error);
            return false;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block=event.getBlock();
        var core=findAt(block);
        if(core.isEmpty()) return;
        // Core anchors may NEVER be mined; only the later capture timer transfers them.
        if(core.get().isAnchor(block.getX(),block.getY(),block.getZ())) {
            event.setCancelled(true);
            plugin.getSecurityAlerts().record(event.getPlayer(), "CORE_ANCHOR", "Attempted mining protected core anchor", true, true);
            return;
        }
        if(mayModify(event.getPlayer(),block)) return;
        // Even a legal war cannot tunnel through NATURAL terrain, or use ordinary tools.
        if(!plugin.getSiegePickaxe().mayBreak(event.getPlayer(),block,core.get())) {
            event.setCancelled(true);
            plugin.getSecurityAlerts().record(event.getPlayer(), "PROTECTED_BREAK", "Denied block mining in core region", true, false);
        }
    }

    private boolean siegePlacementBlocked(Block block) {
        var core=findAt(block);
        if(core.isEmpty()) return false;
        try { return plugin.getSieges().hostilePresent(core.get()); }
        catch(SQLException error) {
            plugin.getLogger().log(Level.SEVERE,"Cannot verify siege building freeze; denying",error);
            return true;
        }
    }

    private boolean rejectPlacement(BlockPlaceEvent event) {
        if(!mayModify(event.getPlayer(),event.getBlockPlaced()) || siegePlacementBlocked(event.getBlockPlaced())) return true;
        if(event instanceof BlockMultiPlaceEvent multi) {
            // Beds and doors can extend across the protection boundary.
            for(BlockState state:multi.getReplacedBlockStates()) {
                if(!mayModify(event.getPlayer(),state.getBlock()) || siegePlacementBlocked(state.getBlock())) return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if(rejectPlacement(event)) {
            event.setCancelled(true);
            plugin.getSecurityAlerts().record(event.getPlayer(), "PROTECTED_PLACE", "Denied block placement in core region", true, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMultiPlace(BlockMultiPlaceEvent event) {
        if(rejectPlacement(event)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block != null && !mayModify(event.getPlayer(), block)) {
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        }
    }

    /**
     * An attacker cannot loot a core's containers during ANY phase of the
     * assault. War and a legal siege never grant permission to open them.
     * After capture the same core belongs to the victor, so their members
     * gain normal owner access (or the last core disappears altogether).
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        var inventory = event.getInventory();
        // Custom plugin GUIs have no block location. Skip those entirely.
        // Check BOTH halves of a double chest. A boundary-crossing chest must
        // not allow access through the half lying outside the protected base.
        Location[] locations = inventory instanceof DoubleChestInventory doubleChest
                ? new Location[]{doubleChest.getLeftSide().getLocation(), doubleChest.getRightSide().getLocation()}
                : new Location[]{inventory.getLocation()};
        try {
            var member = plugin.getNationDatabase().findMembership(player.getUniqueId());
            for (Location location : locations) {
                if (location == null) continue;
                var found = findAt(location);
                if (found.isEmpty() || (member.isPresent() &&
                        member.get().nationId().equals(found.get().nationId()))) continue;
                // Admin override in peace only; even an admin playing for an
                // attacking nation cannot loot a hostile base during war.
                boolean admin = player.hasPermission("earthsmp.admin");
                if (!admin || (member.isPresent() && plugin.getWars().opposed(
                        member.get().nationId(), found.get().nationId()))) {
                    event.setCancelled(true);
                    plugin.getSecurityAlerts().record(player, "PROTECTED_CONTAINER", "Denied access to protected inventory", true, false);
                    return;
                }
            }
        } catch (SQLException error) {
            plugin.getLogger().log(Level.SEVERE, "Cannot authorize container open; denying", error);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Block target=event.getBlockClicked().getRelative(event.getBlockFace());
        if (!mayModify(event.getPlayer(),target) || siegePlacementBlocked(target)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!mayModify(event.getPlayer(), event.getBlockClicked())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChange(EntityChangeBlockEvent event) {
        if (findAt(event.getBlock()).isPresent()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> findAt(block).isPresent());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> findAt(block).isPresent());
    }

    // Conservative in v1.0: pistons affecting protected blocks are denied even for owners.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (findAt(event.getBlock()).isPresent() || event.getBlocks().stream().anyMatch(b ->
                findAt(b).isPresent() || findAt(b.getRelative(event.getDirection())).isPresent())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (findAt(event.getBlock()).isPresent() || event.getBlocks().stream().anyMatch(b ->
                findAt(b).isPresent() || findAt(b.getRelative(event.getDirection())).isPresent())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFluid(BlockFromToEvent event) {
        Optional<NationDatabase.ProtectionCore> source = findAt(event.getBlock());
        Optional<NationDatabase.ProtectionCore> dest = findAt(event.getToBlock());
        if (!source.map(NationDatabase.ProtectionCore::id).equals(dest.map(NationDatabase.ProtectionCore::id))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        if (findAt(event.getBlock()).isPresent() && findAt(event.getSource()).isEmpty()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (findAt(event.getBlock()).isEmpty()) return;
        if (event.getPlayer() == null || !mayModify(event.getPlayer(), event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (findAt(event.getBlock()).isPresent()) event.setCancelled(true);
    }

    // Prevent hoppers outside a base from extracting contents across its boundary.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHopper(InventoryMoveItemEvent event) {
        Location from = event.getSource().getLocation(), to = event.getDestination().getLocation();
        if (from == null || to == null) return;
        Optional<String> src = findAt(from).map(NationDatabase.ProtectionCore::id);
        Optional<String> dst = findAt(to).map(NationDatabase.ProtectionCore::id);
        if (!src.equals(dst)) event.setCancelled(true);
    }
}
