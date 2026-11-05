package io.github.wedelsigma.earthsmp.core;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Admin-only temporary placement for testing the first free protection core. */
public final class CoreCommand {
    private final EarthSmpCorePlugin plugin;
    public CoreCommand(EarthSmpCorePlugin plugin) { this.plugin = plugin; }

    public boolean handle(CommandSender sender, String[] args) {
        String operation = args.length > 1 ? args[1].toLowerCase(java.util.Locale.ROOT) : "help";
        if (operation.equals("here")) {
            if (!(sender instanceof Player player)) { msg(sender, "Tylko w grze.", NamedTextColor.YELLOW); return true; }
            Optional<NationDatabase.ProtectionCore> core = plugin.getCoreProtection().findAt(player.getLocation());
            msg(sender, core.map(c -> "Strefa ochronna: " + c.nationName() + " (65x65)" )
                    .orElse("To miejsce nie jest chronione rdzeniem."), NamedTextColor.YELLOW);
            return true;
        }
        if (operation.equals("stock")) {
            if (!(sender instanceof Player player)) { msg(sender, "Tylko w grze.", NamedTextColor.YELLOW); return true; }
            try {
                var s = plugin.getNationDatabase().coreStock(player.getUniqueId());
                msg(sender, "Rdzenie: darmowy " + s.free() + "/1, wykonane " + s.crafted()
                        + "/2, postawione " + s.placed() + ".", NamedTextColor.YELLOW);
            } catch (SQLException error) { fail(sender, "Could not get core stock", error); }
            return true;
        }
        if (operation.equals("free")) {
            if (!(sender instanceof Player player)) { msg(sender, "Tylko w grze.", NamedTextColor.YELLOW); return true; }
            if (player.getInventory().firstEmpty() == -1) {
                msg(player, "Zwolnij jedno miejsce w ekwipunku.", NamedTextColor.YELLOW); return true;
            }
            try {
                NationDatabase.CoreIssue result = plugin.getNationDatabase().issueCoreToken(player.getUniqueId(), true);
                if (result.status() == NationDatabase.CoreIssueStatus.ISSUED) {
                    var item = plugin.getCoreItems().makeTokenItem(result);
                    var overflow = player.getInventory().addItem(item);
                    if (!overflow.isEmpty()) {
                        plugin.getNationDatabase().revokeUndeliveredCore(result.tokenId());
                        msg(player, "Nie udalo sie przekazac rdzenia, sprobuj ponownie.", NamedTextColor.RED);
                    } else {
                        msg(player, "Otrzymales darmowy rdzen swojego panstwa. Postaw go na swoim terytorium.", NamedTextColor.GREEN);
                    }
                } else if (result.status() == NationDatabase.CoreIssueStatus.ALREADY_FREE) {
                    msg(player, "Panstwo wykorzystalo juz prawo do jednego darmowego rdzenia.", NamedTextColor.YELLOW);
                } else {
                    msg(player, "Darmowy rdzen moze odebrac lider lub zastepca panstwa.", NamedTextColor.YELLOW);
                }
            } catch (SQLException error) { fail(player, "Could not issue initial core", error); }
            return true;
        }
        if (operation.equals("pickup")) {
            if (!(sender instanceof Player player)) { msg(sender, "Tylko w grze.", NamedTextColor.YELLOW); return true; }
            Block looked = player.getTargetBlockExact(6);
            if (looked == null) { msg(sender, "Spojrz bezposrednio na lodestone rdzenia (do 6 blokow).", NamedTextColor.YELLOW); return true; }
            pickup(player, looked);
            return true;
        }
        if (!sender.hasPermission("earthsmp.admin")) {
            msg(sender, "Ta komenda rdzeni jest dostepna tylko dla admina.", NamedTextColor.RED);
            return true;
        }
        if (operation.equals("zonecheck")) {
            if (!(sender instanceof Player player)) {
                msg(sender, "Tylko w grze.", NamedTextColor.YELLOW); return true;
            }
            Block looked = player.getTargetBlockExact(6);
            Optional<NationDatabase.ProtectionCore> found = looked == null ? Optional.empty()
                    : plugin.getCoreProtection().findAnchor(looked);
            if (found.isEmpty() || looked.getType() != Material.LODESTONE) {
                msg(sender, "Spojrz na lodestone istniejacego rdzenia (do 6 blokow).", NamedTextColor.YELLOW);
                return true;
            }
            var core = found.get();
            var audit = CoreCaptureZone.audit(player.getWorld(), core.x(), core.y(), core.z());
            if (audit.clear()) {
                msg(sender, "Strefa 9x9x16 jest czysta (Y " + (core.y()-5) + ".." + (core.y()+10) + ").",
                        NamedTextColor.GREEN);
            } else {
                msg(sender, "Uwaga: " + audit.violations() + " zakazanych blokow w strefie rdzenia. " +
                        "Pierwszy: " + audit.first(), NamedTextColor.RED);
            }
            return true;
        }
        if (operation.equals("capital")) {
            if (!(sender instanceof Player player)) { msg(sender, "Tylko w grze.", NamedTextColor.YELLOW); return true; }
            Block looked = player.getTargetBlockExact(6);
            Optional<NationDatabase.ProtectionCore> found = looked == null ? Optional.empty()
                    : plugin.getCoreProtection().findAnchor(looked);
            if (found.isEmpty() || looked.getType() != Material.LODESTONE) {
                msg(sender, "Spojrz na lodestone rdzenia, ktory ma zostac nowa stolica.", NamedTextColor.YELLOW);
                return true;
            }
            try {
                var result = plugin.getNationDatabase().adminSetCapital(found.get().id());
                if (result == NationDatabase.SetCapitalResult.UPDATED) {
                    plugin.getCoreProtection().reload();
                    msg(sender, "Nowa stolica panstwa " + found.get().nationName() + " wyznaczona.", NamedTextColor.GREEN);
                } else {
                    msg(sender, result == NationDatabase.SetCapitalResult.ALREADY_CAPITAL
                            ? "Ten rdzen jest juz stolica." : "Nie znaleziono rdzenia w bazie.", NamedTextColor.YELLOW);
                }
            } catch (SQLException error) { fail(sender, "Could not change nation capital", error); }
            return true;
        }
        if (operation.equals("list")) {
            List<NationDatabase.ProtectionCore> cores = plugin.getCoreProtection().all();
            msg(sender, "Rdzenie: " + cores.size() + " (koordynaty tylko dla admina)", NamedTextColor.YELLOW);
            for (var c : cores) msg(sender, c.nationName() + " @ " + c.x() + " " + c.y() + " " + c.z()
                    + (c.capital() ? " [STOLICA]" : ""), NamedTextColor.GRAY);
            return true;
        }
        if (!(sender instanceof Player player)) {
            msg(sender, "Ustawianie rdzenia wymaga gracza w grze.", NamedTextColor.YELLOW);
            return true;
        }
        if (operation.equals("place") || operation.equals("remove")) {
            Block looked = player.getTargetBlockExact(6);
            if (looked == null) { msg(sender, "Spojrz na blok z odleglosci do 6 kratek.", NamedTextColor.YELLOW); return true; }
            if (operation.equals("place")) place(player, looked); else remove(player, looked);
            return true;
        }
        msg(sender, "/esmp core here | stock | free | pickup | place | remove | capital | zonecheck | list", NamedTextColor.YELLOW);
        return true;
    }

