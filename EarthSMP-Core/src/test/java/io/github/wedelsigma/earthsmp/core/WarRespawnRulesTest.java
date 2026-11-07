package io.github.wedelsigma.earthsmp.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WarRespawnRulesTest {
    @Test void exactOnlineThresholds() {
        int[][] cases = {{0,15},{1,15},{2,15},{3,20},{5,20},{6,30},{10,30},
                {11,40},{15,40},{16,50},{30,50}};
        for (int[] pair : cases)
            assertEquals(pair[1], WarRespawnRules.secondsFor(pair[0]), "online=" + pair[0]);
    }
}
