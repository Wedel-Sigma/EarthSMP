package io.github.wedelsigma.earthsmp.core;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** 128 resources + plain diamond pickaxe; 15 legal, DB-backed breaks. */
public final class SiegePickaxe implements Listener {
    private static final int MAX_USES = 15;
    private static final Material[] INGREDIENTS = {
            Material.COAL, Material.IRON_INGOT, Material.COPPER_INGOT,
            Material.REDSTONE, Material.DIAMOND_PICKAXE, Material.LAPIS_LAZULI,
            Material.GOLD_INGOT, Material.DIAMOND, Material.AMETHYST_SHARD
    };
    private final EarthSmpCorePlugin plugin;
    private final NamespacedKey recipeKey, idKey, previewKey, speedKey;


    public SiegePickaxe(EarthSmpCorePlugin plugin) {
        this.plugin=plugin;
        recipeKey=new NamespacedKey(plugin,"siege_pickaxe_recipe");
        idKey=new NamespacedKey(plugin,"siege_pickaxe_id");
        previewKey=new NamespacedKey(plugin,"siege_pickaxe_preview");
        speedKey=new NamespacedKey(plugin,"siege_pickaxe_iron_speed");
        ShapedRecipe recipe=new ShapedRecipe(recipeKey, preview());
        recipe.shape("CIM","RPL","GDA");
        recipe.setIngredient('C',Material.COAL);
        recipe.setIngredient('I',Material.IRON_INGOT);
        recipe.setIngredient('M',Material.COPPER_INGOT);
        recipe.setIngredient('R',Material.REDSTONE);
        recipe.setIngredient('P',Material.DIAMOND_PICKAXE);
        recipe.setIngredient('L',Material.LAPIS_LAZULI);
        recipe.setIngredient('G',Material.GOLD_INGOT);
        recipe.setIngredient('D',Material.DIAMOND);
        recipe.setIngredient('A',Material.AMETHYST_SHARD);
        Bukkit.addRecipe(recipe);
    }