    public List<String> tab(CommandSender sender, String[] args) {
        List<String> opts = sender.hasPermission("earthsmp.admin")
                ? List.of("here", "stock", "free", "pickup", "place", "remove", "capital", "zonecheck", "list")
                : List.of("here", "stock", "free", "pickup");
        String prefix = args.length > 1 ? args[1].toLowerCase(java.util.Locale.ROOT) : "";
        return opts.stream().filter(o -> o.startsWith(prefix)).toList();
    }

    /** Command-driven dismantling only: never let vanilla mining give out ordinary lodestones. */
    private void pickup(Player player, Block looked) {
        Optional<NationDatabase.ProtectionCore> found = plugin.getCoreProtection().findAnchor(looked);
        if (found.isEmpty() || looked.getType() != Material.LODESTONE) {
            msg(player, "Spojrz bezposrednio na postawiony rdzen (lodestone).", NamedTextColor.YELLOW);
            return;
        }
        PlayerInventory inventory = player.getInventory();
        int slot = -1;
        // Storage only: never place a lodestone token in armor/offhand equipment slots.
        ItemStack[] storage = inventory.getStorageContents();
        for (int i = 0; i < storage.length; i++) {
            if (storage[i] == null || storage[i].getType().isAir()) { slot = i; break; }
        }
        if (slot == -1) {
            msg(player, "Potrzebujesz wolnego slotu w ekwipunku na rdzen.", NamedTextColor.YELLOW);
            return;
        }
        // A currently active war must also block this operation once Diplomacy is implemented.
        NationDatabase.ProtectionCore original = found.get();
        try {
            var outcome = plugin.getNationDatabase().pickupOwnCore(player.getUniqueId(), original.id());
            if (outcome.status() != NationDatabase.CorePickupStatus.PICKED_UP) {
                String reason = switch (outcome.status()) {
                    case NOT_FOUND -> "Nie znaleziono tego rdzenia w bazie.";
                    case NOT_AUTHORIZED -> "Tylko lider lub zastepca wlasciciela moze przenosic wlasny rdzen.";
                    case CAPITAL -> "To stolica. Admin musi najpierw wskazac inny rdzen jako stolice.";
                    case CONQUERED -> "Zdobytych rdzeni nie wolno demontowac.";
                    case AT_WAR -> "Nie wolno przenosic rdzeni podczas jakiejkolwiek wojny panstwa.";
                    case TOKEN_MISMATCH -> "Dane przedmiotu rdzenia nie zgadzaja sie z baza. Zglos to adminowi.";
                    default -> "Nie mozna zdjac rdzenia.";
                };
                msg(player, reason, NamedTextColor.YELLOW);
                return;
            }
            ItemStack tokenItem = plugin.getCoreItems().makeTokenItem(new NationDatabase.CoreIssue(
                    NationDatabase.CoreIssueStatus.ISSUED, outcome.tokenId(), outcome.nationId(), outcome.origin()));
            boolean handedOver = false;
            try {
                ItemStack current = inventory.getItem(slot);
                if (current != null && !current.getType().isAir()) {
                    throw new IllegalStateException("Reserved inventory slot no longer empty");
                }
                inventory.setItem(slot, tokenItem);
                handedOver = true;
                looked.setType(Material.AIR, false);
                plugin.getCoreProtection().reload();
                msg(player, "Rdzen spakowany do ekwipunku. Zachowal swoje ID i nie zuzywa nowego limitu.", NamedTextColor.GREEN);
            } catch (SQLException | RuntimeException deliveryError) {
                plugin.getLogger().log(Level.SEVERE, "Could not complete core pickup; attempting rollback", deliveryError);
                try {
                    // Restore database first. On failure keep the item, as it may be the only recovery copy.
                    plugin.getNationDatabase().restorePickedUpCore(original, player.getUniqueId());
                    looked.setType(Material.LODESTONE, false);
                    plugin.getCoreProtection().reload();
                    if (handedOver) inventory.setItem(slot, null);
                    msg(player, "Przeniesienie anulowane; rdzen pozostaje na miejscu.", NamedTextColor.RED);
                } catch (SQLException | RuntimeException restoreError) {
                    plugin.getLogger().log(Level.SEVERE, "CRITICAL: Core pickup rollback FAILED for "
                            + original.id() + ". Restore synchronized world + DB backup.", restoreError);
                    msg(player, "KRYTYCZNY blad przenoszenia rdzenia. Nie ruszaj bloku ani przedmiotu; zglos adminowi!",
                            NamedTextColor.RED);
                }
            }
        } catch (SQLException error) { fail(player, "Could not pick up protection core", error); }
    }

