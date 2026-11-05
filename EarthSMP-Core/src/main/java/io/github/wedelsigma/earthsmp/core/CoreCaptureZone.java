package io.github.wedelsigma.earthsmp.core;

import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Permanent building restrictions for the future siege capture area.
 * This does NOT implement sieges, timers, hostile occupancy, or block provenance.
 * The core itself and its UUID never appear on the public web map.
 */
public final class CoreCaptureZone implements Listener {
    public static final int RADIUS = 4; // 9 x 9 inclusive
    public static final int BELOW = 5;
    public static final int ABOVE = 10; // 16 block layers inclusive

    // Exact Material names avoid accidentally banning ordinary build materials.
    // Bed colours all end with "_BED"; the name check also covers newly added dyes.
    private static final Set<String> FORBIDDEN = Set.of(
            "OBSIDIAN", "CRYING_OBSIDIAN", "NETHERITE_BLOCK",
            "REINFORCED_DEEPSLATE", "ANCIENT_DEBRIS", "BEDROCK",
            "BARRIER", "LIGHT", "STRUCTURE_VOID", "STRUCTURE_BLOCK", "JIGSAW",
            "COMMAND_BLOCK", "CHAIN_COMMAND_BLOCK", "REPEATING_COMMAND_BLOCK",
            "END_PORTAL_FRAME", "END_PORTAL", "END_GATEWAY",
            "RESPAWN_ANCHOR", "MOVING_PISTON");

    private final EarthSmpCorePlugin plugin;

    public CoreCaptureZone(EarthSmpCorePlugin plugin) {
        this.plugin = plugin;
    }

    public static boolean forbidden(Material type) {
        String name = type.name();
        return name.endsWith("_BED") || FORBIDDEN.contains(name);
    }

    /** Integer coordinates: edges at +/-4 and Y -5..+10 belong to the zone. */
    public static boolean inside(int blockX, int blockY, int blockZ,
                                 int anchorX, int anchorY, int anchorZ) {
        return Math.abs((long)blockX - anchorX) <= RADIUS
                && Math.abs((long)blockZ - anchorZ) <= RADIUS
                && (long)blockY >= (long)anchorY - BELOW
                && (long)blockY <= (long)anchorY + ABOVE;
    }

    private boolean restricted(Block block, Material newType) {
        if (!forbidden(newType)) return false;
        Location pos = block.getLocation();
        for (NationDatabase.ProtectionCore core : plugin.getCoreProtection().all()) {
            if (core.world().equals(pos.getWorld().getName()) &&
                    inside(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(),
                            core.x(), core.y(), core.z())) return true;
        }
        return false;
    }

    private boolean illegalPlacement(BlockPlaceEvent event) {
        // Normally the placed block is already the new type. Checking the held item
        // also covers cases where a multi-block structure has an unusual block state.
        Material itemType = event.getItemInHand().getType();
        Material blockType = event.getBlockPlaced().getType();
        if (!forbidden(itemType) && !forbidden(blockType)) return false;
        if (restricted(event.getBlockPlaced(), itemType) ||
                restricted(event.getBlockPlaced(), blockType)) return true;
        if (event instanceof BlockMultiPlaceEvent multi) {
            // A bed can have its foot OUTSIDE but its head INSIDE the capture zone.
            // The replaced states identify BOTH coordinates of a multi-block placement.
            for (BlockState replaced : multi.getReplacedBlockStates()) {
                if (restricted(replaced.getBlock(), itemType) ||
                        restricted(replaced.getBlock(), blockType)) return true;
            }
        }
        return false;
    }

    private void deny(Player player) {
        player.sendMessage(Component.text(
                "Strefa 9x9 przy rdzeniu: zakaz lozek i bardzo odpornych blokow " +
                "(Y rdzenia -5 do +10).", NamedTextColor.RED));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (illegalPlacement(event)) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    // Multi-place has its own handler list on Bukkit. Cover it explicitly.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMultiPlace(BlockMultiPlaceEvent event) {
        if (illegalPlacement(event)) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    private void denyForm(BlockFormEvent event) {
        if (restricted(event.getBlock(), event.getNewState().getType())) {
            event.setCancelled(true);
        }
    }

    // Obsidian can FORM by mixing water/lava, without any BlockPlaceEvent.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onForm(BlockFormEvent event) { denyForm(event); }

    // These are subclasses but may be dispatched through their own handler lists.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) { denyForm(event); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityForm(EntityBlockFormEvent event) { denyForm(event); }

    /**
     * Scan BEFORE placing a core. Otherwise a player could construct an
     * obsidian/bedrock cage first and then put a core inside it.
     * Does not modify world blocks, database contents, or chunk claims.
     */
    public static ZoneAudit audit(World world, int anchorX, int anchorY, int anchorZ) {
        int violations = 0;
        String first = null;
        int minY = Math.max(world.getMinHeight(), anchorY - BELOW);
        int maxY = Math.min(world.getMaxHeight() - 1, anchorY + ABOVE);
        for (int y = minY; y <= maxY; y++) {
            for (int x = anchorX - RADIUS; x <= anchorX + RADIUS; x++) {
                for (int z = anchorZ - RADIUS; z <= anchorZ + RADIUS; z++) {
                    Material material = world.getBlockAt(x, y, z).getType();
                    if (forbidden(material)) {
                        violations++;
                        if (first == null) first = material.name() + " @ " + x + " " + y + " " + z;
                    }
                }
            }
        }
        return new ZoneAudit(violations, first);
    }

    public record ZoneAudit(int violations, String first) {
        public boolean clear() { return violations == 0; }
    }
}
