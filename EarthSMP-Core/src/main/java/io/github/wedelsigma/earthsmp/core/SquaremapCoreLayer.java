package io.github.wedelsigma.earthsmp.core;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HexFormat;
import org.bukkit.World;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.Registry;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.SquaremapProvider;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.api.marker.Marker;

/**
 * PUBLIC markers for every placed Earth protection core, including conquered cores.
 * Capital status, nation, owner and all player positions are deliberately omitted.
 * Each icon embeds XYZ coordinates, keeping the label visible without mouse hovering.
 */
final class SquaremapCoreLayer {
    private static final Key LAYER_KEY = Key.of("earthsmp-protection-cores");
    private final EarthSmpCorePlugin plugin;
    private SimpleLayerProvider layer;
    private MapWorld mapWorld;
    private Squaremap squaremap;
    private final Set<Key> ownedImages = new HashSet<>();

    SquaremapCoreLayer(EarthSmpCorePlugin plugin) { this.plugin = plugin; }

    boolean refresh() throws SQLException {
        World earth = plugin.getServer().getWorld(plugin.getConfig().getString("world-name", "Earth"));
        if (earth == null) return false;
        Squaremap api = SquaremapProvider.get();
        MapWorld world = api.getWorldIfEnabled(WorldIdentifier.parse(
                plugin.getConfig().getString("squaremap-world-id", "minecraft:overworld"))).orElse(null);
        if (world == null) return false;
        // The public layer is the same Overworld as the territory layer. Never export other worlds.
        List<NationDatabase.ProtectionCore> cores = plugin.getNationDatabase().allProtectionCores().stream()
                .filter(core -> earth.getName().equals(core.world())).toList();

        Map<Key, Marker> markers = new LinkedHashMap<>();
        Map<Key, BufferedImage> images = new LinkedHashMap<>();
        for (var core : cores) {
            String markerId = digest(core.id());
            Key imageKey = Key.of("earthsmp-core-xyz-" + digest(core.id() + ":" +
                    core.x() + ":" + core.y() + ":" + core.z()));
            BufferedImage img = CoreMarkerImages.create(core.x(), core.y(), core.z());
            images.put(imageKey, img);
            // The anchor is in the centre of the actual lodestone, not the label above it.
            // The squaremap API uses 2D Point(x,z) coordinates (no vertical Y position).
            Marker marker = Marker.icon(Point.of(core.x() + 0.5, core.z() + 0.5),
                    Point.of(0, -img.getHeight() / 2.0),
                    Point.of(img.getWidth() / 2.0, 24 + 15), imageKey,
                    img.getWidth(), img.getHeight());
            markers.put(Key.of("earthsmp-core-" + markerId), marker);
        }

        Registry<BufferedImage> registry = api.iconRegistry();
        // This layer owns its icon keys; never touch keys belonging to other plugins.
        Set<Key> freshlyRegistered = new HashSet<>();
        try {
            for (var image : images.entrySet()) {
                Key key = image.getKey();
                if (ownedImages.contains(key) && registry.hasEntry(key)) continue;
                if (registry.hasEntry(key)) {
                    throw new IllegalStateException("squaremap icon collision: " + key);
                }
                registry.register(key, image.getValue());
                freshlyRegistered.add(key);
            }
            if (mapWorld != world || layer == null || squaremap != api) {
                if (world.layerRegistry().hasEntry(LAYER_KEY)) {
                    throw new IllegalStateException("EarthSMP core layer already registered; restart Paper instead of /reload");
                }
                SimpleLayerProvider candidate = SimpleLayerProvider.builder("Rdzenie EarthSMP")
                        .showControls(true).defaultHidden(false).layerPriority(21).zIndex(350).build();
                world.layerRegistry().register(LAYER_KEY, candidate);
                // Only unregister the old map layer; if squaremap is the same instance,
                // image registrations shared with the new layer must survive the switch.
                if (mapWorld != null && layer != null && mapWorld.layerRegistry().hasEntry(LAYER_KEY)) {
                    mapWorld.layerRegistry().unregister(LAYER_KEY);
                }
                if (squaremap != null && squaremap != api) {
                    Registry<BufferedImage> oldRegistry = squaremap.iconRegistry();
                    for (Key oldKey : ownedImages) {
                        if (oldRegistry.hasEntry(oldKey)) oldRegistry.unregister(oldKey);
                    }
                    ownedImages.clear();
                }
                layer = candidate;
                mapWorld = world;
                squaremap = api;
            }
            layer.clearMarkers();
            markers.forEach(layer::addMarker);
            // Remove images only AFTER their markers have left the layer.
            Set<Key> obsolete = new HashSet<>(ownedImages);
            obsolete.removeAll(images.keySet());
            for (Key key : obsolete) {
                if (registry.hasEntry(key)) registry.unregister(key);
            }
            ownedImages.clear();
            ownedImages.addAll(images.keySet());
            plugin.getLogger().info("squaremap: " + markers.size() + " public Earth core icons");
            return true;
        } catch (RuntimeException e) {
            for (Key key : freshlyRegistered) {
                if (registry.hasEntry(key)) registry.unregister(key);
            }
            throw e;
        }
    }

    void shutdown() {
        cleanupPrevious();
        layer = null;
        mapWorld = null;
        squaremap = null;
    }

    private void cleanupPrevious() {
        if (mapWorld != null && layer != null && mapWorld.layerRegistry().hasEntry(LAYER_KEY)) {
            mapWorld.layerRegistry().unregister(LAYER_KEY);
        }
        if (squaremap != null) {
            Registry<BufferedImage> registry = squaremap.iconRegistry();
            for (Key key : ownedImages) {
                if (registry.hasEntry(key)) registry.unregister(key);
            }
        }
        ownedImages.clear();
    }

    private static String digest(String text) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            // 96-bit IDs. Longer than necessary for a map layer, small enough for readable keys.
            return HexFormat.of().formatHex(bytes, 0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Java SHA-256 unavailable", e);
        }
    }
}
