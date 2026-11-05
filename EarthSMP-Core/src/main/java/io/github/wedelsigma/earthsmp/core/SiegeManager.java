package io.github.wedelsigma.earthsmp.core;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
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

/**
 * Active siege eligibility and grace periods. No destruction, looting or capture in 1.6.0.
 * Lives only in memory: restart always ends all sieges (future capture progress resets too).
 * All calls and the monitor must run on the Paper main server thread.
 */
public final class SiegeManager {
    public enum Result {
        STARTED, NO_CORE, NO_NATION, OWN_CORE, NO_OPPOSING_WAR,
        OUTSIDE_HOURS, TOO_FEW_DEFENDERS, ALREADY_ACTIVE, ERROR
    }
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final EarthSmpCorePlugin plugin;
    private final Map<String, Session> active = new LinkedHashMap<>();

    private static final class Session {
        final String coreId;
        final String defenderId;
        final String defenderName;
        final String attackerId;
        final long warId;
        final Instant startedAt;
        Instant missingSince;

        Session(NationDatabase.ProtectionCore core, String attackerId, long warId, Instant startedAt) {
            this.coreId=core.id();this.defenderId=core.nationId();this.defenderName=core.nationName();
            this.attackerId=attackerId;this.warId=warId;this.startedAt=startedAt;
        }
    }

    public SiegeManager(EarthSmpCorePlugin plugin) { this.plugin=plugin; }
    public int activeCount() { return active.size(); }

    private static void say(Player p,String message,NamedTextColor color) {
        p.sendMessage(Component.text("[EarthSMP] "+message,color));
    }

    private int defenderOnlineCount(String nationId) throws SQLException {
        Set<UUID> roster = new HashSet<>();
        for (var member : plugin.getNationDatabase().findMembers(nationId)) roster.add(member.playerUuid());
        int count=0;
        for (Player p : Bukkit.getOnlinePlayers()) if (roster.contains(p.getUniqueId())) count++;
        return count;
    }

    private Long findOpposingWar(String attackerId,String defenderId) throws SQLException {
        for (var war : plugin.getWars().activeWars()) {
            var attacker = plugin.getWars().participant(war.id(),attackerId);
            var defender = plugin.getWars().participant(war.id(),defenderId);
            if (attacker.isPresent() && defender.isPresent() && attacker.get().side()!=defender.get().side()) {
                return war.id();
            }
        }
        return null;
    }

    private boolean stillOpposed(Session session) throws SQLException {
        var war=plugin.getWars().find(session.warId);
        if (war.isEmpty() || !war.get().active()) return false;
        var attacker=plugin.getWars().participant(session.warId,session.attackerId);
        var defender=plugin.getWars().participant(session.warId,session.defenderId);
        return attacker.isPresent() && defender.isPresent() && attacker.get().side()!=defender.get().side();
    }

    private void alert(Session s,String message) {
        try {
            var participants=plugin.getWars().participants(s.warId);
            Set<String> ids=new HashSet<>();
            for(var p:participants) ids.add(p.nationId());
            for(Player online:Bukkit.getOnlinePlayers()) {
                var own=plugin.getNationDatabase().findMembership(online.getUniqueId());
                if(own.isPresent() && ids.contains(own.get().nationId())) say(online,message,NamedTextColor.GOLD);
            }
        } catch(SQLException e) {
            plugin.getLogger().log(Level.WARNING,"Siege announcement failed",e);
        }
        plugin.getLogger().info(message);
    }

    public Result begin(Player player) {
        var core=plugin.getCoreProtection().findAt(player.getLocation());
        if(core.isEmpty()) return Result.NO_CORE;
        if(active.containsKey(core.get().id())) return Result.ALREADY_ACTIVE;
        try {
            var own=plugin.getNationDatabase().findMembership(player.getUniqueId());
            if(own.isEmpty()) return Result.NO_NATION;
            if(own.get().nationId().equals(core.get().nationId())) return Result.OWN_CORE;
            Long warId=findOpposingWar(own.get().nationId(),core.get().nationId());
            if(warId==null) return Result.NO_OPPOSING_WAR;
            Instant now=Instant.now();
            if(!SiegeRules.openAt(now)) return Result.OUTSIDE_HOURS;
            int roster=plugin.getNationDatabase().findMembers(core.get().nationId()).size();
            int online=defenderOnlineCount(core.get().nationId());
            if(!SiegeRules.mayBegin(now,roster,online)) return Result.TOO_FEW_DEFENDERS;
            Session s=new Session(core.get(),own.get().nationId(),warId,now);
            active.put(s.coreId,s);
            alert(s,"Rozpoczeto legalne oblezenie przeciwko "+s.defenderName+" w wojnie #"+s.warId+".");
            return Result.STARTED;
        } catch(SQLException error) {
            plugin.getLogger().log(Level.SEVERE,"Failed to evaluate siege start",error);
            return Result.ERROR;
        }
    }

