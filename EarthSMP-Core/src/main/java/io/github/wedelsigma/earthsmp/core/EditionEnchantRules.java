package io.github.wedelsigma.earthsmp.core;

import java.util.Map;

/** Pure rule layer used for anvil, trade, enchant table and existing-item validation. */
public final class EditionEnchantRules {
    private EditionEnchantRules() {}

    private static final Map<String, Integer> QUALITY_OF_LIFE = Map.of(
            "unbreaking", 3,
            "efficiency", 5,
            "looting", 3,
            "silk_touch", 1,
            "aqua_affinity", 1,
            "respiration", 3);

    /** Day 0 (before edition start) uses the strictest rules. */
    public static boolean permitted(String key, int level, int day) {
        return level > 0 && normalizedLevel(key, level, day) == level;
    }

    public static int maximum(String key, int day) {
        if ("mending".equals(key)) return 0;
        if ("fortune".equals(key)) {
            if (day < 7) return 0;
            return day >= 16 ? 3 : 1;
        }
        Integer qol = QUALITY_OF_LIFE.get(key);
        if (qol != null) return qol;
        // Unknown and combat/mobility enchants are conservatively subject to the PvP cap.
        // The QoL exception list is deliberately explicit, not guessed from item category.
        return day >= 10 ? 2 : 1;
    }

    /** Fortune II is never legal, even on day 16+. */
    public static int normalizedLevel(String key, int level, int day) {
        if (level <= 0) return 0;
        if ("fortune".equals(key)) {
            if (day < 7) return 0;
            if (day < 16) return 1;
            return level >= 3 ? 3 : 1;
        }
        return Math.min(level, maximum(key, day));
    }

    public static boolean needsChange(String key, int level, int day) {
        return level != normalizedLevel(key, level, day);
    }
}