    private void place(Player player, Block ground) {
        World world = player.getWorld();
        if (!world.getName().equals(plugin.getConfig().getString("world-name", "Earth"))) {
            msg(player, "Rdzen mozna ustawic tylko w swiecie Earth.", NamedTextColor.RED); return;
        }
        Block target = ground.getRelative(BlockFace.UP);
        if (!ground.getType().isSolid() || !target.getType().isAir()) {
            msg(player, "Celuj w pelny blok podloza z wolnym miejscem nad nim.", NamedTextColor.YELLOW); return;
        }
        if (target.getY() < world.getMinHeight() || target.getY() >= world.getMaxHeight()) {
            msg(player, "Wysokosc poza swiatem.", NamedTextColor.RED); return;
        }
        int x = target.getX(), y = target.getY(), z = target.getZ();
        if (!world.getWorldBorder().isInside(new Location(world, x - 31.5, y, z - 31.5)) ||
            !world.getWorldBorder().isInside(new Location(world, x + 32.5, y, z + 32.5)) ||
            !world.getWorldBorder().isInside(new Location(world, x - 31.5, y, z + 32.5)) ||
            !world.getWorldBorder().isInside(new Location(world, x + 32.5, y, z - 31.5))) {
            msg(player, "Caly obszar 65x65 musi byc wewnatrz worldbordera.", NamedTextColor.RED); return;
        }
        var zoneAudit = CoreCaptureZone.audit(world, x, y, z);
        if (!zoneAudit.clear()) {
            msg(player, "Nie mozna ustawic rdzenia: w strefie 9x9 jest " + zoneAudit.violations() +
                    " zabronionych blokow. Pierwszy: " + zoneAudit.first(), NamedTextColor.RED);
            return;
        }
        try {
            NationDatabase.CorePlacement result = plugin.getNationDatabase().placeInitialCore(
                    player.getUniqueId(), world.getName(), x, y, z);
            switch (result) {
                case PLACED -> {
                    target.setType(Material.LODESTONE, false);
                    plugin.getCoreProtection().reload();
                    msg(player, "Ustawiono pierwszy testowy rdzen. Ochrona 65x65 aktywna.", NamedTextColor.GREEN);
                }
                case NO_NATION -> msg(player, "Nie nalezysz do panstwa.", NamedTextColor.YELLOW);
                case NO_CLAIM -> msg(player, "Srodek rdzenia musi byc na zatwierdzonym terytorium twojego panstwa.", NamedTextColor.YELLOW);
                case ALREADY_HAS_FREE_CORE -> msg(player, "Panstwo ma juz swoj darmowy rdzen.", NamedTextColor.YELLOW);
                case OVERLAP -> msg(player, "Strefa 65x65 nachodzilaby na inny rdzen.", NamedTextColor.YELLOW);
                case AT_WAR -> msg(player, "Podczas wojny nie mozna stawiac ani przenosic rdzeni.", NamedTextColor.RED);
            }
        } catch (SQLException error) { fail(player, "Could not place core", error); }
    }

    private void remove(Player player, Block looked) {
        Optional<NationDatabase.ProtectionCore> found = plugin.getCoreProtection().findAnchor(looked);
        if (found.isEmpty()) {
            msg(player, "Spojrz dokladnie na blok rdzenia (lodestone).", NamedTextColor.YELLOW); return;
        }
        try {
            if (plugin.getNationDatabase().adminRemoveCore(found.get().id())) {
                looked.setType(Material.AIR, false);
                plugin.getCoreProtection().reload();
                msg(player, "Testowy rdzen usuniety z bazy i swiata.", NamedTextColor.GREEN);
            }
        } catch (SQLException error) { fail(player, "Could not remove core", error); }
    }

    private void fail(CommandSender sender, String context, SQLException error) {
        plugin.getLogger().log(Level.SEVERE, context, error);
        msg(sender, "Blad bazy. Sprawdz konsole serwera.", NamedTextColor.RED);
    }
    private static void msg(CommandSender sender, String text, NamedTextColor color) {
        sender.sendMessage(Component.text(text, color));
    }
}
