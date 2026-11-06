package io.github.wedelsigma.earthsmp.core;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Automatically started legal sieges and 9x9 conquest. All methods called on
 * the Paper primary thread. Capture progress deliberately lives ONLY in RAM:
 * restarting Paper or losing legal siege eligibility resets it to zero.
 */
public final class SiegeManager {
    public enum Result {
        STARTED, NO_CORE, NO_NATION, OWN_CORE, NO_OPPOSING_WAR,
        OUTSIDE_HOURS, TOO_FEW_DEFENDERS, ALREADY_ACTIVE, ERROR
    }

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final EarthSmpCorePlugin plugin;
    private final Map<String, Session> active = new LinkedHashMap<>();
    private final Set<String> adminStopped = new HashSet<>();

    private static final class Session {
        final String coreId;
        final String defenderId;
        final String defenderName;
        final String attackerId;
        final long warId;
        final Instant startedAt;
        Instant missingSince;
        Instant lastProgressTick;
        String firstClaimantNationId;
        int progressSeconds;
        final BossBar bar = BossBar.bossBar(Component.text("Oblężenie rdzenia"), 0.0f,
                BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
        final Set<UUID> viewers = new HashSet<>();

        Session(NationDatabase.ProtectionCore core, String attackerId, long warId, Instant time) {
            this.coreId = core.id();
            this.defenderId = core.nationId();
            this.defenderName = core.nationName();
            this.attackerId = attackerId;
            this.warId = warId;
            this.startedAt = time;
            this.lastProgressTick = time;
        }
    }

    private record ZonePresence(boolean attackers, boolean defenders, String firstAttackerId) {}
    private enum BarStatus { CAPTURING, CONTESTED, RECOVERING, WAITING }

    public SiegeManager(EarthSmpCorePlugin plugin) { this.plugin = plugin; }
    public int activeCount() { return active.size(); }

    private void say(Player player, String message, NamedTextColor color) {
        player.sendMessage(Component.text("[" + plugin.getDisplayName() + "] " + message, color));
    }

    private int defenderOnlineCount(String nationId) throws SQLException {
        Set<UUID> roster = new HashSet<>();
        for (var member : plugin.getNationDatabase().findMembers(nationId)) roster.add(member.playerUuid());
        int count = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (roster.contains(online.getUniqueId())) count++;
        }
        return count;
    }

    private Long findOpposingWar(String attackerId, String defenderId) throws SQLException {
        for (var war : plugin.getWars().activeWars()) {
            var attacker = plugin.getWars().participant(war.id(), attackerId);
            var defender = plugin.getWars().participant(war.id(), defenderId);
            if (attacker.isPresent() && defender.isPresent() && attacker.get().side() != defender.get().side()) {
                return war.id();
            }
        }
        return null;
    }

    private boolean stillOpposed(Session session) throws SQLException {
        var war = plugin.getWars().find(session.warId);
        if (war.isEmpty() || !war.get().active()) return false;
        var attacker = plugin.getWars().participant(session.warId, session.attackerId);
        var defender = plugin.getWars().participant(session.warId, session.defenderId);
        return attacker.isPresent() && defender.isPresent() && attacker.get().side() != defender.get().side();
    }

