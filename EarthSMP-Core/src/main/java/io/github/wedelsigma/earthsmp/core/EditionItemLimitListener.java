package io.github.wedelsigma.earthsmp.core;

import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Enforces aggregate player inventory limits. Vanilla container -> player transactions
 * are capped BEFORE the transfer; partial pickups leave the remainder in its source.
 * A periodic safety pass drops unexpected plugin/command-injected overflow on the ground,
 * rather than deleting items. Stationary storage itself is never capped.
 */
public final class EditionItemLimitListener implements Listener {
    private final EarthSmpCorePlugin plugin;

    public EditionItemLimitListener(EarthSmpCorePlugin plugin) { this.plugin = plugin; }

    private static boolean tracked(ItemStack stack) {
        return stack != null && EditionItemRules.cap(stack.getType().name()) >= 0;
    }

    private static boolean present(ItemStack stack) {
        return stack != null && !stack.getType().isAir() && stack.getAmount() > 0;
    }

    static int carried(Player player, Material material, boolean cursor) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (present(stack) && stack.getType() == material) total += stack.getAmount();
        }
        if (cursor) {
            ItemStack onCursor = player.getItemOnCursor();
            if (present(onCursor) && onCursor.getType() == material) total += onCursor.getAmount();
        }
        return total;
    }

    private static ItemStack amount(ItemStack original, int size) {
        ItemStack copy = original.clone();
        copy.setAmount(size);
        return copy;
    }

    private void warn(Player player, Material material) {
        plugin.getSecurityAlerts().record(player, "ITEM_LIMIT", material.name() + " cap=" + EditionItemRules.cap(material.name()), true, false);
        int cap = EditionItemRules.cap(material.name());
        player.sendActionBar(Component.text(cap == 0
                ? "Przedmiot zabroniony: " + material.name()
                : "Limit ekwipunku: " + material.name() + " (maks. " + cap + ")", NamedTextColor.RED));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Item entity = event.getItem();
        ItemStack stack = entity.getItemStack();
        if (!tracked(stack)) return;
        int room = EditionItemRules.space(stack.getType().name(), carried(player, stack.getType(), true));
        if (room >= stack.getAmount()) return;
        event.setCancelled(true);
        if (room <= 0) { warn(player, stack.getType()); return; }
        // Manually transfer only the portion which fits. Do not consume the world item on failure.
        ItemStack offered = amount(stack, Math.min(room, stack.getAmount()));
        int requested = offered.getAmount();
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(offered);
        int notAccepted = leftovers.values().stream().mapToInt(ItemStack::getAmount).sum();
        int moved = requested - notAccepted;
        if (moved <= 0) return;
        if (moved >= stack.getAmount()) entity.remove();
        else entity.setItemStack(amount(stack, stack.getAmount() - moved));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        Inventory clicked = event.getClickedInventory();
        if ((event.getAction() == InventoryAction.COLLECT_TO_CURSOR && tracked(event.getCursor())) ||
                (event.getAction() == InventoryAction.CLONE_STACK && tracked(event.getCurrentItem()))) {
            event.setCancelled(true);
            return;
        }
        if (clicked != top || top.getType() == InventoryType.CRAFTING ||
                top.getType() == InventoryType.PLAYER) {
            if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR && tracked(event.getCursor())) {
                // Double click may collect matching items from an external container.
                event.setCancelled(true);
            }
            scheduleCheck(player);
            return;
        }
        ItemStack source = event.getCurrentItem();
        if (!tracked(source)) { scheduleCheck(player); return; }
        int cap = EditionItemRules.cap(source.getType().name());
        int count = carried(player, source.getType(), true);
        int room = EditionItemRules.space(source.getType().name(), count);
        InventoryAction action = event.getAction();

        if (cap == 0) { event.setCancelled(true); warn(player, source.getType()); return; }
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (room >= source.getAmount()) { scheduleCheck(player); return; }
            event.setCancelled(true);
            if (room <= 0) { warn(player, source.getType()); return; }
            // Shift-click from chest: transfer only legal portion to player's inventory.
            ItemStack offered = amount(source, Math.min(room, source.getAmount()));
            int requested = offered.getAmount();
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(offered);
            int notAccepted = leftovers.values().stream().mapToInt(ItemStack::getAmount).sum();
            int moved = requested - notAccepted;
            if (moved > 0) top.setItem(event.getSlot(), moved == source.getAmount()
                    ? null : amount(source, source.getAmount() - moved));
            warn(player, source.getType());
            return;
        }
        if (action == InventoryAction.PICKUP_ALL || action == InventoryAction.PICKUP_HALF ||
                action == InventoryAction.PICKUP_ONE || action == InventoryAction.PICKUP_SOME) {
            if (present(event.getCursor())) {
                // Conservative guard for unusual container implementations that pick up
                // more items onto an already populated cursor.
                int wanted = action == InventoryAction.PICKUP_ONE ? 1 : source.getAmount();
                if (wanted > room) { event.setCancelled(true); warn(player, source.getType()); }
                scheduleCheck(player);
                return;
            }
            int wanted = switch (action) {
                case PICKUP_HALF -> (source.getAmount() + 1) / 2;
                case PICKUP_ONE -> 1;
                default -> source.getAmount();
            };
            if (wanted <= room) { scheduleCheck(player); return; }
            event.setCancelled(true);
            if (room > 0) {
                int moved = Math.min(room, wanted);
                player.setItemOnCursor(amount(source, moved));
                top.setItem(event.getSlot(), moved == source.getAmount()
                        ? null : amount(source, source.getAmount() - moved));
            }
            warn(player, source.getType());
            return;
        }
        if (action == InventoryAction.HOTBAR_SWAP || action == InventoryAction.HOTBAR_MOVE_AND_READD) {
            int hotbar = event.getHotbarButton();
            ItemStack previous = hotbar >= 0 && hotbar < 9 ? player.getInventory().getItem(hotbar) : null;
            if (event.getClick() == ClickType.SWAP_OFFHAND) previous = player.getInventory().getItemInOffHand();
            int displaced = present(previous) && previous.getType() == source.getType() ? previous.getAmount() : 0;
            if (count - displaced + source.getAmount() > cap) {
                event.setCancelled(true); warn(player, source.getType());
            }
            scheduleCheck(player);
            return;
        }
        if (action == InventoryAction.SWAP_WITH_CURSOR) {
            ItemStack cursor = event.getCursor();
            int replaced = present(cursor) && cursor.getType() == source.getType() ? cursor.getAmount() : 0;
            if (count - replaced + source.getAmount() > cap) {
                event.setCancelled(true); warn(player, source.getType());
            }
            scheduleCheck(player);
            return;
        }
        if (action == InventoryAction.COLLECT_TO_CURSOR || action == InventoryAction.CLONE_STACK ||
                action == InventoryAction.UNKNOWN) {
            event.setCancelled(true); warn(player, source.getType()); return;
        }
        scheduleCheck(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreative(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack proposed = event.getCursor();
        if (!tracked(proposed)) { scheduleCheck(player); return; }
        int old = present(event.getCurrentItem()) && event.getCurrentItem().getType() == proposed.getType()
                ? event.getCurrentItem().getAmount() : 0;
        int remaining = EditionItemRules.space(proposed.getType().name(),
                carried(player, proposed.getType(), false) - old);
        if (proposed.getAmount() > remaining) {
            event.setCancelled(true); warn(player, proposed.getType());
        }
        scheduleCheck(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack moving = event.getOldCursor();
        if (tracked(moving) && EditionItemRules.forbidden(moving.getType().name())) {
            event.setCancelled(true); warn(player, moving.getType()); return;
        }
        // Cursor items were already counted when taken from the source.
        scheduleCheck(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack product = event.getRecipe().getResult();
        if (!tracked(product)) return;
        // Bulk crafting can't be estimated reliably from Bukkit's one-result stack.
        if (event.isShiftClick() || EditionItemRules.space(product.getType().name(),
                carried(player, product.getType(), true)) < product.getAmount()) {
            event.setCancelled(true); warn(player, product.getType());
        } else scheduleCheck(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack hand = event.getItemInHand();
        if (tracked(hand) && EditionItemRules.forbidden(hand.getType().name())) {
            event.setCancelled(true); warn(event.getPlayer(), hand.getType());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (tracked(item) && EditionItemRules.forbidden(item.getType().name())) {
            event.setCancelled(true); warn(event.getPlayer(), item.getType());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (tracked(item) && EditionItemRules.forbidden(item.getType().name())) {
            event.setCancelled(true); warn(event.getPlayer(), item.getType());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        ItemStack item = event.getItem();
        if (tracked(item) && EditionItemRules.forbidden(item.getType().name())) event.setCancelled(true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) { scheduleCheck(event.getPlayer()); }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) scheduleCheck(player);
    }

    private void scheduleCheck(Player player) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) audit(player);
        });
    }

    /** Failsafe for /give, other plugins and nonstandard inventory transactions. */
    public void audit(Player player) {
        boolean touched = false;
        for (ItemStack source : player.getInventory().getContents()) {
            if (!tracked(source)) continue;
            Material type = source.getType();
            int excess = carried(player, type, true) - EditionItemRules.cap(type.name());
            if (excess <= 0) continue;
            // Prefer to remove from the player's inventory. No legal items are destroyed.
            ItemStack[] contents = player.getInventory().getContents();
            for (int slot = contents.length - 1; slot >= 0 && excess > 0; slot--) {
                ItemStack stack = contents[slot];
                if (!present(stack) || stack.getType() != type) continue;
                int take = Math.min(excess, stack.getAmount());
                player.getInventory().setItem(slot, take == stack.getAmount()
                        ? null : amount(stack, stack.getAmount() - take));
                player.getWorld().dropItem(player.getLocation(), amount(stack, take));
                excess -= take;
                touched = true;
            }
            if (excess > 0) {
                ItemStack cursor = player.getItemOnCursor();
                if (present(cursor) && cursor.getType() == type) {
                    int take = Math.min(excess, cursor.getAmount());
                    player.setItemOnCursor(take == cursor.getAmount() ? null
                            : amount(cursor, cursor.getAmount() - take));
                    player.getWorld().dropItem(player.getLocation(), amount(cursor, take));
                    touched = true;
                }
            }
        }
        if (touched) {
            plugin.getSecurityAlerts().record(player, "INVENTORY_OVERFLOW", "Automatic cleanup of forbidden or excess items", true, true);
            player.updateInventory();
        }
    }
}
