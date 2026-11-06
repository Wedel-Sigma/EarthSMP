package io.github.wedelsigma.earthsmp.core;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** Registered, nation-bound items; 512 ingredients are spent atomically for one crafted core. */
public final class CoreItems implements Listener {
    private static final Material[] INGREDIENTS = {
            Material.COAL, Material.IRON_INGOT, Material.COPPER_INGOT,
            Material.REDSTONE, Material.JUKEBOX, Material.LAPIS_LAZULI,
            Material.GOLD_INGOT, Material.DIAMOND, Material.AMETHYST_SHARD
    };
    private final EarthSmpCorePlugin plugin;
    private final NamespacedKey recipeKey, tokenIdKey, nationIdKey, previewKey;

    public CoreItems(EarthSmpCorePlugin plugin) {
        this.plugin = plugin;
        this.recipeKey = new NamespacedKey(plugin, "protection_core_crafting");
        this.tokenIdKey = new NamespacedKey(plugin, "protection_core_token_id");
        this.nationIdKey = new NamespacedKey(plugin, "protection_core_nation_id");
        this.previewKey = new NamespacedKey(plugin, "protection_core_preview");
        ItemStack result = makePreview();
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape("CIM", "RJL", "GDA");
        recipe.setIngredient('C', Material.COAL);
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('M', Material.COPPER_INGOT);
        recipe.setIngredient('R', Material.REDSTONE);
        recipe.setIngredient('J', Material.JUKEBOX);
        recipe.setIngredient('L', Material.LAPIS_LAZULI);
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('A', Material.AMETHYST_SHARD);
        Bukkit.addRecipe(recipe);
    }