    public void sendStartResult(Player p,Result result) {
        String message=switch(result) {
            case STARTED -> "Oblezenie rozpoczete. Strefa jest aktywna; niszczenie i przejmowanie dodamy pozniej.";
            case NO_CORE -> "Wejdz do chronionego obszaru 65x65 wrogiego rdzenia.";
            case NO_NATION -> "Musisz nalezec do panstwa uczestniczacego w wojnie.";
            case OWN_CORE -> "Nie mozesz oblegac wlasnego rdzenia.";
            case NO_OPPOSING_WAR -> "Brak aktywnej wojny, w ktorej Twoj kraj walczy przeciw wlascicielowi rdzenia.";
            case OUTSIDE_HOURS -> "Oblezenia wolno zaczynac tylko 10:00-00:00 Europe/Warsaw.";
            case TOO_FEW_DEFENDERS -> "Za malo obroncow online (minimum 20% pelnego skladu, co najmniej 1).";
            case ALREADY_ACTIVE -> "Ten rdzen jest juz oblegany.";
            case ERROR -> "Blad przy sprawdzaniu oblezenia; zobacz konsole serwera.";
        };
        say(p,message,result==Result.STARTED?NamedTextColor.GREEN:NamedTextColor.YELLOW);
    }

    /** Dynamic status for the core surrounding the player. Never discloses core coordinates. */
    public List<String> describe(Player p) {
        Instant now=Instant.now();
        List<String> lines=new ArrayList<>();
        ZonedDateTime local=now.atZone(SiegeRules.WARSAW);
        lines.add("Czas Polski: "+local.format(CLOCK)+" | Okno: "+(SiegeRules.openAt(now)?"OTWARTE":"ZAMKNIETE"));
        var core=plugin.getCoreProtection().findAt(p.getLocation());
        if(core.isEmpty()) {
            lines.add("Nie jestes na chronionym obszarze zadnego rdzenia.");
            return lines;
        }
        try {
            var c=core.get();
            int roster=plugin.getNationDatabase().findMembers(c.nationId()).size();
            int online=defenderOnlineCount(c.nationId());
            lines.add("Broni: "+c.nationName()+" | Obecnosc: "+online+"/"+roster+", wymagane: "+SiegeRules.requiredDefenders(roster));
            Session s=active.get(c.id());
            if(s==null) {
                lines.add("Stan: brak aktywnego oblezenia tego rdzenia.");
                var own=plugin.getNationDatabase().findMembership(p.getUniqueId());
                if(own.isPresent() && !own.get().nationId().equals(c.nationId())) {
                    lines.add(findOpposingWar(own.get().nationId(),c.nationId())==null
                            ? "Nie jestes strona wojny przeciw obroncom."
                            : "Mozesz sprobowac rozpoczac oblezenie.");
                }
            } else {
                lines.add("Stan: AKTYWNE | Wojna #"+s.warId);
                if(s.missingSince!=null) {
                    long remaining=Math.max(0,Duration.between(now,s.missingSince.plus(SiegeRules.GRACE_PERIOD)).toSeconds());
                    lines.add("Obroncow za malo! Tolerancja: "+remaining+" sekund.");
                } else lines.add("Prog obroncow spelniony.");
            }
        } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE,"Failed to describe siege",e);
            lines.add("Blad odczytu SQLite.");
        }
        return lines;
    }

    public void status(Player player) {
        for(String line:describe(player)) say(player,line,NamedTextColor.AQUA);
    }

    /** Called by synchronous scheduler every 20 server ticks. Timeouts use Instant, not ticks. */
    public void tick() {
        if(active.isEmpty()) return;
        Instant now=Instant.now();
        var it=active.values().iterator();
        while(it.hasNext()) {
            Session s=it.next();
            String ended=null;
            try {
                if(!SiegeRules.openAt(now)) ended="Koniec dozwolonych godzin (polnoc w Polsce).";
                else if(plugin.getCoreProtection().all().stream().noneMatch(c->c.id().equals(s.coreId)
                        && c.nationId().equals(s.defenderId))) ended="Rdzen usuniety lub zmienil wlasciciela.";
                else if(!stillOpposed(s)) ended="Wojna zakonczona lub zmienili sie jej uczestnicy.";
                else {
                    int roster=plugin.getNationDatabase().findMembers(s.defenderId).size();
                    int online=defenderOnlineCount(s.defenderId);
                    if(online>=SiegeRules.requiredDefenders(roster)) {
                        if(s.missingSince!=null) {
                            s.missingSince=null;
                            alert(s,"Oblezenie #"+s.warId+": obroncy wrocili, tolerancja 30 minut wyzerowana.");
                        }
                    } else if(s.missingSince==null) {
                        s.missingSince=now;
                        alert(s,"Oblezenie #"+s.warId+": obroncow za malo; pozostalo 30 minut.");
                    } else if(SiegeRules.graceExpired(s.missingSince,now)) {
                        ended="Minelo 30 minut bez wymaganej liczby obroncow.";
                    }
                }
            } catch(SQLException e) {
                plugin.getLogger().log(Level.SEVERE,"Cannot validate siege; failing closed",e);
                ended="Blad odczytu danych — oblezenie zatrzymane ze wzgledow bezpieczenstwa.";
            }
            if(ended!=null) {
                it.remove();
                alert(s,"Oblezenie przeciwko "+s.defenderName+" (wojna #"+s.warId+") zakonczone: "+ended);
            }
        }
    }

    /** Admin-only emergency action, currently targeted at the core the admin stands inside. */
    public boolean adminStopHere(Player player) {
        var core=plugin.getCoreProtection().findAt(player.getLocation());
        if(core.isEmpty()) return false;
        Session s=active.remove(core.get().id());
        if(s==null) return false;
        alert(s,"Administrator zakonczyl oblezenie przeciwko "+s.defenderName+".");
        return true;
    }

    public void shutdown() {
        if(!active.isEmpty()) plugin.getLogger().info("Stopping "+active.size()+" siege(s) on server shutdown");
        active.clear();
    }
}
