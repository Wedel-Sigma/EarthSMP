package io.github.wedelsigma.earthsmp.core;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

/** Locks enchant levels on finished items, trades, enchanting and anvil output. */
public final class EditionEnchantListener implements Listener {
    private final EarthSmpCorePlugin plugin;

    public EditionEnchantListener(EarthSmpCorePlugin plugin) { this.plugin = plugin; }

    private int day() {
        try {
            return plugin.getServerClock().day(plugin.getNationDatabase().editionStart());
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "Edition day unavailable: using restrictive enchant policy", exception);
            return 0;
        }
    }

    private static String key(Enchantment enchantment) {
        return enchantment.getKey().getKey();
    }

    static boolean isIllegal(ItemStack item, int day) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            if (invalid(entry.getKey(), entry.getValue(), day)) return true;
        }
        if (meta instanceof EnchantmentStorageMeta book) {
            for (Map.Entry<Enchantment, Integer> entry : book.getStoredEnchants().entrySet()) {
                if (invalid(entry.getKey(), entry.getValue(), day)) return true;
            }
        }
        return false;
    }

    private static boolean invalid(Enchantment ench, int level, int day) {
        return level > ench.getMaxLevel() || EditionEnchantRules.needsChange(key(ench), level, day);
    }

    /** Modifies only illegal enchantments; preserves the item, name, lore and other metadata. */
    static boolean normalize(ItemStack item, int day) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        boolean changed = false;
        for (var entry : new ArrayList<>(meta.getEnchants().entrySet())) {
            int expected = Math.min(entry.getKey().getMaxLevel(),
                    EditionEnchantRules.normalizedLevel(key(entry.getKey()), entry.getValue(), day));
            if (expected == entry.getValue()) continue;
            meta.removeEnchant(entry.getKey());
            if (expected > 0) meta.addEnchant(entry.getKey(), expected, true);
            changed = true;
        }
        if (meta instanceof EnchantmentStorageMeta book) {
            for (var entry : new ArrayList<>(book.getStoredEnchants().entrySet())) {
                int expected = Math.min(entry.getKey().getMaxLevel(),
                        EditionEnchantRules.normalizedLevel(key(entry.getKey()), entry.getValue(), day));
                if (expected == entry.getValue()) continue;
                book.removeStoredEnchant(entry.getKey());
                if (expected > 0) book.addStoredEnchant(entry.getKey(), expected, true);
                changed = true;
            }
        }
        if (changed) item.setItemMeta(meta);
        return changed;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        int currentDay = day();
        for (var entry : event.getEnchantsToAdd().entrySet()) {
            if (invalid(entry.getKey(), entry.getValue(), currentDay)) {
                event.setCancelled(true);
                plugin.getSecurityAlerts().record(event.getEnchanter(), "ENCHANT_DENIED", "Enchantment table offered illegal result", true, false);
                event.getEnchanter().sendActionBar(Component.text(
                                "Zaklęcie zablokowane przez progresję edycji.", NamedTextColor.RED));
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAnvil(PrepareAnvilEvent event) {
        if (isIllegal(event.getResult(), day())) event.setResult(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSmithing(PrepareSmithingEvent event) {
        if (isIllegal(event.getResult(), day())) event.setResult(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraft(PrepareItemCraftEvent event) {
        if (isIllegal(event.getInventory().getResult(), day())) event.getInventory().setResult(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTradeCreated(VillagerAcquireTradeEvent event) {
        MerchantRecipe recipe = event.getRecipe();
        if (isIllegal(recipe.getResult(), day())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        InventoryType type = event.getView().getTopInventory().getType();
        boolean resultSlot = (type == InventoryType.MERCHANT && event.getRawSlot() == 2)
                || (type == InventoryType.ANVIL && event.getRawSlot() == 2)
                || (type == InventoryType.SMITHING && event.getRawSlot() == 3);
        if (resultSlot && isIllegal(event.getCurrentItem(), day())) {
            event.setCancelled(true);
            plugin.getSecurityAlerts().record(player, "ENCHANT_RESULT", "Illegal merchant/anvil/smithing output; " + type, true, false);
            player.sendActionBar(Component.text("Wynik ma niedozwolone zaklęcie.", NamedTextColor.RED));
        }
        scheduleCheck(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) scheduleCheck(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMending(PlayerItemMendEvent event) {
        event.setCancelled(true);
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) { scheduleCheck(event.getPlayer()); }

    private void scheduleCheck(Player player) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) audit(player);
        });
    }

    /** Audit pre-existing enchanted items and items introduced by other plugins. */
    public void audit(Player player) {
        int currentDay = day();
        boolean changed = false;
        ItemStack[] items = player.getInventory().getContents();
        for (int slot = 0; slot < items.length; slot++) {
            if (normalize(items[slot], currentDay)) {
                player.getInventory().setItem(slot, items[slot]);
                changed = true;
            }
        }
        ItemStack cursor = player.getItemOnCursor();
        if (normalize(cursor, currentDay)) {
            player.setItemOnCursor(cursor);
            changed = true;
        }
        if (changed) {
            plugin.getSecurityAlerts().record(player, "ENCHANT_NORMALIZED", "Illegal enchantment levels on carried item were normalized", true, true);
            player.updateInventory();
            player.sendActionBar(Component.text("Niedozwolone zaklecia zostaly ograniczone do zasad edycji.",
                    NamedTextColor.YELLOW));
        }
    }
}
