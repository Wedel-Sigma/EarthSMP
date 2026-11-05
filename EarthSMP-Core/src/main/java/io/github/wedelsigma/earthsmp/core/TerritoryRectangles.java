package io.github.wedelsigma.earthsmp.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** Compress adjacent claimed chunks into non-overlapping, maximal vertical rectangles.
 * Avoid one marker per chunk on the squaremap web client. Does not read or load Minecraft chunks.
 */
final class TerritoryRectangles {
    private TerritoryRectangles() {}

    record Rect(String nationId, String nationName, int minX, int minZ, int maxX, int maxZ) {}
    private record Run(int minX, int maxX) {}
    private static final class MutableRect {
        final String id, name;
        final int minX, maxX, minZ;
        int maxZ;
        MutableRect(String id, String name, Run run, int z) {
            this.id = id; this.name = name; this.minX = run.minX(); this.maxX = run.maxX();
            this.minZ = z; this.maxZ = z;
        }
        Rect finish() { return new Rect(id, name, minX, minZ, maxX, maxZ); }
    }

    static List<Rect> compress(List<NationDatabase.TerritoryMapChunk> chunks) {
        Map<String, TreeMap<Integer, TreeSet<Integer>>> byNation = new TreeMap<>();
        Map<String, String> names = new HashMap<>();
        for (var chunk : chunks) {
            names.put(chunk.nationId(), chunk.nationName());
            byNation.computeIfAbsent(chunk.nationId(), unused -> new TreeMap<>())
                    .computeIfAbsent(chunk.chunkZ(), unused -> new TreeSet<>()).add(chunk.chunkX());
        }
        List<Rect> result = new ArrayList<>();
        for (var nation : byNation.entrySet()) {
            Map<Run, MutableRect> active = new HashMap<>();
            int lastZ = Integer.MIN_VALUE;
            for (var row : nation.getValue().entrySet()) {
                int z = row.getKey();
                if ((long) z - lastZ != 1L) {
                    for (var rect : active.values()) result.add(rect.finish());
                    active.clear();
                }
                Map<Run, MutableRect> next = new HashMap<>();
                Integer start = null, end = null;
                for (int x : row.getValue()) {
                    if (start == null) { start = x; end = x; }
                    else if ((long) x == (long) end + 1L) { end = x; }
                    else {
                        continueRun(nation.getKey(), names.get(nation.getKey()),
                                new Run(start, end), z, active, next);
                        start = x; end = x;
                    }
                }
                if (start != null) continueRun(nation.getKey(), names.get(nation.getKey()),
                        new Run(start, end), z, active, next);
                for (var entry : active.entrySet()) {
                    if (!next.containsKey(entry.getKey())) result.add(entry.getValue().finish());
                }
                active = next;
                lastZ = z;
            }
            for (var rect : active.values()) result.add(rect.finish());
        }
        result.sort(Comparator.comparing(Rect::nationId)
                .thenComparingInt(Rect::minZ).thenComparingInt(Rect::minX));
        return result;
    }

    private static void continueRun(String id, String name, Run run, int z,
                                    Map<Run, MutableRect> active, Map<Run, MutableRect> next) {
        MutableRect rect = active.get(run);
        if (rect == null) rect = new MutableRect(id, name, run, z);
        else rect.maxZ = z;
        next.put(run, rect);
    }
}
