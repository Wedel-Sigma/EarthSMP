package io.github.wedelsigma.earthsmp.core;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import org.bukkit.World;
import org.bukkit.Bukkit;
import xyz.jpenilla.squaremap.api.SquaremapProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class EarthSmpCorePlugin extends JavaPlugin {

    private NationDatabase nationDatabase;
    private MapPortalAuth mapPortalAuth;
    private MapPortalServer mapPortalServer;
    private ServerClock serverClock;
    private HomeCommand homeCommand;
    private EarthSmpSidebar sidebar;
    private NationTierlistMenu tierlist;
    private NationMenu nationMenu;
    private EarthSmpHubMenu hubMenu;
    private TerritoryCommand territoryCommand;
    private SquaremapTerritoryLayer squaremapTerritoryLayer;
    private SquaremapCoreLayer squaremapCoreLayer;
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
    private PvpProtectionListener pvpProtection;
    private WarRespawnListener warRespawn;
    private CombatTagListener combatTag;
    private RespawnBedProtection respawnBeds;
    private EditionItemLimitListener editionItemLimits;
    private EditionEnchantListener editionEnchants;
    private EditionDimensionsListener editionDimensions;
    private SecurityAlerts securityAlerts;

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

        securityAlerts = new SecurityAlerts(this);
        serverClock = new ServerClock();
        homeCommand = new HomeCommand(this);
        PluginCommand home = Objects.requireNonNull(getCommand("home"), "Missing home command in plugin.yml");
        home.setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) homeCommand.execute(player);
            else sender.sendMessage(Component.text("/home wymaga gracza w grze.", NamedTextColor.YELLOW));
            return true;
        });
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
        tierlist = new NationTierlistMenu(this);
        sidebar = new EarthSmpSidebar(this);
        getServer().getPluginManager().registerEvents(tierlist, this);
        getServer().getPluginManager().registerEvents(sidebar, this);
        getServer().getScheduler().runTaskTimer(this, sidebar::refresh, 20L, 40L);
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

        // Peace protection and death-time war cooldown (single-threaded Paper events).
        try {
            pvpProtection = new PvpProtectionListener(this);
            getServer().getPluginManager().registerEvents(pvpProtection, this);
            warRespawn = new WarRespawnListener(this);
            getServer().getPluginManager().registerEvents(warRespawn, this);
            getServer().getScheduler().runTaskTimer(this, warRespawn::tick, 20L, 20L);
            respawnBeds = new RespawnBedProtection(this);
            getServer().getPluginManager().registerEvents(respawnBeds, this);
            combatTag = new CombatTagListener(this);
            getServer().getPluginManager().registerEvents(combatTag, this);
            getServer().getScheduler().runTaskTimer(this, combatTag::tick, 20L, 20L);
        } catch (IOException error) {
            getLogger().log(Level.SEVERE, "Cannot load persistent war respawn locks; disabling plugin", error);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        // Edition 1.8.8: aggregate inventory limits, enchant progression and dimension gates.
        editionItemLimits = new EditionItemLimitListener(this);
        editionEnchants = new EditionEnchantListener(this);
        editionDimensions = new EditionDimensionsListener(this);
        getServer().getPluginManager().registerEvents(editionItemLimits, this);
        getServer().getPluginManager().registerEvents(editionEnchants, this);
        getServer().getPluginManager().registerEvents(editionDimensions, this);
        // Regular safety pass closes /give and third-party plugin inventory bypasses.
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                editionItemLimits.audit(player);
                editionEnchants.audit(player);
            }
        }, 40L, 40L);

        // Optional squaremap dependency: keep the core usable even when squaremap is absent.
        var mapPlugin = getServer().getPluginManager().getPlugin("squaremap");
        if (mapPlugin != null && mapPlugin.isEnabled()) {
            squaremapTerritoryLayer = new SquaremapTerritoryLayer(this);
            squaremapCoreLayer = new SquaremapCoreLayer(this);
            getServer().getScheduler().runTaskLater(this, () -> {
                if (!refreshTerritoryMap()) {
                    getLogger().warning("Political map unavailable; retry /esmp territory maprefresh later");
                }
                refreshCoreMap();
            }, 40L);
            // Recovery polling: helps if squaremap's web world finishes loading later.
            // This also self-heals public markers after exceptional DB/admin changes.
            getServer().getScheduler().runTaskTimer(this, this::refreshCoreMap, 20L * 60L, 20L * 60L);
        } else {
            getLogger().warning("squaremap not enabled: territory claims still work, but no public overlay");
        }
        if (getConfig().getBoolean("map-portal.enabled", true)) {
            try {
                mapPortalAuth = new MapPortalAuth(getDataFolder().toPath().resolve("earthsmp.db"));
            } catch (SQLException error) {
                getLogger().log(Level.SEVERE, "Cannot open persistent map accounts", error);
                mapPortalAuth = null;
            }
            if (mapPortalAuth != null) {
                try {
                String bind = getConfig().getString("map-portal.bind-address", "127.0.0.1");
                int port = getConfig().getInt("map-portal.port", 8766);
                String origin = getConfig().getString("map-portal.public-origin", "http://127.0.0.1:8766");
                String upstream = getConfig().getString("map-portal.squaremap-internal-url", "http://127.0.0.1:8080");
                String trustedProxyIp = getConfig().getString("map-portal.trusted-proxy-address", "");
                mapPortalServer = new MapPortalServer(bind, port, origin, upstream, mapPortalAuth,
                        getLogger(), this, trustedProxyIp);
                refreshMapPortalSnapshot();
                getServer().getScheduler().runTaskTimer(this, this::refreshMapPortalSnapshot, 20L, 20L);
            } catch (IOException | RuntimeException error) {
                getLogger().log(Level.SEVERE, "Private map portal disabled: check map-portal config and port", error);
                mapPortalServer = null;
                try { mapPortalAuth.close(); }
                catch (SQLException closeError) { getLogger().log(Level.WARNING, "Could not close map account database", closeError); }
                mapPortalAuth = null;
            }
            }
        }
        getLogger().info("EarthSMP Core 1.8.10 enabled - map portal security and WorldBorder overlay");
    }

    @Override
    public void onDisable() {
        editionDimensions = null;
        securityAlerts = null;
        editionEnchants = null;
        editionItemLimits = null;
        if (combatTag != null) { combatTag.shutdown(); combatTag = null; }
        if (warRespawn != null) { warRespawn.shutdown(); warRespawn = null; }
        if (pvpProtection != null) { pvpProtection.shutdown(); pvpProtection = null; }
        if (sidebar != null) { sidebar.shutdown(); sidebar = null; }
        tierlist = null;
        if (mapPortalServer != null) {
            mapPortalServer.close();
            mapPortalServer = null;
        }
        if (mapPortalAuth != null) {
            try { mapPortalAuth.close(); }
            catch (SQLException error) { getLogger().log(Level.WARNING, "Could not close map account database", error); }
            mapPortalAuth = null;
        }
        if (sieges != null) sieges.shutdown();
        if (squaremapCoreLayer != null) {
            squaremapCoreLayer.shutdown();
            squaremapCoreLayer = null;
        }
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
        homeCommand = null;
        serverClock = null;
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

    /** Refresh public XYZ lodestone markers after every committed core operation. */
    public boolean refreshCoreMap() {
        if (squaremapCoreLayer == null) return false;
        try {
            return squaremapCoreLayer.refresh();
        } catch (SQLException | RuntimeException error) {
            getLogger().log(Level.WARNING, "Could not refresh public squaremap core icons", error);
            return false;
        }
    }

    /** Update on the Bukkit main thread; HTTP handlers only read immutable published snapshots. */
    private void refreshMapPortalSnapshot() {
        if (mapPortalServer == null || nationDatabase == null) return;
        Map<UUID, MapPortalSnapshot.Viewer> viewers = new HashMap<>();
        var visible = new ArrayList<MapPortalSnapshot.Position>();
        String earthName = getConfig().getString("world-name", "Earth");
        try {
            // Defense in depth: squaremap's *public* tracker must not reveal any players.
            // The private map portal uses Bukkit snapshots and does not depend on that tracker.
            if (getServer().getPluginManager().isPluginEnabled("squaremap")) {
                var playerManager = SquaremapProvider.get().playerManager();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!playerManager.hidden(player.getUniqueId())) playerManager.hide(player.getUniqueId(), false);
                }
            }
            // A registered browser user need not be online in Minecraft.
            // Rebuild permissions from CURRENT SQLite membership each tick (never cache old nation access).
            if (mapPortalAuth != null) {
                for (var entry : mapPortalAuth.linkedAccounts().entrySet()) {
                    var membership = nationDatabase.findMembership(entry.getKey());
                    viewers.put(entry.getKey(), new MapPortalSnapshot.Viewer(entry.getValue(),
                            membership.map(NationDatabase.Membership::nationId).orElse(null),
                            membership.map(NationDatabase.Membership::nationName).orElse(null)));
                }
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID id = player.getUniqueId();
                var membership = nationDatabase.findMembership(id);
                String nationId = membership.map(NationDatabase.Membership::nationId).orElse(null);
                String nationName = membership.map(NationDatabase.Membership::nationName).orElse(null);
                viewers.put(id, new MapPortalSnapshot.Viewer(player.getName(), nationId, nationName));
                World world = player.getWorld();
                if (world.getEnvironment() != World.Environment.NORMAL || !world.getName().equals(earthName)) continue;
                var loc = player.getLocation();
                visible.add(new MapPortalSnapshot.Position(id, player.getName(), nationId,
                        loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
            }
            // WorldBorder is public and never contains player positions. Read on Paper thread only.
            World earth = Bukkit.getWorld(earthName);
            if (earth == null) mapPortalServer.updateWorldBorder(null);
            else {
                var worldBorder = earth.getWorldBorder();
                var center = worldBorder.getCenter();
                mapPortalServer.updateWorldBorder(new MapWorldBorder(
                        center.getX(), center.getZ(), worldBorder.getSize()));
            }
            mapPortalServer.update(new MapPortalSnapshot(viewers, visible));
        } catch (SQLException | RuntimeException error) {
            // Fail closed: stale membership must never allow reading enemy positions.
            mapPortalServer.update(MapPortalSnapshot.empty());
            mapPortalServer.updateWorldBorder(null);
            getLogger().log(Level.WARNING, "Private map snapshot failed; positions temporarily hidden", error);
        }
    }

    /** Public-facing brand. No internal identifiers or commands depend on this value. */
    public String getDisplayName() {
        String value = getConfig().getString("server.display-name", "Kabina Narodów 2.0");
        if (value == null || value.isBlank()) return "Kabina Narodów 2.0";
        // Keep scoreboard titles and inventory windows readable; ignore control characters.
        value = value.strip().replaceAll("[\\p{Cntrl}]", "");
        return value.length() > 40 ? value.substring(0, 40) : value;
    }

    public SecurityAlerts getSecurityAlerts() { return securityAlerts; }

    public MapPortalAuth getMapPortalAuth() { return mapPortalAuth; }
    public String getMapPortalUrl() { return mapPortalServer == null ? null : mapPortalServer.url(); }

    public ServerClock getServerClock() {
        return Objects.requireNonNull(serverClock, "Edition clock not initialized");
    }

    public EarthSmpSidebar getSidebar() {
        return Objects.requireNonNull(sidebar, "Sidebar not initialized");
    }

    public NationTierlistMenu getTierlist() {
        return Objects.requireNonNull(tierlist, "Tierlist not initialized");
    }

    public HomeCommand getHomeCommand() {
        return Objects.requireNonNull(homeCommand, "Home teleport not initialized");
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

    public WarRespawnListener getWarRespawn() { return Objects.requireNonNull(warRespawn, "War respawn not initialized"); }

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