    public ItemStack makeTokenItem(NationDatabase.CoreIssue issued) {
        ItemStack item = new ItemStack(Material.LODESTONE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Rdzen Ochronny EarthSMP", NamedTextColor.AQUA));
        meta.lore(List.of(
                Component.text("Wlasnosc panstwa (ID przypisane w bazie)", NamedTextColor.GRAY),
                Component.text("Lider/zastepca stawia na swoim terytorium", NamedTextColor.GRAY),
                Component.text("65 x 65 blokow ochrony", NamedTextColor.GOLD)));
        meta.getPersistentDataContainer().set(tokenIdKey, PersistentDataType.STRING, issued.tokenId());
        meta.getPersistentDataContainer().set(nationIdKey, PersistentDataType.STRING, issued.nationId());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makePreview() {
        ItemStack item = new ItemStack(Material.LODESTONE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Rdzen Ochronny (64 surowce w kazdym slocie)", NamedTextColor.AQUA));
        meta.getPersistentDataContainer().set(previewKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isOurRecipe(org.bukkit.inventory.Recipe recipe) {
        return recipe instanceof ShapedRecipe shaped && shaped.getKey().equals(recipeKey);
    }

    private static boolean fullMatrix(ItemStack[] matrix) {
        if (matrix.length != 9) return false;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = matrix[i];
            int expected = i == 4 ? 1 : 64;
            if (stack == null || stack.getType() != INGREDIENTS[i] || stack.getAmount() != expected) return false;
            // Unmodified vanilla ingredients only. Never accept special core blocks as ingredients.
            if (stack.hasItemMeta()) return false;
        }
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepare(PrepareItemCraftEvent event) {
        if (!isOurRecipe(event.getRecipe())) return;
        if (!(event.getView().getPlayer() instanceof Player player) ||
                !fullMatrix(event.getInventory().getMatrix())) {
            event.getInventory().setResult(null); return;
        }
        try {
            event.getInventory().setResult(plugin.getNationDatabase().canCraftCore(player.getUniqueId())
                    ? makePreview() : null);
        } catch (SQLException error) {
            plugin.getLogger().log(Level.SEVERE, "Cannot check core crafting", error);
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!isOurRecipe(event.getRecipe())) return;
        event.setCancelled(true); // NEVER let vanilla consume only one item per ingredient slot.
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClick() != ClickType.LEFT ||
                (player.getItemOnCursor() != null && !player.getItemOnCursor().getType().isAir())) {
            player.sendMessage(Component.text("Rdzen: zwykly lewy klik pustym kursorem (bez Shift).", NamedTextColor.YELLOW));
            return;
        }
        CraftingInventory crafting = event.getInventory();
        if (!fullMatrix(crafting.getMatrix())) {
            player.sendMessage(Component.text("Potrzeba 64 sztuk w kazdym slocie i 1 jukeboxa.", NamedTextColor.YELLOW));
            return;
        }
        try {
            var issued = plugin.getNationDatabase().issueCoreToken(player.getUniqueId(), false);
            if (issued.status() != NationDatabase.CoreIssueStatus.ISSUED) {
                player.sendMessage(Component.text("Nie mozesz wykonac rdzenia: wymagany lider/zastepca, limit 2 na panstwo.", NamedTextColor.RED));
                return;
            }
            // Click is cancelled, so vanilla never touches the matrix or result slot.
            // Clear all 9 exact stacks ourselves, then hand out the unique database-backed item.
            crafting.setMatrix(new ItemStack[9]);
            crafting.setResult(null);
            player.setItemOnCursor(makeTokenItem(issued));
            player.updateInventory();
            player.sendMessage(Component.text("Wykonano rdzen. Zuzyto dokladnie 512 surowcow i 1 jukebox.", NamedTextColor.GREEN));
        } catch (SQLException error) {
            plugin.getLogger().log(Level.SEVERE, "Could not craft protection core", error);
            player.sendMessage(Component.text("Blad bazy. Surowce pozostaly w craftingu.", NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta.getPersistentDataContainer().has(previewKey, PersistentDataType.BYTE)) {
            event.setCancelled(true); return;
        }
        String tokenId = meta.getPersistentDataContainer().get(tokenIdKey, PersistentDataType.STRING);
        if (tokenId == null) return;
        String nationId = meta.getPersistentDataContainer().get(nationIdKey, PersistentDataType.STRING);
        Player player = event.getPlayer();
        Location location = event.getBlockPlaced().getLocation();
        World world = location.getWorld();
        if (item.getType() != Material.LODESTONE || nationId == null ||
                !world.getName().equals(plugin.getConfig().getString("world-name", "Earth")) ||
                !insideBorder(world, location)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Rdzen wymaga swiata Earth i obszaru 65x65 wewnatrz granicy.", NamedTextColor.RED));
            return;
        }
        var zoneAudit = CoreCaptureZone.audit(world, location.getBlockX(), location.getBlockY(),
                location.getBlockZ());
        if (!zoneAudit.clear()) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Nie mozna ustawic rdzenia: strefa 9x9 ma " +
                    zoneAudit.violations() + " zabronionych blokow. Pierwszy: " +
                    zoneAudit.first(), NamedTextColor.RED));
            return;
        }
        try {
            var result = plugin.getNationDatabase().placeCoreToken(player.getUniqueId(), tokenId, nationId,
                    world.getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
            if (result != NationDatabase.TokenPlaceResult.PLACED) {
                event.setCancelled(true);
                player.sendMessage(Component.text(switch (result) {
                    case INVALID_TOKEN -> "Ten przedmiot rdzenia nie istnieje w bazie (lub zmieniono jego ID).";
                    case ALREADY_PLACED -> "Ten rdzen zostal juz postawiony. Kopia przedmiotu jest niewazna.";
                    case NOT_AUTHORIZED -> "Rdzen moze stawiac lider lub zastepca panstwa, do ktorego nalezy.";
                    case NO_CLAIM -> "Srodek rdzenia musi byc na zatwierdzonym terytorium twojego panstwa.";
                    case OVERLAP -> "Ochrona 65x65 nachodzilaby na inny rdzen.";
                    case AT_WAR -> "Panstwo uczestniczy w wojnie: stawianie/przenoszenie rdzeni zablokowane.";
                    default -> "Nie mozna ustawic rdzenia.";
                }, NamedTextColor.RED));
                return;
            }
            plugin.getCoreProtection().reload();
            // One tick later avoids refreshing while Bukkit is still processing BlockPlaceEvent.
            plugin.getServer().getScheduler().runTask(plugin, plugin::refreshCoreMap);
            player.sendMessage(Component.text("Rdzen postawiony. Ochrona 65x65 aktywna!", NamedTextColor.GREEN));
        } catch (SQLException error) {
            event.setCancelled(true);
            plugin.getLogger().log(Level.SEVERE, "Could not place protection core", error);
            player.sendMessage(Component.text("Blad SQLite. Rdzen nie zostal ustawiony.", NamedTextColor.RED));
        }
    }

    private static boolean insideBorder(World world, Location anchor) {
        int x = anchor.getBlockX(), y = anchor.getBlockY(), z = anchor.getBlockZ();
        return world.getWorldBorder().isInside(new Location(world,x-31.5,y,z-31.5))
                && world.getWorldBorder().isInside(new Location(world,x+32.5,y,z+32.5))
                && world.getWorldBorder().isInside(new Location(world,x-31.5,y,z+32.5))
                && world.getWorldBorder().isInside(new Location(world,x+32.5,y,z-31.5));
    }
}
