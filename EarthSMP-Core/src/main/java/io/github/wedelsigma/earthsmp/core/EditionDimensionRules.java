package io.github.wedelsigma.earthsmp.core;

/** Pure progression rules. Nether threshold is configurable until day-10 wording is ratified. */
public final class EditionDimensionRules {
    private EditionDimensionRules() {}

    public static boolean allowed(String environment, int editionDay, int netherUnlockDay) {
        return switch (environment) {
            case "THE_END" -> false;
            case "NETHER" -> editionDay > 0 && editionDay >= Math.max(1, netherUnlockDay);
            default -> true;
        };
    }
}
