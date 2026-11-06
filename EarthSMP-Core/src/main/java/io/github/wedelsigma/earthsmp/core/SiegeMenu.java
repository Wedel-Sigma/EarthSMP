package io.github.wedelsigma.earthsmp.core;

import java.util.List;
import java.util.UUID;
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

/** Compact Bukkit inventory UI; no client mods or exact core coordinates exposed. */
public final class SiegeMenu implements Listener {
    private final EarthSmpCorePlugin plugin;
    private static final class Holder implements InventoryHolder {
        final UUID viewer;
        final Inventory inventory;
        Holder(Player p, String displayName) {
            viewer=p.getUniqueId();
            inventory=Bukkit.createInventory(this,27,Component.text(displayName + " | Oblezenia",NamedTextColor.GOLD));
        }
        @Override public @NotNull Inventory getInventory(){return inventory;}
    }
    public SiegeMenu(EarthSmpCorePlugin plugin){this.plugin=plugin;}
    private ItemStack item(Material type,String title,String... lore) {
        var item=new ItemStack(type);
        ItemMeta m=item.getItemMeta();
        m.displayName(Component.text(title,NamedTextColor.GOLD));
        m.lore(java.util.Arrays.stream(lore).map(x->Component.text(x,NamedTextColor.GRAY)).toList());
        item.setItemMeta(m);
        return item;
    }
    public void open(Player p) {
        Holder h=new Holder(p, plugin.getDisplayName());
        List<String> description=plugin.getSieges().describe(p);
        h.inventory.setItem(11,item(Material.BOOK,"Status oblezenia",description.toArray(String[]::new)));
        h.inventory.setItem(13,item(Material.IRON_SWORD,"Szturm: automatyczny",
                "Wejdz do wrogiej bazy 65x65",
                "Wymaga wojny, godziny 10-00 i 20% obroncow",
                "Strefa 9x9: przejmowanie 480 sekund"));
        h.inventory.setItem(15,item(Material.CLOCK,"Reguly oblezen",
                "10:00-00:00 Europe/Warsaw", "20% pelnego skladu, min. 1 online",
                "Po spadku progu: 30 minut tolerancji",
                "Skrzynie wroga: zablokowane do zdobycia rdzenia"));
        h.inventory.setItem(16,item(Material.DIAMOND_PICKAXE,"Kilof Oblezniczy - receptura",
                "W srodku: zwykly diamentowy kilof",
                "Dookola po 16: wegiel, zelazo, miedz",
                "redstone, lapis, zloto, diament, ametyst",
                "128 surowcow + kilof; 15 legalnych blokow",
                "Tylko gracz-postawione bloki w obleganej bazie"));
        h.inventory.setItem(22,item(Material.ARROW,"Powrot do /esmp"));
        p.openInventory(h.inventory);
    }
    @EventHandler public void onClick(InventoryClickEvent e) {
        if(!(e.getView().getTopInventory().getHolder(false) instanceof Holder h)) return;
        e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(h.viewer)) return;
        if(e.getRawSlot()<0 || e.getRawSlot()>=h.inventory.getSize()) return;
        int slot=e.getRawSlot();
        if(slot==11 || slot==22) {
            plugin.getServer().getScheduler().runTask(plugin,()-> {
                if(!p.isOnline() || p.getOpenInventory().getTopInventory()!=h.inventory) return;
                if(slot==11) {plugin.getSieges().status(p);open(p);}
                if(slot==22) plugin.getHubMenu().openMain(p);
            });
        }
    }
    @EventHandler public void onDrag(InventoryDragEvent e) {
        if(e.getView().getTopInventory().getHolder(false) instanceof Holder h &&
                e.getRawSlots().stream().anyMatch(slot->slot<h.inventory.getSize())) e.setCancelled(true);
    }
}
