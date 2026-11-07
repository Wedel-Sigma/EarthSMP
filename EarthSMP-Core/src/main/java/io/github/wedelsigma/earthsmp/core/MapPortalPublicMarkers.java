package io.github.wedelsigma.earthsmp.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/** Squaremap's markers.json may contain other plugins' layers. Never proxy those layers publicly. */
final class MapPortalPublicMarkers {
    private static final Set<String> PUBLIC_LAYERS = Set.of(
            "earthsmp-political-territories", "earthsmp-protection-cores");
    private MapPortalPublicMarkers() {}

    static byte[] onlyFirstPartyPublicLayers(byte[] input) {
        JsonElement root = JsonParser.parseString(new String(input, StandardCharsets.UTF_8));
        if (!root.isJsonArray()) throw new IllegalArgumentException("Unexpected squaremap markers format");
        JsonArray allowed = new JsonArray();
        for (JsonElement value : root.getAsJsonArray()) {
            if (!value.isJsonObject()) continue;
            JsonElement id = value.getAsJsonObject().get("id");
            if (id != null && id.isJsonPrimitive() && id.getAsJsonPrimitive().isString()
                    && PUBLIC_LAYERS.contains(id.getAsString())) {
                allowed.add(value);
            }
        }
        return allowed.toString().getBytes(StandardCharsets.UTF_8);
    }
}
