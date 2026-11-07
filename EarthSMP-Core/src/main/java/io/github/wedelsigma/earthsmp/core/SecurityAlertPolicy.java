package io.github.wedelsigma.earthsmp.core;

import java.util.HashMap;
import java.util.Map;

/** Suppresses alert spam without hiding individual events from the audit log. */
final class SecurityAlertPolicy {
    record Decision(int count, boolean notifyAdmins) {}
    private static final class Window {
        long startedAt;
        long lastAlertAt = -1L;
        int count;
        Window(long now) { startedAt = now; }
    }
    private final Map<String, Window> windows = new HashMap<>();

    Decision observe(String actor, String category, long nowMillis, int threshold,
                     long windowMillis, long cooldownMillis, boolean urgent) {
        if (windows.size() > 2048) {
            windows.entrySet().removeIf(e -> nowMillis - e.getValue().startedAt > windowMillis * 2);
        }
        String key = actor + "|" + category;
        Window window = windows.get(key);
        if (window == null || nowMillis < window.startedAt ||
                nowMillis - window.startedAt >= windowMillis) {
            window = new Window(nowMillis);
            windows.put(key, window);
        }
        window.count++;
        boolean due = window.lastAlertAt < 0 ||
                nowMillis - window.lastAlertAt >= cooldownMillis;
        boolean notify = (urgent || window.count >= threshold) && due;
        if (notify) window.lastAlertAt = nowMillis;
        return new Decision(window.count, notify);
    }
}
