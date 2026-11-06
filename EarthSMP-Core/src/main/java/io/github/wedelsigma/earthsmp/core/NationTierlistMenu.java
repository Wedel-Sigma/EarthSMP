package io.github.wedelsigma.earthsmp.core;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

/** Read-only, paginated inventory ranking of the CURRENT owners of placed cores. */
public final class NationTierlistMenu implements Listener {
    private static final int PAGE_SIZE = 45;
    private final EarthSmpCorePlugin plugin;

    private static final class RankingHolder implements InventoryHolder {
        final UUID viewer;
        final int page;
        final Inventory inventory;

        RankingHolder(Player player, int page, String displayName) {
            this.viewer = player.getUniqueId();
            this.page = page;
            this.inventory = Bukkit.createInventory(this, 54,
                    Component.text(displayName + " | Ranking panstw", NamedTextColor.GOLD));
        }
        @Override public @NotNull Inventory getInventory() { return inventory; }
    }

    NationTierlistMenu(EarthSmpCorePlugin plugin) { this.plugin = plugin; }

    public void open(Player viewer) { open(viewer, 0); }

    private void open(Player player, int requestedPage) {
        try {
            List<NationDatabase.NationRank> ranking = plugin.getNationDatabase().nationRanking();
            int lastPage = Math.max(0, (ranking.size() - 1) / PAGE_SIZE);
            int page = Math.max(0, Math.min(requestedPage, lastPage));
            RankingHolder holder = new RankingHolder(player, page, plugin.getDisplayName());
            Inventory inv = holder.inventory;
            if (ranking.isEmpty()) {
                inv.setItem(22, item(Material.BARRIER, "Brak panstw", NamedTextColor.YELLOW,
                        "Po utworzeniu panstw pojawi sie tu ranking."));
            } else {
                for (int i = page * PAGE_SIZE; i < Math.min((page + 1) * PAGE_SIZE, ranking.size()); i++) {
                    NationDatabase.NationRank country = ranking.get(i);
                    NamedTextColor color = i == 0 ? NamedTextColor.GOLD : NamedTextColor.WHITE;
                    inv.setItem(i % PAGE_SIZE, item(banner(country.nationId()),
                            "#" + (i + 1) + " " + country.name(), color,
                            "Czlonkowie: " + country.members(),
                            "Kontrolowane rdzenie: " + country.placedCores(),
                            "Wlasne i zdobyczne (tylko postawione)"));
                }
            }
            if (page > 0) inv.setItem(45, item(Material.ARROW, "Poprzednia strona", NamedTextColor.YELLOW));
            inv.setItem(49, item(Material.OAK_DOOR, "Wroc do centrum " + plugin.getDisplayName(), NamedTextColor.AQUA,
                    "Wszystkich panstw: " + ranking.size(), "Strona " + (page + 1) + "/" + (lastPage + 1)));
            if (page < lastPage) inv.setItem(53, item(Material.ARROW, "Nastepna strona", NamedTextColor.YELLOW));
            player.openInventory(inv);
        } catch (SQLException error) {
            plugin.getLogger().log(Level.WARNING, "Could not open nation tierlist", error);
            player.sendMessage(Component.text("Nie udalo sie pobrac rankingu panstw.", NamedTextColor.RED));
        }
    }

    /** Deterministic distinct country colors; no country icon data is stored yet. */
    private static Material banner(String nationId) {
        Material[] banners = {Material.BLUE_BANNER, Material.RED_BANNER, Material.GREEN_BANNER,
                Material.YELLOW_BANNER, Material.PURPLE_BANNER, Material.CYAN_BANNER,
                Material.ORANGE_BANNER, Material.LIME_BANNER, Material.WHITE_BANNER};
        return banners[Math.floorMod(nationId.hashCode(), banners.length)];
    }

    private static ItemStack item(Material type, String title, NamedTextColor titleColor, String... lore) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(title, titleColor));
        meta.lore(java.util.Arrays.stream(lore).map(line -> Component.text(line, NamedTextColor.GRAY)).toList());
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof RankingHolder holder)) return;
        event.setCancelled(true); // no stealing banners or moving inventory items
        if (!(event.getWhoClicked() instanceof Player player) || !player.getUniqueId().equals(holder.viewer)) return;
        if (event.getClickedInventory() != holder.inventory) return;
        int slot = event.getRawSlot();
        if (slot != 45 && slot != 49 && slot != 53) return;
        // Never open inventories recursively inside InventoryClickEvent.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || player.getOpenInventory().getTopInventory() != holder.inventory) return;
            if (slot == 49) plugin.getHubMenu().openMain(player);
            else if (slot == 45 && holder.page > 0) open(player, holder.page - 1);
            else if (slot == 53) open(player, holder.page + 1);
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof RankingHolder)) return;
        if (event.getRawSlots().stream().anyMatch(slot -> slot < event.getView().getTopInventory().getSize()))
            event.setCancelled(true);
    }
}
