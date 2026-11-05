package io.github.wedelsigma.earthsmp.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Optional CLI fallback: all player actions are available in /esmp > Oblezenia. */
public final class SiegeCommand {
    private final EarthSmpCorePlugin plugin;
    public SiegeCommand(EarthSmpCorePlugin plugin) {this.plugin=plugin;}
    public boolean handle(CommandSender sender,String[] args) {
        if(!(sender instanceof Player p)) {
            sender.sendMessage(Component.text("Menu oblezen wymaga gracza.",NamedTextColor.YELLOW));
            return true;
        }
        String action=args.length>=2?args[1].toLowerCase(java.util.Locale.ROOT):"menu";
        switch(action) {
            case "menu" -> plugin.getSiegeMenu().open(p);
            case "check", "status" -> plugin.getSieges().status(p);
            case "start" -> plugin.getSieges().sendStartResult(p,plugin.getSieges().begin(p));
            case "adminstop" -> {
                if(!p.hasPermission("earthsmp.admin")) {
                    p.sendMessage(Component.text("Tylko administrator moze zatrzymac oblezenie.",NamedTextColor.RED));
                } else if(!plugin.getSieges().adminStopHere(p)) {
                    p.sendMessage(Component.text("Nie ma aktywnego oblezenia w tym regionie.",NamedTextColor.YELLOW));
                }
            }
            default -> p.sendMessage(Component.text("/esmp siege menu | check | start",NamedTextColor.YELLOW));
        }
        return true;
    }
}
