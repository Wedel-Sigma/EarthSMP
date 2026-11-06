package io.github.wedelsigma.earthsmp.core;

import java.util.List;
import java.util.UUID;

/** Small JSON writer; does not need a separate JSON serialization dependency. */
final class MapPortalJson {
    private MapPortalJson() {}
    static String quote(String s) {
        if (s == null) return "null";
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 32 || c == '\u2028' || c == '\u2029') b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
                }
            }
        }
        return b.append('"').toString();
    }
    static String positions(UUID viewerId, MapPortalSnapshot snap, List<MapPortalSnapshot.Position> players) {
        var viewer = snap.viewers().get(viewerId);
        StringBuilder b = new StringBuilder("{\"viewer\":{");
        b.append("\"name\":").append(quote(viewer.name()));
        b.append(",\"nation\":").append(quote(viewer.nationName()));
        b.append("},\"players\":[");
        for (int i = 0; i < players.size(); i++) {
            var p = players.get(i);
            if (i > 0) b.append(',');
            b.append("{\"name\":").append(quote(p.name()));
            b.append(",\"uuid\":").append(quote(p.player().toString()));
            b.append(",\"x\":").append(p.x());
            b.append(",\"y\":").append(p.y());
            b.append(",\"z\":").append(p.z()).append('}');
        }
        return b.append("]}").toString();
    }
}
