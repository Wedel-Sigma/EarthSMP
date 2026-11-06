package io.github.wedelsigma.earthsmp.core;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class EarthSmpCorePlugin extends JavaPlugin {

    private NationDatabase nationDatabase;
    private NationMenu nationMenu;
    private EarthSmpHubMenu hubMenu;
    private TerritoryCommand territoryCommand;
    private SquaremapTerritoryLayer squaremapTerritoryLayer;
    private CoreProtection coreProtection;
    private CoreCommand coreCommand;
    private CoreItems coreItems;
    private CoreCaptureZone coreCaptureZone;
    private WarRepository wars;
    private WarCommand warCommand;
    private WarMenu warMenu;
    private SiegeManager sieges;
    private SiegeMenu siegeMenu;
    private SiegeCommand siegeCommand;
    private SiegeStorage siegeStorage;
    private SiegePickaxe siegePickaxe;
    private BlockOriginListener blockOriginListener;

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
        territoryCommand = new TerritoryCommand(this);
        wars = new WarRepository(nationDatabase);
        warCommand = new WarCommand(this);
        warMenu = new WarMenu(this);
        sieges = new SiegeManager(this);
        siegeMenu = new SiegeMenu(this);
        siegeCommand = new SiegeCommand(this);
        siegeStorage = new SiegeStorage(nationDatabase);
        nationMenu = new NationMenu(this);
        hubMenu = new EarthSmpHubMenu(this);
        coreCommand = new CoreCommand(this);
        try {
            coreProtection = new CoreProtection(this);
            getServer().getPluginManager().registerEvents(coreProtection, this);
        } catch (SQLException error) {
            getLogger().log(Level.SEVERE, "Cannot load core protection; disabling plugin", error);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getPluginManager().registerEvents(nationMenu, this);
        getServer().getPluginManager().registerEvents(hubMenu, this);
        getServer().getPluginManager().registerEvents(warMenu, this);
        getServer().getPluginManager().registerEvents(siegeMenu, this);
        getServer().getScheduler().runTaskTimer(this, sieges::tick, 20L, 20L);
        coreItems = new CoreItems(this);
        getServer().getPluginManager().registerEvents(coreItems, this);
        coreCaptureZone = new CoreCaptureZone(this);
        getServer().getPluginManager().registerEvents(coreCaptureZone, this);
        siegePickaxe = new SiegePickaxe(this);
        getServer().getPluginManager().registerEvents(siegePickaxe, this);
        blockOriginListener = new BlockOriginListener(this);
        // MONITOR events record only accepted placements/breaks after protection plugins.
        getServer().getPluginManager().registerEvents(blockOriginListener, this);

        // Optional squaremap dependency: keep the core usable even when squaremap is absent.
        var mapPlugin = getServer().getPluginManager().getPlugin("squaremap");
        if (mapPlugin != null && mapPlugin.isEnabled()) {
            squaremapTerritoryLayer = new SquaremapTerritoryLayer(this);
            getServer().getScheduler().runTaskLater(this, () -> {
                if (!refreshTerritoryMap()) {
                    getLogger().warning("Political map unavailable; retry /esmp territory maprefresh later");
                }
            }, 40L);
        } else {
            getLogger().warning("squaremap not enabled: territory claims still work, but no public overlay");
        }
        getLogger().info("EarthSMP Core 1.8.1 enabled - legal sieges, pickaxes, persistent block provenance");
    }

    @Override
    public void onDisable() {
        if (sieges != null) sieges.shutdown();
        if (squaremapTerritoryLayer != null) {
            squaremapTerritoryLayer.shutdown();
            squaremapTerritoryLayer = null;
        }
        if (nationDatabase != null) {
            try {
                nationDatabase.close();
            } catch (SQLException error) {
                getLogger().log(Level.SEVERE, "Could not close SQLite database", error);
            } finally {
                nationDatabase = null;
            }
        }
        coreProtection = null;
        coreCommand = null;
        coreItems = null;
        coreCaptureZone = null;
        siegePickaxe = null;
        blockOriginListener = null;
        siegeStorage = null;
        warMenu = null;
        siegeCommand = null;
        siegeMenu = null;
        sieges = null;
        warCommand = null;
        wars = null;
        nationMenu = null;
        hubMenu = null;
        territoryCommand = null;
        getLogger().info("EarthSMP Core disabled");
    }

    /** Refreshes the public map from committed SQLite claims. Can safely fail if squaremap is unavailable. */
    public boolean refreshTerritoryMap() {
        if (squaremapTerritoryLayer == null) return false;
        try {
            return squaremapTerritoryLayer.refresh();
        } catch (SQLException | RuntimeException error) {
            getLogger().log(Level.WARNING, "Could not refresh political squaremap layer", error);
            return false;
        }
    }

    public SiegeStorage getSiegeStorage() {
        return Objects.requireNonNull(siegeStorage, "Siege SQLite storage not initialized");
    }

    public SiegePickaxe getSiegePickaxe() {
        return Objects.requireNonNull(siegePickaxe, "Siege pickaxe not initialized");
    }

    public CoreProtection getCoreProtection() {
        return Objects.requireNonNull(coreProtection, "Core protection not initialized");
    }

    public CoreItems getCoreItems() {
        return Objects.requireNonNull(coreItems, "Core item manager not initialized");
    }

    public CoreCommand getCoreCommand() {
        return Objects.requireNonNull(coreCommand, "Core commands not initialized");
    }

    public TerritoryCommand getTerritoryCommand() {
        return Objects.requireNonNull(territoryCommand, "Territory commands not initialized");
    }

    public EarthSmpHubMenu getHubMenu() {
        return Objects.requireNonNull(hubMenu, "Central menu not initialized");
    }

    public NationMenu getNationMenu() {
        return Objects.requireNonNull(nationMenu, "Nation menu not initialized");
    }

    public WarRepository getWars() { return Objects.requireNonNull(wars, "Wars not initialized"); }

    public WarCommand getWarCommand() { return Objects.requireNonNull(warCommand, "War commands not initialized"); }

    public WarMenu getWarMenu() { return Objects.requireNonNull(warMenu, "War GUI not initialized"); }

    public SiegeManager getSieges() { return Objects.requireNonNull(sieges, "Sieges not initialized"); }

    public SiegeMenu getSiegeMenu() { return Objects.requireNonNull(siegeMenu, "Siege menu not initialized"); }

    public SiegeCommand getSiegeCommand() { return Objects.requireNonNull(siegeCommand, "Siege commands not initialized"); }

    public NationDatabase getNationDatabase() {
        return Objects.requireNonNull(nationDatabase, "SQLite not initialized");
    }
}
