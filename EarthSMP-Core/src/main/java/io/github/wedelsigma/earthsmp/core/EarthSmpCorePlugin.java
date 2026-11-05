package io.github.wedelsigma.earthsmp.core;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class EarthSmpCorePlugin extends JavaPlugin {

    private NationDatabase nationDatabase;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            nationDatabase = new NationDatabase(getDataFolder().toPath().resolve("earthsmp.db"));
        } catch (IOException | SQLException error) {
            getLogger().log(Level.SEVERE, "Cannot initialize SQLite database; disabling EarthSMP Core", error);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        PluginCommand earthSmpCommand = Objects.requireNonNull(
                getCommand("earthsmp"),
                "The 'earthsmp' command must be declared in plugin.yml"
        );
        EarthSmpCommand executor = new EarthSmpCommand(this);
        earthSmpCommand.setExecutor(executor);
        earthSmpCommand.setTabCompleter(executor);

        getLogger().info("EarthSMP Core 0.3.0 enabled - SQLite nation operations ready");
    }

    @Override
    public void onDisable() {
        if (nationDatabase != null) {
            try {
                nationDatabase.close();
            } catch (SQLException error) {
                getLogger().log(Level.SEVERE, "Could not close SQLite database", error);
            } finally {
                nationDatabase = null;
            }
        }
        getLogger().info("EarthSMP Core disabled");
    }

    public NationDatabase getNationDatabase() {
        return Objects.requireNonNull(nationDatabase, "SQLite not initialized");
    }
}
