package io.github.wedelsigma.earthsmp.core;

/** Immutable public border snapshot, captured on the Paper main thread. */
record MapWorldBorder(double centerX, double centerZ, double size) {
    MapWorldBorder {
        if (!Double.isFinite(centerX) || !Double.isFinite(centerZ)
                || !Double.isFinite(size) || size <= 0.0) {
            throw new IllegalArgumentException("Invalid worldborder");
        }
    }

    String json() {
        return "{\"centerX\":" + centerX + ",\"centerZ\":" + centerZ
                + ",\"size\":" + size + "}";
    }
}
