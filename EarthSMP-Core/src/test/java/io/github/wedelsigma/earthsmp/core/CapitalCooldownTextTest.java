package io.github.wedelsigma.earthsmp.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CapitalCooldownTextTest {
    @Test void showsActualTimeIncludingSeconds() {
        assertEquals("6 godz. 0 min 0 s", CapitalCooldownText.remaining(21600));
        assertEquals("5 godz. 59 min 59 s", CapitalCooldownText.remaining(21599));
        assertEquals("4 godz. 23 min 12 s", CapitalCooldownText.remaining(15792));
        assertEquals("59 min 59 s", CapitalCooldownText.remaining(3599));
        assertEquals("1 s", CapitalCooldownText.remaining(1));
        assertEquals("0 s", CapitalCooldownText.remaining(0));
        assertEquals("0 s", CapitalCooldownText.remaining(-1));
    }
}
