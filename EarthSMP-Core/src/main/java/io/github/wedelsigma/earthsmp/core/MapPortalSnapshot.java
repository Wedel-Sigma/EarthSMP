package io.github.wedelsigma.earthsmp.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Immutable snapshot produced exclusively on Paper's main thread, read on HTTP threads. */
record MapPortalSnapshot(Map<UUID, Viewer> viewers, List<Position> overworld) {
    record Viewer(String name, String nationId, String nationName) {}
    record Position(UUID player, String name, String nationId, int x, int y, int z) {}

    MapPortalSnapshot {
        viewers = Map.copyOf(viewers);
        overworld = List.copyOf(overworld);
    }
    static MapPortalSnapshot empty() { return new MapPortalSnapshot(Map.of(), List.of()); }

    /** Linked accounts may access the map while offline; membership is evaluated in each snapshot. */
    Optional<List<Position>> visibleTo(UUID viewerId) {
        Viewer viewer = viewers.get(viewerId);
        if (viewer == null) return Optional.empty();
        List<Position> result = new ArrayList<>();
        for (Position p : overworld) {
            if (p.player().equals(viewerId) || (viewer.nationId() != null
                    && viewer.nationId().equals(p.nationId()))) result.add(p);
        }
        return Optional.of(List.copyOf(result));
    }
}
