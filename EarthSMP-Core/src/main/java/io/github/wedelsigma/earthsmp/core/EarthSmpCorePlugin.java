package io.github.wedelsigma.earthsmp.core;

import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class EarthSmpCorePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        PluginCommand earthSmpCommand = Objects.requireNonNull(
                getCommand("earthsmp"),
                "The 'earthsmp' command must be declared in plugin.yml"
        );
        earthSmpCommand.setExecutor(new EarthSmpCommand(this));

        getLogger().info("EarthSMP Core enabled (skeleton 0.1.0)");
    }

    @Override
    public void onDisable() {
        getLogger().info("EarthSMP Core disabled");
    }
}
