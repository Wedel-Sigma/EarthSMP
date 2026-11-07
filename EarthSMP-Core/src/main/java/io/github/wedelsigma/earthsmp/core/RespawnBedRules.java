package io.github.wedelsigma.earthsmp.core;

/** Pure block-coordinate rule for both bed halves and their supporting blocks. */
public final class RespawnBedRules {
    private RespawnBedRules() {}

    public static boolean protects(int x, int y, int z,
                                   int headX, int headY, int headZ,
                                   int footX, int footY, int footZ) {
        return matches(x, y, z, headX, headY, headZ)
                || matches(x, y, z, footX, footY, footZ)
                || matches(x, y + 1, z, headX, headY, headZ)
                || matches(x, y + 1, z, footX, footY, footZ);
    }

    private static boolean matches(int x, int y, int z, int bx, int by, int bz) {
        return x == bx && y == by && z == bz;
    }
}
