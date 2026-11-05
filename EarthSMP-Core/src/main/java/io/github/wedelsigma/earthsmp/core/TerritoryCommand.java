package io.github.wedelsigma.earthsmp.core;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Political-territory commands. All selection positions are chunk coordinates, never blocks. */
public final class TerritoryCommand {
    private static final long MAX_SELECTION_CHUNKS = 10000L;
    private final EarthSmpCorePlugin plugin;
    private final Map<UUID, Selection> selections = new HashMap<>();

    private record ChunkPosition(String world, int x, int z) { }
    private record Rectangle(String world, int minX, int minZ, int maxX, int maxZ) {
        long count() { return (long) (maxX - minX + 1) * (maxZ - minZ + 1); }
    }
    private static final class Selection {
        ChunkPosition first;
        ChunkPosition second;
    }

    public TerritoryCommand(EarthSmpCorePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean handle(CommandSender sender, String[] args) {
        if (args.length < 2) { help(sender); return true; }
        String action = args[1].toLowerCase(Locale.ROOT);
        try {
            switch (action) {
                case "pos1", "pos2" -> saveCorner(sender, "pos1".equals(action));
                case "selection" -> showSelection(sender);
                case "here" -> showHere(sender);
                case "propose" -> {
                    if (args.length == 3 && ("add".equalsIgnoreCase(args[2]) || "remove".equalsIgnoreCase(args[2]))) {
                        propose(sender, "add".equalsIgnoreCase(args[2]));
                    } else help(sender);
                }
                case "requests" -> ownRequests(sender);
                case "cancel" -> {
                    if (args.length == 3) cancel(sender, args[2]); else help(sender);
                }
                case "pending", "approve", "reject", "adminclaim", "adminclear", "maprefresh" -> {
                    if (!sender.hasPermission("earthsmp.admin")) {
                        say(sender, "Tylko administrator moze zatwierdzac i recznie zmieniac granice.", NamedTextColor.RED);
                        break;
                    }
                    switch (action) {
                        case "pending" -> adminPending(sender);
                        case "maprefresh" -> say(sender,
                                plugin.refreshTerritoryMap() ? "squaremap: odswiezono warstwe panstw." :
                                        "Nie udalo sie odswiezyc squaremap. Sprawdz konsole.",
                                NamedTextColor.YELLOW);
                        case "approve", "reject" -> {
                            if (args.length == 3) review(sender, args[2], action.equals("approve"));
                            else help(sender);
                        }
                        case "adminclaim" -> {
                            if (args.length >= 3) adminClaim(sender, String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)));
                            else help(sender);
                        }
                        case "adminclear" -> adminClear(sender);
                        default -> { }
                    }
                }
                default -> help(sender);
            }
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "Territory command failed", exception);
            say(sender, "Blad SQLite. Sprawdz konsole serwera.", NamedTextColor.RED);
        }
        return true;
    }

    public List<String> tab(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> values = sender.hasPermission("earthsmp.admin")
                    ? List.of("pos1", "pos2", "selection", "here", "propose", "requests", "cancel", "pending", "approve", "reject", "adminclaim", "adminclear", "maprefresh")
                    : List.of("pos1", "pos2", "selection", "here", "propose", "requests", "cancel");
            return filter(values, args[1]);
        }
        if (args.length == 3 && "propose".equalsIgnoreCase(args[1])) return filter(List.of("add", "remove"), args[2]);
        return List.of();
    }

    private static List<String> filter(List<String> values, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(v -> v.toLowerCase(Locale.ROOT).startsWith(p)).toList();
    }

    private static void say(CommandSender sender, String text, NamedTextColor color) {
        sender.sendMessage(Component.text(text, color));
    }

    private static Player player(CommandSender sender) {
        if (sender instanceof Player p) return p;
        say(sender, "Ta komenda wymaga gracza na mapie Earth.", NamedTextColor.YELLOW);
        return null;
    }

    private World earthWorld() { return Bukkit.getWorld(plugin.getConfig().getString("world-name", "Earth")); }

    private ChunkPosition current(Player player) {
        World earth = earthWorld();
        if (earth == null || !earth.equals(player.getWorld())) {
            say(player, "Ta operacja dziala tylko w glownym swiecie Earth.", NamedTextColor.RED);
            return null;
        }
        Location location = player.getLocation();
        return new ChunkPosition(earth.getName(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    private void saveCorner(CommandSender sender, boolean first) {
        Player player = player(sender);
        if (player == null) return;
        ChunkPosition position = current(player);
        if (position == null) return;
        Selection selection = selections.computeIfAbsent(player.getUniqueId(), ignored -> new Selection());
        if (first) selection.first = position; else selection.second = position;
        say(sender, (first ? "Rog 1" : "Rog 2") + ": chunk X=" + position.x() + " Z=" + position.z(), NamedTextColor.GREEN);
    }

    private Rectangle rectangle(CommandSender sender) {
        Player player = player(sender);
        if (player == null) return null;
        Selection selection = selections.get(player.getUniqueId());
        if (selection == null || selection.first == null || selection.second == null) {
            say(sender, "Najpierw zaznacz dwa rogi: /esmp territory pos1 i pos2.", NamedTextColor.YELLOW);
            return null;
        }
        if (!selection.first.world().equals(selection.second.world())) {
            say(sender, "Oba rogi musza byc w tym samym swiecie.", NamedTextColor.RED);
            return null;
        }
        Rectangle r = new Rectangle(selection.first.world(),
                Math.min(selection.first.x(), selection.second.x()),
                Math.min(selection.first.z(), selection.second.z()),
                Math.max(selection.first.x(), selection.second.x()),
                Math.max(selection.first.z(), selection.second.z()));
        if (r.count() > MAX_SELECTION_CHUNKS) {
            say(sender, "Zaznaczenie jest zbyt duze: " + r.count() + " chunkow (limit 10000).", NamedTextColor.RED);
            return null;
        }
        // Border validation does not load or generate any chunks.
        World earth = earthWorld();
        if (earth == null || !r.world().equals(earth.getName()) || !insideBorder(earth, r)) {
            say(sender, "Zaznaczenie musi lezec w calosci wewnatrz worldbordera Earth.", NamedTextColor.RED);
            return null;
        }
        return r;
    }

    private static boolean insideBorder(World world, Rectangle r) {
        return world.getWorldBorder().isInside(new Location(world, (r.minX() << 4) + 8, 64, (r.minZ() << 4) + 8))
            && world.getWorldBorder().isInside(new Location(world, (r.maxX() << 4) + 8, 64, (r.maxZ() << 4) + 8));
    }

    private void showSelection(CommandSender sender) {
        Rectangle r = rectangle(sender);
        if (r != null) say(sender, "Zaznaczono " + r.count() + " chunkow (X=" + r.minX() + ".." + r.maxX()
                + ", Z=" + r.minZ() + ".." + r.maxZ() + ").", NamedTextColor.GREEN);
    }

    private void showHere(CommandSender sender) throws SQLException {
        Player player = player(sender);
        if (player == null) return;
        ChunkPosition p = current(player);
        if (p == null) return;
        Optional<NationDatabase.TerritoryOwner> owner = plugin.getNationDatabase().findTerritory(p.world(), p.x(), p.z());
        say(sender, "Chunk " + p.x() + ", " + p.z() + " | "
                + owner.map(o -> "Terytorium: " + o.nationName()).orElse("Teren neutralny"), NamedTextColor.GREEN);
    }

    private void propose(CommandSender sender, boolean add) throws SQLException {
        Rectangle r = rectangle(sender);
        if (r == null) return;
        Player player = (Player) sender;
        Optional<Long> id = plugin.getNationDatabase().submitTerritoryRequest(player.getUniqueId(), r.world(), add,
                r.minX(), r.minZ(), r.maxX(), r.maxZ());
        if (id.isEmpty()) {
            say(sender, "Zglosic granice moze tylko lider lub zastepca panstwa.", NamedTextColor.RED);
        } else {
            say(sender, "Zgloszono " + (add ? "przyznanie" : "oddanie") + " " + r.count()
                    + " chunkow. Wniosek #" + id.get() + " czeka na admina.", NamedTextColor.GREEN);
        }
    }

    private void ownRequests(CommandSender sender) throws SQLException {
        Player p = player(sender);
        if (p == null) return;
        showRequests(sender, plugin.getNationDatabase().findOwnPendingTerritoryRequests(p.getUniqueId()));
    }

    private void adminPending(CommandSender sender) throws SQLException {
        showRequests(sender, plugin.getNationDatabase().findPendingTerritoryRequests());
    }

    private void showRequests(CommandSender sender, List<NationDatabase.TerritoryRequest> requests) {
        if (requests.isEmpty()) { say(sender, "Brak oczekujacych wnioskow.", NamedTextColor.YELLOW); return; }
        for (NationDatabase.TerritoryRequest r : requests) {
            say(sender, "#" + r.id() + " " + r.nationName() + " " + r.action() + " " + r.size()
                    + " chunkow | X " + r.minX() + ".." + r.maxX()
                    + " Z " + r.minZ() + ".." + r.maxZ(), NamedTextColor.YELLOW);
        }
    }

    private static Long parseId(CommandSender sender, String input) {
        try { long id = Long.parseLong(input); if (id > 0) return id; }
        catch (NumberFormatException ignored) { }
        say(sender, "ID wniosku musi byc dodatnia liczba.", NamedTextColor.YELLOW);
        return null;
    }

    private void cancel(CommandSender sender, String rawId) throws SQLException {
        Player p = player(sender);
        if (p == null) return;
        Long id = parseId(sender, rawId);
        if (id == null) return;
        NationDatabase.TerritoryDecision result = plugin.getNationDatabase().cancelTerritoryRequest(id, p.getUniqueId());
        say(sender, result == NationDatabase.TerritoryDecision.CANCELLED ? "Wniosek anulowany." :
                "Nie znaleziono wniosku albo nie masz uprawnien.",
                result == NationDatabase.TerritoryDecision.CANCELLED ? NamedTextColor.GREEN : NamedTextColor.YELLOW);
    }

    private void review(CommandSender sender, String rawId, boolean approve) throws SQLException {
        Long id = parseId(sender, rawId);
        if (id == null) return;
        NationDatabase.TerritoryDecision result = plugin.getNationDatabase().reviewTerritory(id, sender.getName(), approve);
        switch (result) {
            case APPROVED -> {
                say(sender, "Wniosek #" + id + " zatwierdzony. Terytorium zapisane.", NamedTextColor.GREEN);
                plugin.refreshTerritoryMap();
            }
            case REJECTED -> say(sender, "Wniosek #" + id + " odrzucony.", NamedTextColor.YELLOW);
            case CONFLICT -> say(sender, "Konflikt granic: obszar zawiera chunki innego panstwa. Najpierw rozstrzygnij spor.", NamedTextColor.RED);
            default -> say(sender, "Nie ma takiego oczekujacego wniosku.", NamedTextColor.YELLOW);
        }
    }

    private void adminClaim(CommandSender sender, String nationName) throws SQLException {
        Rectangle r = rectangle(sender);
        if (r == null) return;
        if (plugin.getNationDatabase().adminAssignTerritory(sender.getName(), nationName, r.world(),
                r.minX(), r.minZ(), r.maxX(), r.maxZ())) {
            say(sender, "ADMIN: przypisano " + r.count() + " chunkow panstwu " + nationName + ".", NamedTextColor.GREEN);
            plugin.refreshTerritoryMap();
        } else {
            say(sender, "Nie znaleziono panstwa o nazwie: " + nationName, NamedTextColor.RED);
        }
    }

    private void adminClear(CommandSender sender) throws SQLException {
        Rectangle r = rectangle(sender);
        if (r == null) return;
        plugin.getNationDatabase().adminClearTerritory(sender.getName(), r.world(),
                r.minX(), r.minZ(), r.maxX(), r.maxZ());
        say(sender, "ADMIN: wyczyszczono polityczne granice w " + r.count() + " chunkach.", NamedTextColor.GREEN);
        plugin.refreshTerritoryMap();
    }

    private void help(CommandSender sender) {
        say(sender, "/esmp territory pos1 | pos2 | selection | here", NamedTextColor.YELLOW);
        say(sender, "/esmp territory propose add|remove | requests | cancel <id>", NamedTextColor.YELLOW);
        if (sender.hasPermission("earthsmp.admin")) {
            say(sender, "/esmp territory pending | approve <id> | reject <id>", NamedTextColor.GRAY);
            say(sender, "/esmp territory adminclaim <nazwa panstwa> | adminclear", NamedTextColor.GRAY);
            say(sender, "/esmp territory maprefresh", NamedTextColor.GRAY);
        }
    }
}