    private void alert(Session s, String message) {
        try {
            var participants = plugin.getWars().participants(s.warId);
            Set<String> nationIds = new HashSet<>();
            for (var part : participants) nationIds.add(part.nationId());
            for (Player online : Bukkit.getOnlinePlayers()) {
                var membership = plugin.getNationDatabase().findMembership(online.getUniqueId());
                if (membership.isPresent() && nationIds.contains(membership.get().nationId())) {
                    say(online, message, NamedTextColor.GOLD);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Siege announcement failed", e);
        }
        plugin.getLogger().info(message);
    }

    /** Automatically invoked from tick when an eligible enemy enters a protected base. */
    public Result begin(Player player) {
        var core = plugin.getCoreProtection().findAt(player.getLocation());
        if (core.isEmpty()) return Result.NO_CORE;
        if (active.containsKey(core.get().id()) || adminStopped.contains(core.get().id())) return Result.ALREADY_ACTIVE;
        try {
            var own = plugin.getNationDatabase().findMembership(player.getUniqueId());
            if (own.isEmpty()) return Result.NO_NATION;
            if (own.get().nationId().equals(core.get().nationId())) return Result.OWN_CORE;
            Long warId = findOpposingWar(own.get().nationId(), core.get().nationId());
            if (warId == null) return Result.NO_OPPOSING_WAR;
            Instant now = Instant.now();
            if (!SiegeRules.openAt(now)) return Result.OUTSIDE_HOURS;
            int roster = plugin.getNationDatabase().findMembers(core.get().nationId()).size();
            int online = defenderOnlineCount(core.get().nationId());
            if (!SiegeRules.mayBegin(now, roster, online)) return Result.TOO_FEW_DEFENDERS;
            Session s = new Session(core.get(), own.get().nationId(), warId, now);
            active.put(s.coreId, s);
            alert(s, "Rozpoczeto legalny szturm przeciwko " + s.defenderName + " w wojnie #" + s.warId + ".");
            return Result.STARTED;
        } catch (SQLException error) {
            plugin.getLogger().log(Level.SEVERE, "Failed to evaluate siege start", error);
            return Result.ERROR;
        }
    }

    public void sendStartResult(Player p, Result result) {
        String message = switch (result) {
            case STARTED -> "Szturm aktywny. Przejmowanie 9x9 uruchamia sie automatycznie.";
            case NO_CORE -> "Wejdz do chronionej bazy wrogiego rdzenia.";
            case NO_NATION -> "Musisz nalezec do panstwa uczestniczacego w wojnie.";
            case OWN_CORE -> "Nie mozesz oblegac wlasnej bazy.";
            case NO_OPPOSING_WAR -> "Brak aktywnej wojny przeciw wlascicielowi tej bazy.";
            case OUTSIDE_HOURS -> "Szturmy tylko 10:00–00:00 Europe/Warsaw.";
            case TOO_FEW_DEFENDERS -> "Za malo obroncow online (min. 20% skladu, co najmniej 1).";
            case ALREADY_ACTIVE -> "Ten rdzen jest juz oblegany lub szturm przerwano administracyjnie.";
            case ERROR -> "Blad SQLite — sprawdz konsole serwera.";
        };
        say(p, message, result == Result.STARTED ? NamedTextColor.GREEN : NamedTextColor.YELLOW);
    }

    public List<String> describe(Player p) {
        Instant now = Instant.now();
        List<String> lines = new ArrayList<>();
        ZonedDateTime local = now.atZone(SiegeRules.WARSAW);
        lines.add("Czas Polski: " + local.format(CLOCK) + " | Okno: " + (SiegeRules.openAt(now) ? "OTWARTE" : "ZAMKNIETE"));
        var core = plugin.getCoreProtection().findAt(p.getLocation());
        if (core.isEmpty()) {
            lines.add("Nie jestes na chronionym obszarze zadnego rdzenia.");
            return lines;
        }
        try {
            var c = core.get();
            int roster = plugin.getNationDatabase().findMembers(c.nationId()).size();
            int online = defenderOnlineCount(c.nationId());
            lines.add("Broni: " + c.nationName() + " | Obecnosc: " + online + "/" + roster
                    + ", wymagane: " + SiegeRules.requiredDefenders(roster));
            Session session = active.get(c.id());
            if (session == null) {
                lines.add("Stan: brak aktywnego szturmu tego rdzenia.");
                var own = plugin.getNationDatabase().findMembership(p.getUniqueId());
                if (own.isPresent() && !own.get().nationId().equals(c.nationId())) {
                    lines.add(findOpposingWar(own.get().nationId(), c.nationId()) == null
                            ? "Nie jestes strona wojny przeciw obroncom."
                            : "Szturm rozpocznie sie automatycznie po spelnieniu warunkow.");
                }
            } else {
                lines.add("Stan: AKTYWNY | Wojna #" + session.warId);
                lines.add("Przejmowanie: " + session.progressSeconds + "/480 s");
                if (session.missingSince != null) {
                    long remaining = Math.max(0, Duration.between(now,
                            session.missingSince.plus(SiegeRules.GRACE_PERIOD)).toSeconds());
                    lines.add("Obroncow za malo! Tolerancja: " + remaining + " sekund.");
                } else lines.add("Prog obroncow spelniony.");
            }
        } catch (SQLException error) {
            plugin.getLogger().log(Level.SEVERE, "Failed to describe siege", error);
            lines.add("Blad odczytu danych.");
        }
        return lines;
    }

    public void status(Player player) {
        for (String line : describe(player)) say(player, line, NamedTextColor.AQUA);
    }

    private static boolean inside(Player player, NationDatabase.ProtectionCore core) {
        var pos = player.getLocation();
        return pos.getWorld() != null && core.world().equals(pos.getWorld().getName())
                && core.contains(pos.getBlockX(), pos.getBlockZ());
    }

    private static boolean inCaptureZone(Player player, NationDatabase.ProtectionCore core) {
        var pos = player.getLocation();
        return pos.getWorld() != null && core.world().equals(pos.getWorld().getName())
                && CoreCaptureZone.inside(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(),
                        core.x(), core.y(), core.z());
    }

    /** One membership lookup per online player and one participant snapshot per siege. */
    private ZonePresence capturePresence(Session s, NationDatabase.ProtectionCore core,
                                         Map<String, WarRepository.Side> sides) throws SQLException {
        WarRepository.Side defenderSide = sides.get(s.defenderId);
        boolean attackers = false, defenders = false;
        String firstAttackerId = null;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!inCaptureZone(player, core)) continue;
            var member = plugin.getNationDatabase().findMembership(player.getUniqueId());
            if (member.isEmpty()) continue;
            WarRepository.Side side = sides.get(member.get().nationId());
            if (side == null) continue;
            if (side == defenderSide) {
                defenders = true;
            } else {
                attackers = true;
                if (firstAttackerId == null) firstAttackerId = member.get().nationId();
            }
        }
        return new ZonePresence(attackers, defenders, firstAttackerId);
    }

