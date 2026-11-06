package io.github.wedelsigma.earthsmp.core;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/** Per-player lightweight sidebar; it does not modify the global/main scoreboard. */
public final class EarthSmpSidebar implements Listener {
    private static final String DISABLED = "sidebar.disabled-players";
    private static final int LINES = 8;
    private final EarthSmpCorePlugin plugin;
    private final Map<UUID, Session> attached = new HashMap<>();
    private final Set<UUID> optOut = new HashSet<>();
    private final Set<UUID> yielded = new HashSet<>();

    private record Session(Scoreboard previous, Scoreboard ours, List<Team> lines, List<Component> cache) {}

    EarthSmpSidebar(EarthSmpCorePlugin plugin) {
        this.plugin = plugin;
        for (String id : plugin.getConfig().getStringList(DISABLED)) {
            try { optOut.add(UUID.fromString(id)); }
            catch (IllegalArgumentException ignored) { plugin.getLogger().warning("Invalid sidebar UUID in config: " + id); }
        }
    }

    /** Returns true when the HUD is now enabled. Saves preferences across plugin/server restarts. */
    public boolean toggle(Player player) {
        UUID id = player.getUniqueId();
        boolean enabled;
        if (optOut.add(id)) {
            enabled = false;
            detach(player);
        } else {
            optOut.remove(id);
            yielded.remove(id);
            enabled = true;
            refresh();
        }
        plugin.getConfig().set(DISABLED, optOut.stream().map(UUID::toString).sorted().toList());
        plugin.saveConfig();
        return enabled;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        yielded.remove(event.getPlayer().getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, this::refresh, 2L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Player disconnects; the scoreboard is discarded by Minecraft.
        attached.remove(event.getPlayer().getUniqueId());
        yielded.remove(event.getPlayer().getUniqueId());
    }

    public void refresh() {
        try {
            NationDatabase db = plugin.getNationDatabase();
            List<NationDatabase.NationRank> ranking = db.nationRanking();
            Map<String, NationDatabase.NationRank> nations = new HashMap<>();
            for (var country : ranking) nations.put(country.nationId(), country);
            Map<UUID, NationDatabase.Membership> membership = new HashMap<>();
            Map<String, Integer> online = new HashMap<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                Optional<NationDatabase.Membership> member = db.findMembership(player.getUniqueId());
                if (member.isPresent()) {
                    membership.put(player.getUniqueId(), member.get());
                    online.merge(member.get().nationId(), 1, Integer::sum);
                }
            }
            Map<String, Boolean> activeWars = new HashMap<>();
            Instant now = plugin.getServerClock().now();
            int day = plugin.getServerClock().day(db.editionStart(), now);
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                if (optOut.contains(uuid)) {
                    detach(player);
                    continue;
                }
                if (yielded.contains(uuid)) continue;
                Session session = attached.get(uuid);
                if (session != null && player.getScoreboard() != session.ours()) {
                    // Another plugin changed this player's scoreboard. Don't fight it.
                    attached.remove(uuid);
                    yielded.add(uuid);
                    continue;
                }
                if (session == null) {
                    if (player.getScoreboard() != Bukkit.getScoreboardManager().getMainScoreboard()) {
                        // A different plugin already owns a custom scoreboard; leave it alone.
                        yielded.add(uuid);
                        continue;
                    }
                    session = attach(player);
                }
                NationDatabase.Membership member = membership.get(uuid);
                NationDatabase.NationRank rank = member == null ? null : nations.get(member.nationId());
                boolean war = member != null && activeWars.computeIfAbsent(member.nationId(), id -> {
                    try { return db.nationAtWar(id); }
                    catch (SQLException error) { throw new SidebarSqlError(error); }
                });
                int used = member == null || day < 3 ? 0 : db.homeUses(uuid, plugin.getServerClock().serverDate(now));
                List<Component> lines = SidebarText.lines(day, member, rank,
                        member == null ? 0 : online.getOrDefault(member.nationId(), 0), war, used);
                for (int i = 0; i < LINES; i++) {
                    Component line = lines.get(i);
                    if (!line.equals(session.cache().get(i))) {
                        session.lines().get(i).prefix(line);
                        session.cache().set(i, line);
                    }
                }
            }
        } catch (SQLException | SidebarSqlError error) {
            plugin.getLogger().log(Level.WARNING, "EarthSMP sidebar refresh failed", error);
        }
    }

    private static final class SidebarSqlError extends RuntimeException {
        SidebarSqlError(SQLException error) { super(error); }
    }

    private Session attach(Player player) {
        Scoreboard previous = player.getScoreboard();
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("esmp_sidebar", Criteria.DUMMY,
                Component.text(plugin.getDisplayName(), NamedTextColor.GOLD));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        List<Team> lines = new ArrayList<>(LINES);
        for (int i = 0; i < LINES; i++) {
            String entry = "§" + Integer.toHexString(i); // visually empty, unique score entry
            Team team = board.registerNewTeam("esmp_line_" + i);
            team.addEntry(entry);
            objective.getScore(entry).setScore(LINES - i);
            lines.add(team);
        }
        List<Component> cache = new ArrayList<>(java.util.Collections.nCopies(LINES, null));
        Session session = new Session(previous, board, lines, cache);
        attached.put(player.getUniqueId(), session);
        player.setScoreboard(board);
        return session;
    }

    private void detach(Player player) {
        Session session = attached.remove(player.getUniqueId());
        if (session != null && player.getScoreboard() == session.ours()) {
            player.setScoreboard(session.previous());
        }
    }

    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) detach(player);
        attached.clear();
    }
}
