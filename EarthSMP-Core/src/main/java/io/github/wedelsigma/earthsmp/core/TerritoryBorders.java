package io.github.wedelsigma.earthsmp.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Pure calculation on political chunk coordinates; never reads chunks or any private core data. */
final class TerritoryBorders {
    private TerritoryBorders() {}

    record Border(String nationId, int x1, int z1, int x2, int z2) {}
    private record Chunk(int x, int z) {}
    private record BoundaryKey(String nationId, int line) {}

    static List<Border> outlines(List<NationDatabase.TerritoryMapChunk> chunks) {
        Map<String, Set<Chunk>> byNation = new HashMap<>();
        for (var c : chunks) {
            byNation.computeIfAbsent(c.nationId(), ignored -> new HashSet<>())
                    .add(new Chunk(c.chunkX(), c.chunkZ()));
        }
        Map<BoundaryKey, TreeSet<Integer>> horizontal = new TreeMap<>((a, b) -> {
            int c = a.nationId().compareTo(b.nationId());
            return c != 0 ? c : Integer.compare(a.line(), b.line());
        });
        Map<BoundaryKey, TreeSet<Integer>> vertical = new TreeMap<>((a, b) -> {
            int c = a.nationId().compareTo(b.nationId());
            return c != 0 ? c : Integer.compare(a.line(), b.line());
        });
        for (var entry : byNation.entrySet()) {
            String id = entry.getKey();
            Set<Chunk> occupied = entry.getValue();
            for (Chunk chunk : occupied) {
                int x = chunk.x(), z = chunk.z();
                if (!occupied.contains(new Chunk(x, z - 1))) {
                    horizontal.computeIfAbsent(new BoundaryKey(id, z), ignored -> new TreeSet<>()).add(x);
                }
                if (!occupied.contains(new Chunk(x, z + 1))) {
                    horizontal.computeIfAbsent(new BoundaryKey(id, z + 1), ignored -> new TreeSet<>()).add(x);
                }
                if (!occupied.contains(new Chunk(x - 1, z))) {
                    vertical.computeIfAbsent(new BoundaryKey(id, x), ignored -> new TreeSet<>()).add(z);
                }
                if (!occupied.contains(new Chunk(x + 1, z))) {
                    vertical.computeIfAbsent(new BoundaryKey(id, x + 1), ignored -> new TreeSet<>()).add(z);
                }
            }
        }
        List<Border> borders = new ArrayList<>();
        for (var entry : horizontal.entrySet()) {
            mergeRuns(entry.getValue(), (start, endExclusive) -> borders.add(new Border(
                    entry.getKey().nationId(), start * 16, entry.getKey().line() * 16,
                    endExclusive * 16, entry.getKey().line() * 16)));
        }
        for (var entry : vertical.entrySet()) {
            mergeRuns(entry.getValue(), (start, endExclusive) -> borders.add(new Border(
                    entry.getKey().nationId(), entry.getKey().line() * 16, start * 16,
                    entry.getKey().line() * 16, endExclusive * 16)));
        }
        return borders;
    }

    private interface RunConsumer { void add(int from, int toExclusive); }

    private static void mergeRuns(TreeSet<Integer> starts, RunConsumer out) {
        Integer begin = null, last = null;
        for (int coord : starts) {
            if (begin == null) begin = coord;
            else if ((long) coord != (long) last + 1) {
                out.add(begin, last + 1);
                begin = coord;
            }
            last = coord;
        }
        if (begin != null) out.add(begin, last + 1);
    }
}
