package io.github.wedelsigma.earthsmp.core;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

/**
 * Provenance ledger. Tracks *successful* vanilla player placements in Earth, even before
 * an area is claimed, and forgets locations after typical block changes/removal.
 * Historical blocks and blocks modified by external editing plugins remain UNKNOWN.
 * UNKNOWN is never eligible for siege destruction.
 */
public final class BlockOriginListener implements Listener {
    private final EarthSmpCorePlugin plugin;
    public BlockOriginListener(EarthSmpCorePlugin plugin) { this.plugin=plugin; }

    private boolean inEarth(Block block) {
        return block.getWorld().getName().equals(plugin.getConfig().getString("world-name","Earth"));
    }
    private void mark(Block block) {
        if(!inEarth(block) || block.getType().isAir()) return;
        try {
            plugin.getSiegeStorage().markPlaced(block.getWorld().getName(),block.getX(),block.getY(),
                    block.getZ(),block.getType().name());
        } catch(SQLException e) { plugin.getLogger().log(Level.SEVERE,"Cannot record block provenance; location will be unknown",e); }
    }
    private void forget(Block block) {
        if(!inEarth(block)) return;
        try {
            plugin.getSiegeStorage().forget(block.getWorld().getName(),block.getX(),block.getY(),block.getZ());
        } catch(SQLException e) { plugin.getLogger().log(Level.SEVERE,"Cannot clear stale block provenance",e); }
    }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onPlace(BlockPlaceEvent event) {
        if(event.canBuild()) mark(event.getBlockPlaced());
    }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onMultiPlace(BlockMultiPlaceEvent event) {
        if(!event.canBuild()) return;
        mark(event.getBlockPlaced());
        for(BlockState state : event.getReplacedBlockStates()) mark(state.getBlock());
    }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onBreak(BlockBreakEvent event) {
        // Debit the validated 15-use pickaxe while the provenance row still exists.
        plugin.getSiegePickaxe().onSuccessfulBreak(event);
        forget(event.getBlock());
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onBurn(BlockBurnEvent event) { forget(event.getBlock()); }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onFade(BlockFadeEvent event) { forget(event.getBlock()); }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onForm(BlockFormEvent event) { forget(event.getBlock()); }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onSpread(BlockSpreadEvent event) { forget(event.getBlock()); }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onFluid(BlockFromToEvent event) { forget(event.getToBlock()); }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onEntityChange(EntityChangeBlockEvent event) { forget(event.getBlock()); }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onBlockExplode(BlockExplodeEvent event) { for(Block b : event.blockList()) forget(b); }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onEntityExplode(EntityExplodeEvent event) { for(Block b : event.blockList()) forget(b); }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for(Block b:event.getBlocks()) { forget(b);forget(b.getRelative(event.getDirection())); }
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for(Block b:event.getBlocks()) { forget(b);forget(b.getRelative(event.getDirection())); }
    }
}
