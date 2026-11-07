package io.github.wedelsigma.earthsmp.core;

/** Seconds of war respawn lockdown, fixed at death and not recalculated while waiting. */
public final class WarRespawnRules {
    private WarRespawnRules() { }

    public static int secondsFor(int onlineNationMembers) {
        if (onlineNationMembers <= 0) return 15;
        if (onlineNationMembers <= 2) return 15;
        if (onlineNationMembers <= 5) return 20;
        if (onlineNationMembers <= 10) return 30;
        if (onlineNationMembers <= 15) return 40;
        return 50;
    }
}
