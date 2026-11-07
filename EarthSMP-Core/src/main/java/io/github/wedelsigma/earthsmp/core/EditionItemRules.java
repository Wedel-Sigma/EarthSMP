package io.github.wedelsigma.earthsmp.core;

import java.util.Map;

/** Pure policy: entire carried inventory (including offhand and armour), not per stack. */
public final class EditionItemRules {
    private EditionItemRules() {}

    private static final Map<String, Integer> LIMITS = Map.of(
            "GOLDEN_APPLE", 8,
            "ENDER_PEARL", 3,
            "COBWEB", 16,
            "TOTEM_OF_UNDYING", 3,
            "ENCHANTED_GOLDEN_APPLE", 0,
            "END_CRYSTAL", 0,
            "RESPAWN_ANCHOR", 0);

    /** Returns -1 for unrestricted materials. */
    public static int cap(String material) {
        return LIMITS.getOrDefault(material, -1);
    }

    public static boolean forbidden(String material) {
        return cap(material) == 0;
    }

    public static int space(String material, int currentTotal) {
        int limit = cap(material);
        return limit < 0 ? Integer.MAX_VALUE : Math.max(0, limit - Math.max(0, currentTotal));
    }

    public static int transferable(String material, int currentTotal, int offeredAmount) {
        return Math.min(Math.max(0, offeredAmount), space(material, currentTotal));
    }
}
