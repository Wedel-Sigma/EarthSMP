package io.github.wedelsigma.earthsmp.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Strict wire format used by the web editor: chunkX,chunkZ;chunkX,chunkZ.
 * Coordinates are chunks, not blocks. No SQL or world-access dependency here.
 */
final class MapChunkSelection {
    static final int LIMIT = 1024;
    private MapChunkSelection() {}

    static List<NationDatabase.TerritoryChunk> parse(String input) {
        if (input == null || input.isBlank() || input.length() > 24_000)
            throw new IllegalArgumentException("Brak wybranych chunkow.");
        String[] parts = input.split(";", -1);
        if (parts.length > LIMIT) throw new IllegalArgumentException("Limit 1024 chunkow na wniosek.");
        List<NationDatabase.TerritoryChunk> chunks = new ArrayList<>(parts.length);
        Set<NationDatabase.TerritoryChunk> seen = new HashSet<>();
        for (String part : parts) {
            // No whitespace, signs other than a single '-', float or malformed coordinates.
            if (!part.matches("-?[0-9]{1,7},-?[0-9]{1,7}"))
                throw new IllegalArgumentException("Nieprawidlowe wspolrzedne chunkow.");
            String[] pair = part.split(",", 2);
            try {
                int x = Integer.parseInt(pair[0]), z = Integer.parseInt(pair[1]);
                if (Math.abs((long)x) > 1_000_000 || Math.abs((long)z) > 1_000_000)
                    throw new IllegalArgumentException("Chunk poza dopuszczalnym zakresem.");
                NationDatabase.TerritoryChunk chunk = new NationDatabase.TerritoryChunk(x, z);
                if (!seen.add(chunk)) throw new IllegalArgumentException("Powtorzony chunk.");
                chunks.add(chunk);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Nieprawidlowa liczba chunka.");
            }
        }
        return List.copyOf(chunks);
    }
}
