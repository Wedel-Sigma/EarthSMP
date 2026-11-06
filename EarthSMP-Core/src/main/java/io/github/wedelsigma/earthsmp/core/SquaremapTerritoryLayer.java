package io.github.wedelsigma.earthsmp.core;

import java.awt.Color;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import org.bukkit.World;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.SquaremapProvider;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;

/** Public political map only: NO member positions, private claims/cores, or other dimensions. */
final class SquaremapTerritoryLayer {
    private static final Key LAYER_KEY = Key.of("earthsmp-political-territories");
    private final EarthSmpCorePlugin plugin;
    private SimpleLayerProvider layer;
    private MapWorld mapWorld;

    SquaremapTerritoryLayer(EarthSmpCorePlugin plugin) { this.plugin = plugin; }

    /** @return false if squaremap or its Overworld is not ready yet. */
    boolean refresh() throws SQLException {
        World earth = plugin.getServer().getWorld(plugin.getConfig().getString("world-name", "Earth"));
        if (earth == null) return false;
        Squaremap squaremap = SquaremapProvider.get();
        WorldIdentifier identifier = WorldIdentifier.parse(
                plugin.getConfig().getString("squaremap-world-id", "minecraft:overworld"));
        MapWorld map = squaremap.getWorldIfEnabled(identifier).orElse(null);
        if (map == null) return false;

        // Fetch/prepare all data before replacing the current layer, so a DB failure leaves it unchanged.
        var chunks = plugin.getNationDatabase().territoryMapChunks(earth.getName());
        var rectangles = TerritoryRectangles.compress(chunks);
        Map<Key, xyz.jpenilla.squaremap.api.marker.Marker> markers = new java.util.LinkedHashMap<>();
        int count = 0;
        for (var rect : rectangles) {
            // End coordinate is exclusive: full outer edge of the last claimed chunk.
            Point a = Point.of(rect.minX() * 16, rect.minZ() * 16);
            Point b = Point.of((rect.maxX() + 1) * 16, (rect.maxZ() + 1) * 16);
            Color color = colorFor(rect.nationId());
            String nation = escapeHtml(rect.nationName());
            // No outlines on fill rectangles: they may be an arbitrary decomposition of one country.
            Marker marker = Marker.rectangle(a, b).markerOptions(MarkerOptions.builder()
                    .stroke(false).fill(true).fillColor(color).fillOpacity(0.24)
                    .hoverTooltip(nation).clickTooltip(nation));
            markers.put(Key.of("nation-fill-" + (++count)), marker);
        }
        // Draw ONLY externally exposed edges of actual claimed chunks. Neighbouring chunks of
        // the same nation share no border; adjacent segments are combined into longer lines.
        int lines = 0;
        for (var edge : TerritoryBorders.outlines(chunks)) {
            Color color = colorFor(edge.nationId());
            Marker line = Marker.polyline(Point.of(edge.x1(), edge.z1()), Point.of(edge.x2(), edge.z2()))
                    .markerOptions(MarkerOptions.builder()
                            .stroke(true).strokeColor(color).strokeOpacity(0.85).strokeWeight(2)
                            .fill(false));
            markers.put(Key.of("nation-border-" + (++lines)), line);
        }

        if (this.mapWorld != map || this.layer == null) {
            if (this.mapWorld != null && this.layer != null
                    && this.mapWorld.layerRegistry().hasEntry(LAYER_KEY)) {
                this.mapWorld.layerRegistry().unregister(LAYER_KEY);
            }
            // If another instance owns the same key (e.g. after /reload), don't unregister unknown layers.
            if (map.layerRegistry().hasEntry(LAYER_KEY)) {
                plugin.getLogger().warning("squaremap layer key already registered; restart Paper instead of /reload");
                return false;
            }
            SimpleLayerProvider newLayer = SimpleLayerProvider.builder("Panstwa " + plugin.getDisplayName())
                    .showControls(true).defaultHidden(false).layerPriority(20).zIndex(200).build();
            map.layerRegistry().register(LAYER_KEY, newLayer);
            this.mapWorld = map;
            this.layer = newLayer;
        }
        layer.clearMarkers();
        markers.forEach(layer::addMarker);
        plugin.getLogger().info("squaremap: " + chunks.size() + " political chunks -> "
                + rectangles.size() + " fill rectangles, " + lines + " exterior border segments");
        return true;
    }

    void shutdown() {
        if (mapWorld != null && layer != null && mapWorld.layerRegistry().hasEntry(LAYER_KEY)) {
            mapWorld.layerRegistry().unregister(LAYER_KEY);
        }
        mapWorld = null;
        layer = null;
    }

    private static Color colorFor(String nationId) {
        // Stable even if the name is changed; IDs are persisted in SQLite.
        int hash = nationId.hashCode();
        float hue = (float) (Integer.toUnsignedLong(hash) % 360L) / 360f;
        return Color.getHSBColor(hue, 0.74f, 0.93f);
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
