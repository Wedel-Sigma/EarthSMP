package io.github.wedelsigma.earthsmp.core;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Admin/console fallback. All business rules are enforced inside WarRepository. */
public final class WarCommand {
    private final EarthSmpCorePlugin plugin;
    private final WarRepository repo;
    public WarCommand(EarthSmpCorePlugin plugin) {
        this.plugin=plugin;this.repo=plugin.getWars();
    }
    private void send(CommandSender user,String text,NamedTextColor c) {
        user.sendMessage(Component.text(text,c));
    }
    private void broadcast(String text) {
        for(Player p:Bukkit.getOnlinePlayers()) send(p,text,NamedTextColor.GOLD);
        plugin.getLogger().info(text);
    }
    private void alertWar(long warId,String text) {
        try {
            var nations=repo.participants(warId);
            for(Player player:Bukkit.getOnlinePlayers()) {
                var own=plugin.getNationDatabase().findMembership(player.getUniqueId());
                if(own.isPresent() && nations.stream().anyMatch(p->p.nationId().equals(own.get().nationId()))) {
                    send(player,text,NamedTextColor.YELLOW);
                }
            }
        } catch(SQLException e){plugin.getLogger().log(Level.SEVERE,"Could not announce war event",e);}
        plugin.getLogger().info(text);
    }
    private void error(CommandSender sender,String action,SQLException error) {
        plugin.getLogger().log(Level.SEVERE,action,error);
        send(sender,"Blad SQLite. Sprawdz konsole serwera.",NamedTextColor.RED);
    }
    private static long number(String v) {
        try {return Long.parseLong(v);}catch(NumberFormatException ex){return -1L;}
    }
    private Optional<String> nationId(String name) throws SQLException {
        return repo.listNations().stream().filter(n->n.name().equalsIgnoreCase(name.strip()))
                .map(WarRepository.NationOption::id).findFirst();
    }
    public boolean handle(CommandSender sender,String[] args) {
        String op=args.length>=2?args[1].toLowerCase(java.util.Locale.ROOT):"menu";
        if(op.equals("menu")) {
            if(sender instanceof Player p) plugin.getWarMenu().openMain(p);
            else send(sender,"Menu dostepne tylko w grze.",NamedTextColor.YELLOW);
            return true;
        }
        if(op.equals("list")) {
            try {
                var wars=repo.activeWars();
                send(sender,"Aktywne wojny: "+wars.size(),NamedTextColor.YELLOW);
                for(var w:wars) send(sender,"#"+w.id()+" "+w.attackerName()+" vs "+w.defenderName(),NamedTextColor.GRAY);
            }catch(SQLException e){error(sender,"War list",e);}
            return true;
        }
        if(op.equals("adminend") && args.length==3 && sender.hasPermission("earthsmp.admin")) {
            try {
                long id=number(args[2]);
                var result=repo.adminEnd(id,sender instanceof Player p?p.getUniqueId():new UUID(0L,0L));
                if (result.status() == WarRepository.Status.SUCCESS) {
                    broadcast("[" + plugin.getDisplayName() + "] Admin zakonczyl wojne #" + id);
                    plugin.getEventTitles().everyone("KONIEC WOJNY", "Administrator zakończył wojnę #" + id, NamedTextColor.GOLD);
                } else report(sender, result.status());
            }catch(SQLException e){error(sender,"War admin end",e);}
            return true;
        }
        if(!(sender instanceof Player player)) {send(sender,"Ta komenda wymaga gracza.",NamedTextColor.RED);return true;}
        try {
            if(op.equals("declare") && args.length>=3) {
                String input=String.join(" ",java.util.Arrays.copyOfRange(args,2,args.length));
                String[] split=input.split("\\s*\\|\\s*",2);
                var id=nationId(split[0]);
                if(id.isEmpty()) {send(player,"Nie znaleziono panstwa. Nazwy z odstępami mozna podac przed |.",NamedTextColor.YELLOW);return true;}
                declare(player,id.get(),split.length==2?split[1]:""); return true;
            }
            if((op.equals("invite") && args.length>=4)) {
                var id=nationId(String.join(" ",java.util.Arrays.copyOfRange(args,3,args.length)));
                if(id.isEmpty()) send(player,"Nie znaleziono panstwa.",NamedTextColor.YELLOW);
                else invite(player,number(args[2]),id.get());
                return true;
            }
            if(args.length>=3) {
                long warId=number(args[2]);
                switch(op) {
                    case "accept" -> answer(player,warId,true);
                    case "decline" -> answer(player,warId,false);
                    case "leave" -> leave(player,warId);
                    case "peace" -> peace(player,warId);
                    case "cancelpeace" -> cancelPeace(player,warId);
                    default -> help(player);
                }
                return true;
            }
            help(player);
        }catch(SQLException e){error(player,"Diplomacy command",e);}
        return true;
    }
    private void help(CommandSender p) {
        send(p,"/esmp war menu | list | declare <panstwo> | <powod>",NamedTextColor.YELLOW);
        send(p,"/esmp war invite <id> <panstwo> | accept/decline <id>",NamedTextColor.YELLOW);
        send(p,"/esmp war accept/decline/leave/peace/cancelpeace <id>",NamedTextColor.YELLOW);
    }
    private void report(CommandSender sender,WarRepository.Status status) {
        String info=switch(status) {
            case NOT_AUTHORIZED,NO_NATION -> "Tylko lider lub zastepca wlasnego panstwa.";
            case NATION_NOT_FOUND -> "Nie znaleziono panstwa.";
            case SAME_NATION -> "Nie mozesz wypowiedziec wojny samemu sobie.";
            case WAR_ALREADY_EXISTS -> "Miedzy tymi glownymi stronami trwa juz wojna.";
            case WAR_NOT_FOUND -> "Nie znaleziono wojny.";
            case NOT_ACTIVE -> "Ta wojna juz sie zakonczyla.";
            case NOT_PARTICIPANT -> "Twoj kraj nie uczestniczy w tej wojnie.";
            case ALREADY_PARTICIPANT -> "To panstwo juz walczy w tej wojnie.";
            case ALREADY_INVITED -> "To panstwo ma juz zaproszenie do tej wojny.";
            case INVITE_NOT_FOUND -> "Brak zaproszenia do tej wojny.";
            case JOIN_WINDOW_CLOSED -> "Uplynely 4 godziny od wypowiedzenia wojny.";
            case PRIMARY_CANNOT_LEAVE -> "Glowni uczestnicy nie moga sie jednostronnie wycofac.";
            case ONLY_PRIMARIES_CAN_PEACE -> "Pokoj musza uzgodnic obie glowne strony wojny.";
            case PEACE_ALREADY_OFFERED -> "Twoj kraj juz zlozyl propozycje pokoju.";
            default -> "Status: "+status;
        };
        send(sender,info,NamedTextColor.RED);
    }
    public void declare(Player player,String defenderId,String reason) throws SQLException {
        var o=repo.declare(player.getUniqueId(),defenderId,reason);
        if(o.status()!=WarRepository.Status.SUCCESS) {report(player,o.status());return;}
        var w=repo.find(o.warId()).orElseThrow();
        broadcast("[" + plugin.getDisplayName() + "] WOJNA #"+w.id()+": "+w.attackerName()+" wypowiada wojne "+w.defenderName()+"!");
        plugin.getEventTitles().everyone("WOJNA!", w.attackerName() + " vs " + w.defenderName(), NamedTextColor.RED);
        if(!w.declaration().isBlank()) broadcast("[" + plugin.getDisplayName() + "] Ogłoszenie: "+w.declaration());
        broadcast("[" + plugin.getDisplayName() + "] Sojusznicy moga dolaczyc przez pierwsze 4 godziny.");
    }
    public void invite(Player player,long warId,String targetId) throws SQLException {
        var o=repo.inviteAlly(player.getUniqueId(),warId,targetId);
        if(o.status()!=WarRepository.Status.SUCCESS) {report(player,o.status());return;}
        String nation=repo.listNations().stream().filter(n->n.id().equals(targetId))
                .map(WarRepository.NationOption::name).findFirst().orElse(targetId);
        alertWar(warId,"[" + plugin.getDisplayName() + "] "+player.getName()+" zaprosil panstwo "+nation+" do wojny #"+warId);
        for(Player online:Bukkit.getOnlinePlayers()) {
            var m=plugin.getNationDatabase().findMembership(online.getUniqueId());
            if(m.isPresent() && m.get().nationId().equals(targetId))
                {
                    send(online,"[" + plugin.getDisplayName() + "] Zaproszenie do wojny #"+warId+". Otworz /esmp -> Dyplomacja.",NamedTextColor.GREEN);
                    plugin.getEventTitles().show(online, "ZAPROSZENIE", "Do wojny #" + warId, NamedTextColor.GOLD);
                }
        }
    }
    public void answer(Player player,long warId,boolean yes) throws SQLException {
        var o=repo.answerInvite(player.getUniqueId(),warId,yes);
        if(o.status()!=WarRepository.Status.SUCCESS) {report(player,o.status());return;}
        String name=plugin.getNationDatabase().findMembership(player.getUniqueId()).orElseThrow().nationName();
        alertWar(warId,"[" + plugin.getDisplayName() + "] "+name+(yes?" dolacza do wojny ":" odrzuca zaproszenie do wojny ")+"#"+warId);
        if (yes) plugin.getEventTitles().warParticipants(warId, "NOWY SOJUSZNIK", name + " dołącza do wojny", NamedTextColor.GOLD);
        send(player,"Zapisano decyzje.",NamedTextColor.GREEN);
    }
    public void leave(Player player,long warId) throws SQLException {
        String name=plugin.getNationDatabase().findMembership(player.getUniqueId()).map(NationDatabase.Membership::nationName)
                .orElse(player.getName());
        var o=repo.leaveWar(player.getUniqueId(),warId);
        if(o.status()!=WarRepository.Status.SUCCESS) {report(player,o.status());return;}
        alertWar(warId,"[" + plugin.getDisplayName() + "] "+name+" opuszcza wojne #"+warId);
        plugin.getEventTitles().warParticipants(warId, "WYCOFANIE", name + " opuszcza wojnę", NamedTextColor.GOLD);
        send(player,"Twoj kraj wycofal sie z wojny.",NamedTextColor.GREEN);
    }
    public void peace(Player player,long warId) throws SQLException {
        var o=repo.peace(player.getUniqueId(),warId);
        if(o.status()==WarRepository.Status.PEACE_PROPOSED) {
            alertWar(warId,"[" + plugin.getDisplayName() + "] Propozycja pokoju w wojnie #"+warId+". Druga glowna strona musi ja zaakceptowac.");
            plugin.getEventTitles().warParticipants(warId, "PROPOZYCJA POKOJU", "Wojna #" + warId, NamedTextColor.GOLD);
        } else if(o.status()==WarRepository.Status.PEACE_ENDED) {
            broadcast("[" + plugin.getDisplayName() + "] POKOJ! Wojna #"+warId+" zakonczyla sie za zgoda obu glownych panstw.");
            plugin.getEventTitles().everyone("POKÓJ!", "Wojna #" + warId + " zakończona", NamedTextColor.GREEN);
        } else report(player,o.status());
    }
    public void cancelPeace(Player player,long warId) throws SQLException {
        var o=repo.cancelPeace(player.getUniqueId(),warId);
        if(o.status()==WarRepository.Status.PEACE_CANCELLED) alertWar(warId,"[" + plugin.getDisplayName() + "] Wycofano propozycje pokoju w wojnie #"+warId);
        else report(player,o.status());
    }
}
