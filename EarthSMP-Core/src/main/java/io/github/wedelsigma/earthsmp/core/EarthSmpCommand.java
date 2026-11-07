package io.github.wedelsigma.earthsmp.core;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class EarthSmpCommand implements CommandExecutor, TabCompleter {

    private final EarthSmpCorePlugin plugin;

    public EarthSmpCommand(EarthSmpCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("menu"))) {
            return showHubMenu(sender);
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("tierlist")) {
            Player player = requirePlayer(sender);
            if (player != null) plugin.getTierlist().open(player);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("sidebar")) {
            Player player = requirePlayer(sender);
            if (player != null) {
                boolean enabled = plugin.getSidebar().toggle(player);
                send(sender, enabled ? "Wlaczono panel " + plugin.getDisplayName() + "." : "Wylaczono panel " + plugin.getDisplayName() + ".",
                        enabled ? NamedTextColor.GREEN : NamedTextColor.YELLOW);
            }
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("map")) {
            Player player = requirePlayer(sender);
            if (player == null) return true;
            MapPortalAuth auth = plugin.getMapPortalAuth();
            if (auth == null || plugin.getMapPortalUrl() == null) {
                send(sender, "Portal mapy jest wylaczony. Sprawdz konfiguracje i konsole.", NamedTextColor.RED);
                return true;
            }
            try {
                if (args.length == 2 && args[1].equalsIgnoreCase("code")) {
                    var code = auth.issue(player.getUniqueId(), player.getName());
                    if (code.isEmpty()) {
                        send(sender, "Konto mapy juz powiazane. Zaloguj sie nickiem i haslem. Zapomniane haslo? /esmp map reset", NamedTextColor.YELLOW);
                    } else {
                        send(sender, "JEDNORAZOWY kod powiazania konta (5 min): " + code.get(), NamedTextColor.GREEN);
                        send(sender, "Wejdz na: " + plugin.getMapPortalUrl() + " i ustaw haslo (min. 12 znakow).", NamedTextColor.YELLOW);
                        send(sender, "Po powiazaniu mozesz logowac sie z dowolnego urzadzenia bez Minecrafta.", NamedTextColor.GRAY);
                    }
                } else if (args.length == 2 && args[1].equalsIgnoreCase("revoke")) {
                    auth.revoke(player.getUniqueId());
                    send(sender, "Wszystkie sesje przegladarek zostaly uniewaznione. Konto i haslo pozostaja aktywne.", NamedTextColor.GREEN);
                } else if (args.length == 2 && args[1].equalsIgnoreCase("reset")) {
                    auth.reset(player.getUniqueId());
                    send(sender, "Powiazanie konta usuniete, sesje uniewaznione. Utworz nowe przez /esmp map code.", NamedTextColor.GREEN);
                } else {
                    send(sender, "/esmp map code (pierwsze powiazanie) | map revoke (uniewaznij sesje) | map reset (usun konto)", NamedTextColor.YELLOW);
                }
            } catch (SQLException error) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Map account operation failed", error);
                send(sender, "Blad konta mapy. Sprawdz konsole.", NamedTextColor.RED);
            }
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("clock")) {
            if (!requireAdmin(sender)) return true;
            return showClock(sender, args);
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("home")) {
            Player player = requirePlayer(sender);
            if (player != null) plugin.getHomeCommand().execute(player);
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("security")) {
            if (!requireAdmin(sender)) return true;
            int limit = 10;
            if (args.length == 2) {
                try { limit = Integer.parseInt(args[1]); }
                catch (NumberFormatException error) {
                    send(sender, "Uzycie: /esmp security [1-30]", NamedTextColor.YELLOW);
                    return true;
                }
            } else if (args.length > 2) {
                send(sender, "Uzycie: /esmp security [1-30]", NamedTextColor.YELLOW);
                return true;
            }
            limit = Math.max(1, Math.min(30, limit));
            var entries = plugin.getSecurityAlerts().recent(limit);
            send(sender, "Ostatnie zdarzenia EarthSMP Security (" + entries.size() + "):", NamedTextColor.GOLD);
            if (entries.isEmpty()) send(sender, "Brak zdarzen od startu pluginu.", NamedTextColor.GRAY);
            for (String entry : entries) send(sender, entry, NamedTextColor.GRAY);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            if (!requireAdmin(sender)) return true;
            return showStatus(sender);
        }
        if (args.length >= 1 && (args[0].equalsIgnoreCase("war") || args[0].equalsIgnoreCase("diplomacy"))) {
            return plugin.getWarCommand().handle(sender, args);
        }
        if (args.length >= 1 && (args[0].equalsIgnoreCase("siege") || args[0].equalsIgnoreCase("oblez") || args[0].equalsIgnoreCase("oblezenie"))) {
            return plugin.getSiegeCommand().handle(sender, args);
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("core")) {
            return plugin.getCoreCommand().handle(sender, args);
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("territory")) {
            return plugin.getTerritoryCommand().handle(sender, args);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("nation")) {
            String operation = args[1].toLowerCase(Locale.ROOT);
            switch (operation) {
                case "menu", "gui" -> {
                    if (args.length == 2) return showNationMenu(sender);
                }
                case "info" -> {
                    if (args.length == 2) return showOwnNation(sender);
                }
                case "create" -> {
                    if (args.length >= 3) return createNation(sender, joined(args, 2));
                }
                case "leave" -> {
                    if (args.length == 2) return leaveNation(sender);
                }
                case "disband" -> {
                    if (args.length == 2) {
                        Player player = requirePlayer(sender);
                        if (player != null) plugin.getNationMenu().openDisbandConfirmation(player);
                        return true;
                    }
                }
                case "transfer" -> {
                    if (args.length == 3) return transferNation(sender, args[2]);
                }
                case "members" -> {
                    if (args.length == 2) return showMembers(sender);
                }
                case "invites" -> {
                    if (args.length == 2) return showInvites(sender);
                }
                case "invite" -> {
                    if (args.length == 3) return invitePlayer(sender, args[2]);
                }
                case "accept" -> {
                    if (args.length >= 3) return respondInvite(sender, joined(args, 2), true);
                }
                case "decline" -> {
                    if (args.length >= 3) return respondInvite(sender, joined(args, 2), false);
                }
                case "promote", "demote", "kick" -> {
                    if (args.length == 3) return changeMember(sender, args[2], operation);
                }
                default -> { }
            }
        }
        showHelp(sender, label);
        return true;
    }

    private boolean showHubMenu(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) return true;
        plugin.getHubMenu().openMain(player);
        return true;
    }

    private boolean showNationMenu(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) return true;
        plugin.getNationMenu().openMain(player);
        return true;
    }

    private static String joined(String[] args, int first) {
        return String.join(" ", Arrays.copyOfRange(args, first, args.length)).trim();
    }

    private static boolean requireAdmin(CommandSender sender) {
        if (sender.hasPermission("earthsmp.admin")) return true;
        send(sender, "Ta komenda jest tylko dla administratorow.", NamedTextColor.RED);
        return false;
    }

    private static @Nullable Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) return player;
        send(sender, "Ta komenda wymaga gracza w grze.", NamedTextColor.YELLOW);
        return null;
    }

    private static void send(CommandSender sender, String message, NamedTextColor color) {
        sender.sendMessage(Component.text(message, color));
    }

    private static void showHelp(CommandSender sender, String label) {
        send(sender, "/" + label + " (centralne GUI) | nation menu | nation info | members | invites", NamedTextColor.YELLOW);
        send(sender, "/" + label + " tierlist | sidebar (ranking i HUD)", NamedTextColor.YELLOW);
        send(sender, "/" + label + " territory (granice polityczne)", NamedTextColor.YELLOW);
        send(sender, "/" + label + " war (wojny, sojusznicy i pokoj)", NamedTextColor.YELLOW);
        send(sender, "/" + label + " nation create <nazwa> | leave | disband | transfer <nick>", NamedTextColor.YELLOW);
        send(sender, "/home lub /" + label + " home (powrot do stolicy)", NamedTextColor.YELLOW);
        send(sender, "/" + label + " map code | map revoke | map reset (konto na mapie WWW)", NamedTextColor.YELLOW);
        send(sender, "/" + label + " nation invite <nick> | accept <panstwo> | decline <panstwo>", NamedTextColor.YELLOW);
        send(sender, "/" + label + " nation promote <nick> | demote <nick> | kick <nick>", NamedTextColor.YELLOW);
        if (sender.hasPermission("earthsmp.admin")) {
            send(sender, "/" + label + " status | clock | clock start | security [1-30]", NamedTextColor.GRAY);
        }
    }

    private boolean showStatus(CommandSender sender) {
        String worldName = plugin.getConfig().getString("world-name", "Earth");
        boolean worldLoaded = Bukkit.getWorld(worldName) != null;
        send(sender, plugin.getDisplayName() + " | Core " + plugin.getDescription().getVersion() + " - plugin dziala.", NamedTextColor.GREEN);
        send(sender, "Swiat " + worldName + ": " + (worldLoaded ? "zaladowany" : "nie znaleziono"),
                worldLoaded ? NamedTextColor.GREEN : NamedTextColor.RED);
        try {
            send(sender, "SQLite: aktywne | Panstwa: " + plugin.getNationDatabase().countNations(), NamedTextColor.GREEN);
        } catch (SQLException error) {
            dbError(sender, "Nation status query failed", error);
        }
        send(sender, "GUI, terytoria, rdzenie i dyplomacja: aktywne.", NamedTextColor.GRAY);
        try {
            send(sender, "Aktywne wojny: " + plugin.getWars().activeWars().size(), NamedTextColor.YELLOW);
            send(sender, "Aktywne oblezenia: " + plugin.getSieges().activeCount(), NamedTextColor.YELLOW);
        } catch (SQLException error) { dbError(sender, "War status query failed", error); }
        return true;
    }

    private boolean showClock(CommandSender sender, String[] args) {
        try {
            var clock = plugin.getServerClock();
            if (args.length == 2 && args[1].equalsIgnoreCase("start")) {
                if (plugin.getNationDatabase().startEdition(clock.now())) {
                    send(sender, "Edycja " + plugin.getDisplayName() + " rozpoczeta! Dzien 1. Reset dni o 06:00 Europe/Warsaw.",
                            NamedTextColor.GREEN);
                } else {
                    send(sender, "Edycja zostala juz rozpoczeta. Nie resetuje zegara.", NamedTextColor.YELLOW);
                }
            } else if (args.length != 1) {
                send(sender, "Uzycie: /esmp clock [start] (start tylko raz)", NamedTextColor.YELLOW);
                return true;
            }
            Instant now = clock.now();
            var start = plugin.getNationDatabase().editionStart();
            int day = clock.day(start, now);
            send(sender, day == 0 ? "Edycja jeszcze nie rozpoczeta."
                    : "Dzien serwera: " + day, NamedTextColor.GOLD);
            if (start.isPresent()) {
                send(sender, "Start: " + DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm z")
                        .format(start.get().atZone(ServerClock.ZONE)), NamedTextColor.GRAY);
            }
            var next = clock.nextReset(now);
            Duration left = clock.untilReset(now);
            send(sender, "Nastepny dzien: " + next.format(DateTimeFormatter.ofPattern("dd.MM HH:mm z"))
                    + " (za " + left.toHours() + "h " + left.toMinutesPart() + "min)", NamedTextColor.YELLOW);
        } catch (SQLException e) { dbError(sender, "Edition clock error", e); }
        return true;
    }

    private boolean showOwnNation(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) return true;
        try {
            Optional<NationDatabase.Membership> membership = plugin.getNationDatabase().findMembership(player.getUniqueId());
            if (membership.isEmpty()) {
                send(sender, "Nie nalezysz do zadnego panstwa.", NamedTextColor.YELLOW);
            } else {
                NationDatabase.Membership member = membership.get();
                send(sender, "Panstwo: " + member.nationName() + " | Ranga: " + member.role(), NamedTextColor.GREEN);
                send(sender, "ID: " + member.nationId(), NamedTextColor.GRAY);
            }
        } catch (SQLException error) {
            dbError(sender, "Nation membership lookup failed", error);
        }
        return true;
    }

    private boolean createNation(CommandSender sender, String rawName) {
        Player player = requirePlayer(sender);
        if (player != null) plugin.getNationMenu().createNation(player, rawName);
        return true;
    }

    private boolean leaveNation(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) return true;
        try {
            NationDatabase.LeaveResult result = plugin.getNationDatabase().leaveNation(player.getUniqueId());
            switch (result) {
                case LEFT -> send(sender, "Opusciles swoje panstwo.", NamedTextColor.GREEN);
                case NOT_MEMBER -> send(sender, "Nie nalezysz do zadnego panstwa.", NamedTextColor.YELLOW);
                case LEADER_MUST_TRANSFER -> send(sender,
                        "Najpierw przekaz przywodztwo z menu Czlonkowie. Rozwiazanie panstwa bedzie pozniej.",
                        NamedTextColor.YELLOW);
            }
        } catch (SQLException error) {
            dbError(sender, "Leaving nation failed", error);
        }
        return true;
    }

    private boolean transferNation(CommandSender sender, String nick) {
        Player player = requirePlayer(sender);
        if (player == null) return true;
        try {
            Optional<NationDatabase.Membership> membership = plugin.getNationDatabase().findMembership(player.getUniqueId());
            if (membership.isEmpty() || !"LEADER".equals(membership.get().role())) {
                send(sender, "Tylko aktualny lider moze przekazac przywodztwo.", NamedTextColor.RED);
                return true;
            }
            UUID target = null;
            for (NationDatabase.NationMember member : plugin.getNationDatabase().findMembers(membership.get().nationId())) {
                String displayName = Bukkit.getOfflinePlayer(member.playerUuid()).getName();
                if (displayName != null && displayName.equalsIgnoreCase(nick)) {
                    target = member.playerUuid();
                    break;
                }
            }
            if (target == null) {
                send(sender, "Nie znaleziono takiego czlonka w panstwie.", NamedTextColor.YELLOW);
                return true;
            }
            send(sender, "Przekazanie przywodztwa wymaga potwierdzenia w GUI.", NamedTextColor.YELLOW);
            plugin.getNationMenu().openTransferConfirmation(player, target, 0);
        } catch (SQLException error) {
            dbError(sender, "Nation transfer lookup failed", error);
        }
        return true;
    }

    private boolean invitePlayer(CommandSender sender, String name) {
        Player actor = requirePlayer(sender);
        if (actor == null) return true;
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            send(sender, "Gracz musi byc online. Nie znaleziono: " + name, NamedTextColor.YELLOW);
            return true;
        }
        try {
            NationDatabase.InviteResult result = plugin.getNationDatabase().invite(actor.getUniqueId(), target.getUniqueId());
            switch (result) {
                case SENT -> {
                    NationDatabase.Membership nation = plugin.getNationDatabase()
                            .findMembership(actor.getUniqueId()).orElseThrow();
                    send(sender, "Wyslano zaproszenie do " + target.getName() + " (wazne 24h).", NamedTextColor.GREEN);
                    send(target, "Zaproszenie do panstwa " + nation.nationName() + " od " + actor.getName(), NamedTextColor.GREEN);
                    send(target, "Akceptuj: /earthsmp nation accept " + nation.nationName(), NamedTextColor.YELLOW);
                }
                case NOT_AUTHORIZED -> send(sender, "Zapraszac moga tylko lider i zastepcy.", NamedTextColor.RED);
                case TARGET_ALREADY_MEMBER -> send(sender, "Gracz nalezy juz do panstwa.", NamedTextColor.YELLOW);
                case SELF_INVITE -> send(sender, "Nie mozesz zaprosic samego siebie.", NamedTextColor.YELLOW);
            }
        } catch (SQLException error) {
            dbError(sender, "Nation invitation failed", error);
        }
        return true;
    }

    private boolean showInvites(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) return true;
        try {
            List<NationDatabase.NationInvite> invites = plugin.getNationDatabase().findInvites(player.getUniqueId());
            if (invites.isEmpty()) {
                send(sender, "Nie masz aktywnych zaproszen.", NamedTextColor.YELLOW);
            } else {
                send(sender, "Aktywne zaproszenia (24h od wyslania):", NamedTextColor.GREEN);
                for (NationDatabase.NationInvite invite : invites) {
                    send(sender, "- " + invite.nationName() + " | /earthsmp nation accept " + invite.nationName(), NamedTextColor.YELLOW);
                }
            }
        } catch (SQLException error) {
            dbError(sender, "Nation invite lookup failed", error);
        }
        return true;
    }

    private boolean respondInvite(CommandSender sender, String name, boolean accept) {
        Player player = requirePlayer(sender);
        if (player == null) return true;
        try {
            if (accept) {
                NationDatabase.AcceptResult result = plugin.getNationDatabase().acceptInvite(player.getUniqueId(), name);
                switch (result) {
                    case JOINED -> send(sender, "Dolaczono do panstwa: " + name, NamedTextColor.GREEN);
                    case ALREADY_MEMBER -> send(sender, "Juz nalezysz do panstwa.", NamedTextColor.YELLOW);
                    case NO_ACTIVE_INVITE -> send(sender, "Nie masz aktywnego zaproszenia do tego panstwa.", NamedTextColor.YELLOW);
                }
            } else {
                boolean removed = plugin.getNationDatabase().declineInvite(player.getUniqueId(), name);
                send(sender, removed ? "Odrzucono zaproszenie: " + name : "Nie znaleziono aktywnego zaproszenia.",
                        removed ? NamedTextColor.GREEN : NamedTextColor.YELLOW);
            }
        } catch (SQLException error) {
            dbError(sender, "Nation invite response failed", error);
        }
        return true;
    }

    private boolean showMembers(CommandSender sender) {
        Player actor = requirePlayer(sender);
        if (actor == null) return true;
        try {
            Optional<NationDatabase.Membership> nation = plugin.getNationDatabase().findMembership(actor.getUniqueId());
            if (nation.isEmpty()) {
                send(sender, "Nie nalezysz do zadnego panstwa.", NamedTextColor.YELLOW);
                return true;
            }
            List<NationDatabase.NationMember> members = plugin.getNationDatabase().findMembers(nation.get().nationId());
            send(sender, "Sklad panstwa " + nation.get().nationName() + " (" + members.size() + "):", NamedTextColor.GREEN);
            for (NationDatabase.NationMember member : members) {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(member.playerUuid());
                String displayName = offline.getName() == null ? member.playerUuid().toString() : offline.getName();
                send(sender, "- " + displayName + " [" + member.role() + "]", NamedTextColor.YELLOW);
            }
        } catch (SQLException error) {
            dbError(sender, "Nation members lookup failed", error);
        }
        return true;
    }

    private boolean changeMember(CommandSender sender, String name, String actionText) {
        Player actor = requirePlayer(sender);
        if (actor == null) return true;
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            send(sender, "Gracz musi byc online. Nie znaleziono: " + name, NamedTextColor.YELLOW);
            return true;
        }
        NationDatabase.MemberAction action = switch (actionText) {
            case "promote" -> NationDatabase.MemberAction.PROMOTE;
            case "demote" -> NationDatabase.MemberAction.DEMOTE;
            default -> NationDatabase.MemberAction.KICK;
        };
        try {
            NationDatabase.ManageResult result = plugin.getNationDatabase()
                    .manageMember(actor.getUniqueId(), target.getUniqueId(), action);
            switch (result) {
                case SUCCESS -> {
                    String description = switch (action) {
                        case PROMOTE -> "awansowany na zastepce";
                        case DEMOTE -> "zdegradowany do czlonka";
                        case KICK -> "usuniety z panstwa";
                    };
                    send(sender, target.getName() + " zostal " + description + ".", NamedTextColor.GREEN);
                    send(target, "Zostales " + description + " przez " + actor.getName() + ".", NamedTextColor.YELLOW);
                }
                case NOT_AUTHORIZED -> send(sender, "Zarzadzac czlonkami moga tylko lider i zastepcy.", NamedTextColor.RED);
                case NOT_IN_SAME_NATION -> send(sender, "Gracz nie nalezy do twojego panstwa.", NamedTextColor.YELLOW);
                case CANNOT_MODIFY_LEADER -> send(sender, "Nie mozna zmienic rangi ani wyrzucic lidera.", NamedTextColor.RED);
                case CANNOT_MODIFY_SELF -> send(sender, "Nie mozesz uzyc tej komendy na sobie.", NamedTextColor.YELLOW);
                case INVALID_ROLE -> send(sender, "Gracz ma juz te range albo operacja nie ma sensu.", NamedTextColor.YELLOW);
                case DEPUTY_LIMIT -> send(sender, "Panstwo moze miec maksymalnie dwoch zastepcow.", NamedTextColor.YELLOW);
            }
        } catch (SQLException error) {
            dbError(sender, "Nation member management failed", error);
        }
        return true;
    }

    private void dbError(CommandSender sender, String message, SQLException error) {
        send(sender, "Blad bazy danych - sprawdz konsole serwera.", NamedTextColor.RED);
        plugin.getLogger().log(Level.SEVERE, message, error);
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            List<String> actions = sender.hasPermission("earthsmp.admin")
                    ? List.of("status", "security", "clock", "home", "map", "nation", "menu", "tierlist", "sidebar", "territory", "core", "war", "siege") : List.of("home", "map", "nation", "menu", "tierlist", "sidebar", "territory", "core", "war", "siege");
            return filter(actions, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("map")) return filter(List.of("code", "revoke", "reset"), args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("clock")
                && sender.hasPermission("earthsmp.admin")) return filter(List.of("start"), args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("siege")) {
            return filter(sender.hasPermission("earthsmp.admin")
                    ? List.of("menu", "check", "status", "adminstop")
                    : List.of("menu", "check", "status"), args[1]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("war") || args[0].equalsIgnoreCase("diplomacy"))) {
            List<String> actions = sender.hasPermission("earthsmp.admin")
                    ? List.of("menu", "list", "declare", "invite", "accept", "decline", "leave", "peace", "cancelpeace", "adminend")
                    : List.of("menu", "list", "declare", "invite", "accept", "decline", "leave", "peace", "cancelpeace");
            return filter(actions, args[1]);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("core")) {
            return plugin.getCoreCommand().tab(sender, args);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("territory")) {
            return plugin.getTerritoryCommand().tab(sender, args);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("nation")) {
            List<String> actions = List.of("menu", "gui", "info", "members", "invites", "invite", "accept",
                    "decline", "promote", "demote", "kick", "create", "leave", "disband", "transfer");
            return filter(actions, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("nation")) {
            String subcommand = args[1].toLowerCase(Locale.ROOT);
            if (List.of("invite", "promote", "demote", "kick").contains(subcommand)) {
                return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[2]);
            }
            if (List.of("accept", "decline").contains(subcommand) && sender instanceof Player player) {
                try {
                    return filter(plugin.getNationDatabase().findInvites(player.getUniqueId()).stream()
                            .map(NationDatabase.NationInvite::nationName).toList(), args[2]);
                } catch (SQLException error) {
                    plugin.getLogger().log(Level.WARNING, "Invite tab completion failed", error);
                }
            }
        }
        return List.of();
    }

    private static List<String> filter(List<String> values, String prefix) {
        String query = prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(query)).toList();
    }
}