    private ItemStack preview() {
        ItemStack item=new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta=item.getItemMeta();
        meta.displayName(Component.text("Kilof Oblezniczy (po 16 surowcow)",NamedTextColor.GOLD));
        meta.getPersistentDataContainer().set(previewKey,PersistentDataType.BYTE,(byte)1);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack signedItem(String id,int remaining) {
        ItemStack item=new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta=item.getItemMeta();
        meta.displayName(Component.text("Kilof Oblezniczy EarthSMP",NamedTextColor.GOLD));
        meta.lore(List.of(
                Component.text("Tylko legalny szturm na wroga baze",NamedTextColor.GRAY),
                Component.text("Tylko bloki postawione przez graczy",NamedTextColor.GRAY),
                Component.text("Poza legalnym szturmem: nie kopie",NamedTextColor.GRAY),
                Component.text("Predkosc zelaznego kilofa (6)",NamedTextColor.GRAY),
                Component.text("Legalne zniszczenia: "+remaining+"/"+MAX_USES,NamedTextColor.YELLOW)));
        meta.setEnchantmentGlintOverride(true);
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(idKey,PersistentDataType.STRING,id);
        applyIronSpeed(meta);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * A diamond appearance/tier but iron mining speed (6, not vanilla diamond 8).
     * Rules preserve diamond-tier harvesting while deliberately rejecting blocks
     * tagged incorrect for diamond tools. No extra durability: the SQLite ledger
     * already enforces exactly fifteen LEGAL breaks.
     */
    private void applyIronSpeed(ItemMeta meta) {
        ToolComponent tool=meta.getTool();
        tool.setDefaultMiningSpeed(1.0f);
        tool.setRules(List.of());
        tool.addRule(Tag.INCORRECT_FOR_DIAMOND_TOOL, null, false);
        tool.addRule(Tag.MINEABLE_PICKAXE, 6.0f, true);
        tool.setDamagePerBlock(0);
        meta.setTool(tool);
        meta.getPersistentDataContainer().set(speedKey,PersistentDataType.BYTE,(byte)1);
    }

    /** Upgrade 1.7.0 pickaxes already in inventories, preserving the PDC UUID and uses. */
    private ItemStack upgrade(ItemStack stack) {
        if(!isSiegePickaxe(stack)) return stack;
        ItemMeta meta=stack.getItemMeta();
        Byte marker=meta.getPersistentDataContainer().get(speedKey,PersistentDataType.BYTE);
        if(stack.getType()==Material.DIAMOND_PICKAXE && marker!=null && marker==(byte)1) return stack;
        ItemStack copy=stack.clone();
        // Recover an older converted item instead of leaving an unbreakable netherite tool.
        copy.setType(Material.DIAMOND_PICKAXE);
        ItemMeta updated=copy.getItemMeta();
        updated.setUnbreakable(true);
        updated.setEnchantmentGlintOverride(true);
        applyIronSpeed(updated);
        copy.setItemMeta(updated);
        return copy;
    }

    private void upgradeHeld(Player player) {
        ItemStack old=player.getInventory().getItemInMainHand();
        ItemStack corrected=upgrade(old);
        if(corrected!=old) player.getInventory().setItemInMainHand(corrected);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        // An old pickaxe might be in any slot when a player returns after patching.
        Player player=event.getPlayer();
        ItemStack[] items=player.getInventory().getContents();
        boolean changed=false;
        for(int i=0;i<items.length;i++) {
            ItemStack corrected=upgrade(items[i]);
            if(corrected!=items[i]) { items[i]=corrected; changed=true; }
        }
        if(changed) player.getInventory().setContents(items);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onHeld(PlayerItemHeldEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> upgradeHeld(event.getPlayer()));
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void onDamage(BlockDamageEvent event) {
        Player player=event.getPlayer();
        if(!isSiegePickaxe(player.getInventory().getItemInMainHand())) return;
        upgradeHeld(player); // Handles old crafted tools retrieved from a chest.
        var core=plugin.getCoreProtection().findAt(event.getBlock());
        if(core.isEmpty() || !mayBreak(player,event.getBlock(),core.get())) {
            event.setCancelled(true); // Stop starting to mine outside an allowed siege.
            player.sendActionBar(Component.text("Kilof Oblezniczy: tylko legalny szturm!",NamedTextColor.RED));
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player=event.getPlayer();
        if(!isSiegePickaxe(player.getInventory().getItemInMainHand())) return;
        upgradeHeld(player);
        var core=plugin.getCoreProtection().findAt(event.getBlock());
        if(core.isEmpty() || !mayBreak(player,event.getBlock(),core.get())) {
            // Mandatory final gate, even for admins and instant-break operations.
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void onEnchant(EnchantItemEvent event) {
        if(isSiegePickaxe(event.getItem())) event.setCancelled(true);
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onAnvil(PrepareAnvilEvent event) {
        if(isSiegePickaxe(event.getInventory().getFirstItem())
                || isSiegePickaxe(event.getInventory().getSecondItem())) {
            event.setResult(null); // Cannot add Efficiency or repair/rename the special tool.
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onSmithing(PrepareSmithingEvent event) {
        if(isSiegePickaxe(event.getInventory().getInputEquipment())) event.setResult(null);
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onGrindstone(PrepareGrindstoneEvent event) {
        if(isSiegePickaxe(event.getInventory().getUpperItem())
                || isSiegePickaxe(event.getInventory().getLowerItem())) event.setResult(null);
    }

    private boolean ourRecipe(org.bukkit.inventory.Recipe recipe) {
        return recipe instanceof ShapedRecipe shaped && shaped.getKey().equals(recipeKey);
    }

    /** Accept at least sixteen in each perimeter slot, consume EXACTLY sixteen. */
    private boolean valid(ItemStack[] matrix) {
        if(matrix.length!=9) return false;
        for(int i=0;i<9;i++) {
            ItemStack stack=matrix[i];
            if(stack==null || stack.getType()!=INGREDIENTS[i]) return false;
            if(i==4) {
                if(stack.getAmount()!=1 || stack.hasItemMeta()) return false;
            } else {
                if(stack.getAmount()<16 || stack.hasItemMeta()) return false;
            }
        }
        return true;
    }

    private ItemStack[] remainingMatrix(ItemStack[] matrix) {
        ItemStack[] after=new ItemStack[9];
        for(int i=0;i<9;i++) {
            if(i==4) continue;
            int left=matrix[i].getAmount()-16;
            if(left>0) {
                after[i]=matrix[i].clone();
                after[i].setAmount(left);
            }
        }
        return after;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPrepare(PrepareItemCraftEvent event) {
        if(!ourRecipe(event.getRecipe())) return;
        event.getInventory().setResult(valid(event.getInventory().getMatrix())?preview():null);
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void onCraft(CraftItemEvent event) {
        if(!ourRecipe(event.getRecipe())) return;
        event.setCancelled(true); // Never let vanilla consume one item per slot.
        if(!(event.getWhoClicked() instanceof Player player)) return;
        if(event.getClick()!=ClickType.LEFT || !player.getItemOnCursor().getType().isAir()) {
            player.sendMessage(Component.text("Uzyj zwyklego lewego klikniecia pustym kursorem, bez Shift.",NamedTextColor.YELLOW));
            return;
        }
        CraftingInventory inventory=event.getInventory();
        ItemStack[] matrix=inventory.getMatrix();
        if(!valid(matrix)) {
            player.sendMessage(Component.text("Kilof: po 16 surowcow wokol 1 zwyklego diamentowego kilofa.",NamedTextColor.YELLOW));
            return;
        }
        String id=null;
        try {
            id=plugin.getSiegeStorage().issuePickaxe();
            ItemStack result=signedItem(id,MAX_USES);
            ItemStack[] after=remainingMatrix(matrix);
            inventory.setMatrix(after);
            inventory.setResult(null);
            player.setItemOnCursor(result);
            player.updateInventory();
            player.sendMessage(Component.text("Wykonano Kilof Oblezniczy: 15 legalnych zniszczen.",NamedTextColor.GREEN));
        } catch(SQLException | RuntimeException error) {
            plugin.getLogger().log(Level.SEVERE,"Siege pickaxe crafting failed",error);
            if(id!=null) try { plugin.getSiegeStorage().revokePickaxe(id); }
                catch(SQLException rollback) { error.addSuppressed(rollback); }
            player.sendMessage(Component.text("Blad tworzenia Kilofa Oblezniczego; zglos administratorowi.",NamedTextColor.RED));
        }
    }

    public boolean isSiegePickaxe(ItemStack item) { return id(item)!=null; }

    private String id(ItemStack item) {
        if(item==null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(idKey,PersistentDataType.STRING);
    }

    /** CoreProtection calls this only for protected blocks and non-owner players. */
    public boolean mayBreak(Player player, Block block, NationDatabase.ProtectionCore core) {
        String id=id(player.getInventory().getItemInMainHand());
        if(id==null || core.isAnchor(block.getX(),block.getY(),block.getZ())) return false;
        try {
            return plugin.getSieges().isLegalAttacker(player,core)
                    && plugin.getSiegeStorage().usesRemaining(id)>0
                    && plugin.getSiegeStorage().isPlayerPlaced(block.getWorld().getName(),
                            block.getX(),block.getY(),block.getZ(),block.getType().name());
        } catch(SQLException error) {
            plugin.getLogger().log(Level.SEVERE,"Siege break validation failed; denying",error);
            return false;
        }
    }

    /** Count ONLY successful (not cancelled) legal breaks. No normal mining consumes charges. */
    /** Invoked by BlockOriginListener at MONITOR, before deleting the provenance row. */
    public void onSuccessfulBreak(BlockBreakEvent event) {
        Block block=event.getBlock();
        var core=plugin.getCoreProtection().findAt(block);
        if(core.isEmpty()) return;
        Player player=event.getPlayer();
        String id=id(player.getInventory().getItemInMainHand());
        if(id==null) return;
        try {
            // Check again: admins/owners are not charged for unrelated ordinary mining.
            if(!plugin.getSieges().isLegalAttacker(player,core.get())) return;
            if(core.get().isAnchor(block.getX(),block.getY(),block.getZ())) return;
            if(!plugin.getSiegeStorage().isPlayerPlaced(block.getWorld().getName(),
                    block.getX(),block.getY(),block.getZ(),block.getType().name())) return;
            int after=plugin.getSiegeStorage().usePickaxe(id);
            if(after<0) {
                plugin.getLogger().severe("Illegal extra siege break avoided by prior HIGHEST validation; ID="+id);
                return;
            }
            if(after==0) {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                player.sendMessage(Component.text("Kilof Oblezniczy zuzyl 15. blok i rozpadl sie!",NamedTextColor.GOLD));
            } else {
                player.getInventory().setItemInMainHand(signedItem(id,after));
                player.sendMessage(Component.text("Kilof Oblezniczy: pozostalo "+after+"/15.",NamedTextColor.YELLOW));
            }
        } catch(SQLException error) {
            plugin.getLogger().log(Level.SEVERE,"Cannot debit siege pickaxe after block break",error);
            player.sendMessage(Component.text("KRYTYCZNY blad licznika kilofa. Zglos adminowi.",NamedTextColor.RED));
        }
    }
}
