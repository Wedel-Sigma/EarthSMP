package io.github.wedelsigma.earthsmp.core;

import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
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
        if (args.length != 1 || !args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(Component.text("Uzycie: /" + label + " status", NamedTextColor.YELLOW));
            return true;
        }

        String worldName = plugin.getConfig().getString("world-name", "Earth");
        boolean worldLoaded = Bukkit.getWorld(worldName) != null;

        sender.sendMessage(Component.text("EarthSMP Core 0.1.0 - plugin dziala.", NamedTextColor.GREEN));
        sender.sendMessage(Component.text(
                "Swiat " + worldName + ": " + (worldLoaded ? "zaladowany" : "nie znaleziono"),
                worldLoaded ? NamedTextColor.GREEN : NamedTextColor.RED
        ));
        sender.sendMessage(Component.text(
                "Mechaniki panstw i wojen: jeszcze niezaimplementowane.",
                NamedTextColor.GRAY
        ));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1 && "status".startsWith(args[0].toLowerCase(Locale.ROOT))) {
            return List.of("status");
        }
        return List.of();
    }
}
