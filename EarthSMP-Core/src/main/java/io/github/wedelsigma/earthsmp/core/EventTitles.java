package io.github.wedelsigma.earthsmp.core;

import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Short, low-noise event titles. Chat remains the persistent event log.
 * Never overwrite /home or wartime respawn countdowns, or interrupt tagged PvP.
 */
public final class EventTitles {
    private static final long MIN_INTERVAL_NANOS = Duration.ofSeconds(6).toNanos();
    private static final Title.Times DISPLAY = Title.Times.times(
            Duration.ofMillis(100), Duration.ofMillis(2000), Duration.ofMillis(350));

    private final EarthSmpCorePlugin plugin;
    private final Map<UUID, Long> lastTitleAt = new HashMap<>();

    EventTitles(EarthSmpCorePlugin plugin) { this.plugin = plugin; }

    public void show(Player player, String heading, String detail, NamedTextColor color) {
        if (player == null || !player.isOnline() || player.isDead() || plugin.majorTitleBlocked(player)) return;
        long now = System.nanoTime();
        Long last = lastTitleAt.get(player.getUniqueId());
        if (last != null && now - last >= 0 && now - last < MIN_INTERVAL_NANOS) return;
        lastTitleAt.put(player.getUniqueId(), now);
        player.showTitle(Title.title(
                Component.text(shorten(heading, 40), color),
                Component.text(shorten(detail, 65), NamedTextColor.YELLOW), DISPLAY));
    }

    public void everyone(String heading, String detail, NamedTextColor color) {
        for (Player player : Bukkit.getOnlinePlayers()) show(player, heading, detail, color);
    }

    /** Notifications only to nations participating in the selected war. */
    public void warParticipants(long warId, String heading, String detail, NamedTextColor color) {
        try {
            Set<String> nations = new HashSet<>();
            for (var entry : plugin.getWars().participants(warId)) nations.add(entry.nationId());
            for (Player player : Bukkit.getOnlinePlayers()) {
                var membership = plugin.getNationDatabase().findMembership(player.getUniqueId());
                if (membership.isPresent() && nations.contains(membership.get().nationId())) {
                    show(player, heading, detail, color);
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not show event titles to war participants", exception);
        }
    }

    private static String shorten(String input, int max) {
        if (input == null) return "";
        return input.length() <= max ? input : input.substring(0, max - 1) + "…";
    }
}
