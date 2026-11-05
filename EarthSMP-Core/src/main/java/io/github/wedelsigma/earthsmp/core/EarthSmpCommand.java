package io.github.wedelsigma.earthsmp.core;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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
        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            return showStatus(sender);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("nation")) {
            if (args.length == 2 && args[1].equalsIgnoreCase("info")) {
                return showOwnNation(sender);
            }
            if (args.length >= 3 && args[1].equalsIgnoreCase("create")) {
                return createNation(sender, String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
            }
        }
        sender.sendMessage(Component.text("Uzycie: /" + label + " status", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("        /" + label + " nation create <nazwa>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("        /" + label + " nation info", NamedTextColor.YELLOW));
        return true;
    }

    private boolean showStatus(CommandSender sender) {
        String worldName = plugin.getConfig().getString("world-name", "Earth");
        boolean worldLoaded = Bukkit.getWorld(worldName) != null;
        sender.sendMessage(Component.text("EarthSMP Core 0.3.0 - plugin dziala.", NamedTextColor.GREEN));
        sender.sendMessage(Component.text(
                "Swiat " + worldName + ": " + (worldLoaded ? "zaladowany" : "nie znaleziono"),
                worldLoaded ? NamedTextColor.GREEN : NamedTextColor.RED
        ));
        try {
            sender.sendMessage(Component.text(
                    "SQLite: aktywne | Panstwa: " + plugin.getNationDatabase().countNations(),
                    NamedTextColor.GREEN));
        } catch (SQLException error) {
            dbError(sender, "SQLite status query failed", error);
        }
        sender.sendMessage(Component.text("GUI, rdzenie i terytoria: jeszcze niezaimplementowane.", NamedTextColor.GRAY));
        return true;
    }

    private boolean showOwnNation(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Ta komenda wymaga gracza w grze.", NamedTextColor.YELLOW));
            return true;
        }
        try {
            Optional<NationDatabase.Membership> membership = plugin.getNationDatabase()
                    .findMembership(player.getUniqueId());
            if (membership.isEmpty()) {
                sender.sendMessage(Component.text("Nie nalezysz do zadnego panstwa.", NamedTextColor.YELLOW));
            } else {
                NationDatabase.Membership member = membership.get();
                sender.sendMessage(Component.text(
                        "Panstwo: " + member.nationName() + " | Ranga: " + member.role(),
                        NamedTextColor.GREEN));
                sender.sendMessage(Component.text("ID: " + member.nationId(), NamedTextColor.GRAY));
            }
        } catch (SQLException error) {
            dbError(sender, "Nation membership lookup failed", error);
        }
        return true;
    }

    private boolean createNation(CommandSender sender, String rawName) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Na razie tworzenie panstwa jest dostepne tylko dla gracza.", NamedTextColor.YELLOW));
            return true;
        }
        String name = rawName.trim();
        if (!isValidName(name)) {
            sender.sendMessage(Component.text(
                    "Nazwa: 3-32 znaki (litery, cyfry, spacje, _ lub -).", NamedTextColor.RED));
            return true;
        }
        try {
            if (plugin.getNationDatabase().findMembership(player.getUniqueId()).isPresent()) {
                sender.sendMessage(Component.text("Juz nalezysz do panstwa.", NamedTextColor.YELLOW));
                return true;
            }
            if (!plugin.getNationDatabase().createNation(name, player.getUniqueId())) {
                sender.sendMessage(Component.text("Ta nazwa jest juz zajeta albo masz juz panstwo.", NamedTextColor.YELLOW));
                return true;
            }
            sender.sendMessage(Component.text(
                    "Utworzono panstwo: " + name + ". Jestes jego liderem!", NamedTextColor.GREEN));
            sender.sendMessage(Component.text(
                    "To test zapisu danych. Darmowy rdzen dodamy w nastepnym etapie.", NamedTextColor.GRAY));
        } catch (SQLException error) {
            dbError(sender, "Nation creation failed", error);
        }
        return true;
    }

    private static boolean isValidName(String name) {
        if (name.length() < 3 || name.length() > 32) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (!(Character.isLetterOrDigit(ch) || ch == ' ' || ch == '_' || ch == '-')) {
                return false;
            }
        }
        return true;
    }

    private void dbError(CommandSender sender, String logMessage, SQLException error) {
        sender.sendMessage(Component.text("Blad bazy danych - sprawdz konsole serwera.", NamedTextColor.RED));
        plugin.getLogger().log(Level.SEVERE, logMessage, error);
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            return List.of("status", "nation").stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("nation")) {
            return List.of("create", "info").stream()
                    .filter(option -> option.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}
