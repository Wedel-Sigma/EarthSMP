package io.github.wedelsigma.earthsmp.core;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Independent audit trail for EarthSMP rule enforcement, not a cheat verdict.
 * Runs on Paper's main thread. Vendor anticheat/anti-dupe alerts remain separate.
 */
public final class SecurityAlerts {
    private final EarthSmpCorePlugin plugin;
    private final SecurityAlertPolicy policy = new SecurityAlertPolicy();
    private final ArrayDeque<String> recent = new ArrayDeque<>();
    private final Path logDirectory;
    private long lastIoError;

    SecurityAlerts(EarthSmpCorePlugin plugin) {
        this.plugin = plugin;
        this.logDirectory = plugin.getDataFolder().toPath().resolve("security-events");
    }

    public void record(Player player, String category, String detail, boolean enforced, boolean urgent) {
        if (!plugin.getConfig().getBoolean("security.enabled", true)) return;
        // Only call from the primary server thread; event context and online admins are Bukkit data.
        if (!Bukkit.isPrimaryThread()) {
            plugin.getLogger().warning("Ignored off-thread security event; caller must schedule on main thread");
            return;
        }
        long now = System.currentTimeMillis();
        String kind = safe(category, 48);
        String context = safe(detail, 160);
        Location location = player.getLocation();
        JsonObject event = new JsonObject();
        event.addProperty("timeUtc", Instant.ofEpochMilli(now).toString());
        event.addProperty("playerUuid", player.getUniqueId().toString());
        event.addProperty("player", safe(player.getName(), 32));
        event.addProperty("type", kind);
        event.addProperty("context", context);
        event.addProperty("enforced", enforced);
        event.addProperty("world", safe(location.getWorld().getName(), 70));
        event.addProperty("x", location.getBlockX());
        event.addProperty("y", location.getBlockY());
        event.addProperty("z", location.getBlockZ());
        append(event.toString(), now);

        String summary = event.get("timeUtc").getAsString() + " " + player.getName() +
                " [" + kind + "] " + context + (enforced ? " (zablokowano/ograniczono)" : " (obserwacja)");
        recent.addLast(summary);
        while (recent.size() > 30) recent.removeFirst();

        int threshold = Math.max(1, Math.min(30, plugin.getConfig().getInt("security.alert-threshold", 4)));
        long cooldown = Math.max(5, plugin.getConfig().getInt("security.alert-cooldown-seconds", 60)) * 1000L;
        var decision = policy.observe(player.getUniqueId().toString(), kind, now,
                threshold, 60_000L, cooldown, urgent);
        if (!decision.notifyAdmins()) return;

        String notice = "[EarthSMP Security] " + player.getName() + " (" +
                player.getUniqueId() + ") " + kind + " x" + decision.count() +
                " | " + context + " | " + location.getWorld().getName() + " " +
                location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() +
                " | " + (enforced ? "egzekwowano blokade" : "obserwacja");
        plugin.getLogger().warning(notice);
        for (Player admin : Bukkit.getOnlinePlayers()) {
            if (admin.hasPermission("earthsmp.security.alerts")) {
                admin.sendMessage(Component.text(notice, NamedTextColor.GOLD));
            }
        }
        if (plugin.getConfig().getBoolean("security.warn-player-on-repeat", true) &&
                decision.count() >= threshold && !player.hasPermission("earthsmp.admin")) {
            player.sendMessage(Component.text(
                    "Wiele operacji narusza ograniczenia serwera. Sprawdz zasady lub skontaktuj sie z administracja.",
                    NamedTextColor.YELLOW));
        }
    }

    /** Up to 30 most recent entries since plugin start; all entries persist as daily JSONL files. */
    public List<String> recent(int max) {
        List<String> result = new ArrayList<>();
        Iterator<String> it = recent.descendingIterator();
        while (it.hasNext() && result.size() < Math.max(0, Math.min(30, max))) result.add(it.next());
        return List.copyOf(result);
    }

    private void append(String line, long now) {
        try {
            Files.createDirectories(logDirectory);
            Path target = logDirectory.resolve(LocalDate.now(ZoneOffset.UTC) + ".jsonl");
            Files.writeString(target, line + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException error) {
            if (now - lastIoError > 60_000L) {
                plugin.getLogger().warning("Could not persist security event: " + error.getMessage());
                lastIoError = now;
            }
        }
    }

    private static String safe(String value, int max) {
        if (value == null) return "";
        String clean = value.replaceAll("[\\p{Cntrl}]", " ").strip();
        return clean.length() > max ? clean.substring(0, max) : clean;
    }
}