    private Map<String, WarRepository.Side> participantSides(Session s) throws SQLException {
        Map<String, WarRepository.Side> sides = new HashMap<>();
        for (var p : plugin.getWars().participants(s.warId)) sides.put(p.nationId(), p.side());
        return sides;
    }

    private boolean anyActiveAttackerInside(Session s, NationDatabase.ProtectionCore core,
                                            Map<String, WarRepository.Side> sides) throws SQLException {
        var defenderSide = sides.get(s.defenderId);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!inside(player, core)) continue;
            var membership = plugin.getNationDatabase().findMembership(player.getUniqueId());
            if (membership.isPresent()) {
                var side = sides.get(membership.get().nationId());
                if (side != null && side != defenderSide) return true;
            }
        }
        return false;
    }

    private void updateBossbar(Session s, NationDatabase.ProtectionCore core,
                               Map<String, WarRepository.Side> sides, BarStatus state) throws SQLException {
        BossBar.Color color = switch (state) {
            case CAPTURING -> BossBar.Color.GREEN;
            case CONTESTED -> BossBar.Color.YELLOW;
            case RECOVERING -> BossBar.Color.RED;
            case WAITING -> BossBar.Color.BLUE;
        };
        String description = switch (state) {
            case CAPTURING -> "Przejmowanie";
            case CONTESTED -> "Kontestowane";
            case RECOVERING -> "Odzyskiwanie";
            case WAITING -> "Oczekiwanie";
        };
        s.bar.name(Component.text(description + " | " + s.progressSeconds + "/480 s", NamedTextColor.WHITE));
        s.bar.progress(s.progressSeconds / (float) CaptureRules.REQUIRED_SECONDS);
        s.bar.color(color);
        var defenderSide = sides.get(s.defenderId);
        Set<UUID> visible = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!inside(player, core)) continue;
            var membership = plugin.getNationDatabase().findMembership(player.getUniqueId());
            if (membership.isEmpty() || !sides.containsKey(membership.get().nationId())) continue;
            // Only active belligerents physically inside the base see progress.
            visible.add(player.getUniqueId());
            if (s.viewers.add(player.getUniqueId())) player.showBossBar(s.bar);
        }
        for (UUID viewer : Set.copyOf(s.viewers)) {
            if (visible.contains(viewer)) continue;
            Player player = Bukkit.getPlayer(viewer);
            if (player != null) player.hideBossBar(s.bar);
            s.viewers.remove(viewer);
        }
    }

    private void hideBossbar(Session s) {
        for (UUID id : s.viewers) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) player.hideBossBar(s.bar);
        }
        s.viewers.clear();
    }

    /** Returns true only after the SQLite transaction succeeded. */
    private boolean finishCapture(Session s, NationDatabase.ProtectionCore core) throws SQLException {
        if (s.firstClaimantNationId == null || s.progressSeconds != CaptureRules.REQUIRED_SECONDS) return false;
        // In particular, an ally that withdrew from the war may not collect a core.
        var sides = participantSides(s);
        if (!sides.containsKey(s.firstClaimantNationId) ||
                sides.get(s.firstClaimantNationId) == sides.get(s.defenderId)) {
            s.firstClaimantNationId = null;
            s.progressSeconds = 0;
            return false;
        }
        String victor = plugin.getWars().participants(s.warId).stream()
                .filter(p -> p.nationId().equals(s.firstClaimantNationId))
                .map(WarRepository.Participant::nationName).findFirst().orElse("Zdobywcy");
        NationDatabase.CaptureOutcome outcome = plugin.getNationDatabase().captureCore(
                core.id(), s.defenderId, s.firstClaimantNationId);
        if (outcome == NationDatabase.CaptureOutcome.INVALID_VICTOR ||
                outcome == NationDatabase.CaptureOutcome.NOT_FOUND) {
            throw new SQLException("Core conquest rejected: " + outcome);
        }
        if (outcome == NationDatabase.CaptureOutcome.LAST_CORE_DESTROYED) {
            World world = Bukkit.getWorld(core.world());
            if (world != null) {
                // A conquered capital of the final defender is destroyed, not transferred.
                world.getBlockAt(core.x(), core.y(), core.z()).setType(Material.AIR, false);
            } else {
                plugin.getLogger().severe("Fallen nation has an orphan anchor: unloaded world " + core.world());
            }
        }
        plugin.getCoreProtection().reload();
        plugin.refreshCoreMap();
        if (outcome == NationDatabase.CaptureOutcome.LAST_CORE_DESTROYED) {
            plugin.refreshTerritoryMap();
        }
        String message = outcome == NationDatabase.CaptureOutcome.CONQUERED
                ? "Panstwo " + victor + " zdobylo rdzen panstwa " + s.defenderName + "!"
                : "Panstwo " + s.defenderName + " upadlo po utracie ostatniego rdzenia!";
        Bukkit.broadcast(Component.text("[" + plugin.getDisplayName() + "] " + message, NamedTextColor.GOLD));
        plugin.getLogger().info(message);
        return true;
    }

    /** Scheduler tick (20 ticks) keeps legality, capture, and displayed bar synchronized. */
    public void tick() {
        Instant now = Instant.now();
        var it = active.values().iterator();
        while (it.hasNext()) {
            Session s = it.next();
            String ended = null;
            boolean captured = false;
            try {
                if (!SiegeRules.openAt(now)) ended = "Polnoc w Polsce — koniec legalnych godzin.";
                else if (!stillOpposed(s)) ended = "Wojna zakonczona lub uczestnicy sie wycofali.";
                else {
                    NationDatabase.ProtectionCore core = plugin.getCoreProtection().all().stream()
                            .filter(c -> c.id().equals(s.coreId) && c.nationId().equals(s.defenderId))
                            .findFirst().orElse(null);
                    if (core == null) ended = "Rdzen usuniety lub zmienil wlasciciela.";
                    else {
                        int roster = plugin.getNationDatabase().findMembers(s.defenderId).size();
                        int online = defenderOnlineCount(s.defenderId);
                        if (online >= SiegeRules.requiredDefenders(roster)) {
                            if (s.missingSince != null) {
                                s.missingSince = null;
                                alert(s, "Obroncy wrocili — tolerancja 30 minut wyzerowana.");
                            }
                        } else if (s.missingSince == null) {
                            s.missingSince = now;
                            alert(s, "Obroncow za malo — 30 minut na dokonczenie.");
                        } else if (SiegeRules.graceExpired(s.missingSince, now)) {
                            ended = "Minelo 30 minut bez wymaganej liczby obroncow.";
                        }
                        if (ended == null) {
                            Map<String, WarRepository.Side> sides = participantSides(s);
                            if (s.firstClaimantNationId != null &&
                                    (!sides.containsKey(s.firstClaimantNationId) ||
                                     sides.get(s.firstClaimantNationId) == sides.get(s.defenderId))) {
                                s.firstClaimantNationId = null;
                                s.progressSeconds = 0;
                            }
                            ZonePresence presence = capturePresence(s, core, sides);
                            // First attacker to ACTUALLY start positive progress owns the claim.
                            if (s.progressSeconds == 0 && presence.attackers() && !presence.defenders()) {
                                s.firstClaimantNationId = presence.firstAttackerId();
                            }
                            long elapsed = Math.max(0L, Duration.between(s.lastProgressTick, now).getSeconds());
                            if (elapsed > 0) s.lastProgressTick = s.lastProgressTick.plusSeconds(elapsed);
                            s.progressSeconds = CaptureRules.advance(s.progressSeconds, elapsed,
                                    presence.attackers(), presence.defenders());
                            if (s.progressSeconds == 0) s.firstClaimantNationId = null;
                            BarStatus barStatus = presence.attackers() && !presence.defenders()
                                    ? BarStatus.CAPTURING
                                    : presence.attackers() ? BarStatus.CONTESTED
                                    : presence.defenders() && s.progressSeconds > 0 ? BarStatus.RECOVERING
                                    : BarStatus.WAITING;
                            updateBossbar(s, core, sides, barStatus);
                            if (s.progressSeconds == CaptureRules.REQUIRED_SECONDS) {
                                captured = finishCapture(s, core);
                            } else if (s.progressSeconds == 0 &&
                                    !anyActiveAttackerInside(s, core, sides)) {
                                ended = "Atakujacy opuscili baze, postep wyzerowany.";
                            }
                        }
                    }
                }
            } catch (SQLException | RuntimeException error) {
                plugin.getLogger().log(Level.SEVERE, "Siege tick failed, ending session (fail closed)", error);
                ended = "Blad kontroli oblężenia — przerwano dla bezpieczenstwa.";
            }
            if (captured || ended != null) {
                hideBossbar(s);
                it.remove();
                if (!captured) alert(s, "Szturm przeciwko " + s.defenderName + " zakonczony: " + ended);
            }
        }

        adminStopped.removeIf(coreId -> {
            var core = plugin.getCoreProtection().all().stream().filter(c -> c.id().equals(coreId)).findFirst();
            if (core.isEmpty()) return true;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!inside(player, core.get())) continue;
                try {
                    var nation = plugin.getNationDatabase().findMembership(player.getUniqueId());
                    if (nation.isPresent() && !nation.get().nationId().equals(core.get().nationId())) return false;
                } catch (SQLException error) {
                    plugin.getLogger().log(Level.WARNING, "Cannot clear admin stop; keeping blocked", error);
                    return false;
                }
            }
            return true;
        });

        // No commands or GUI interactions are needed to initiate legal assaults.
        if (!SiegeRules.openAt(now)) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            var core = plugin.getCoreProtection().findAt(player.getLocation());
            if (core.isEmpty() || active.containsKey(core.get().id()) || adminStopped.contains(core.get().id())) continue;
            begin(player);
        }
    }

    /** Only a legal attacker can use the siege pickaxe inside the 65x65 protection. */
    public boolean isLegalAttacker(Player player, NationDatabase.ProtectionCore core) throws SQLException {
        Session s = active.get(core.id());
        if (s == null || !s.defenderId.equals(core.nationId()) ||
                !SiegeRules.openAt(Instant.now()) || !stillOpposed(s)) return false;
        if (s.missingSince != null && SiegeRules.graceExpired(s.missingSince, Instant.now())) return false;
        var member = plugin.getNationDatabase().findMembership(player.getUniqueId());
        if (member.isEmpty() || member.get().nationId().equals(core.nationId())) return false;
        var attacker = plugin.getWars().participant(s.warId, member.get().nationId());
        var defender = plugin.getWars().participant(s.warId, s.defenderId);
        return attacker.isPresent() && defender.isPresent() && attacker.get().side() != defender.get().side();
    }

    public boolean hostilePresent(NationDatabase.ProtectionCore core) throws SQLException {
        if (!active.containsKey(core.id())) return false;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (inside(player, core) && isLegalAttacker(player, core)) return true;
        }
        return false;
    }

    public boolean adminStopHere(Player player) {
        var core = plugin.getCoreProtection().findAt(player.getLocation());
        if (core.isEmpty()) return false;
        Session s = active.remove(core.get().id());
        if (s == null) return false;
        hideBossbar(s);
        adminStopped.add(s.coreId);
        alert(s, "Administrator przerwal szturm przeciwko " + s.defenderName + ".");
        return true;
    }

    public void shutdown() {
        if (!active.isEmpty()) plugin.getLogger().info("Stopping " + active.size() + " siege(s) on shutdown");
        for (Session s : active.values()) hideBossbar(s);
        active.clear();
        adminStopped.clear();
    }
}
