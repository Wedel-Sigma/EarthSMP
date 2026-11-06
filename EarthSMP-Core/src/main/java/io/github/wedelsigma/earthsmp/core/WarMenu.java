package io.github.wedelsigma.earthsmp.core;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

/** GUI-only player workflow; all mutations delegate to WarRepository through WarCommand. */
public final class WarMenu implements Listener {
    private static final int PAGE_SIZE=45;
    private final EarthSmpCorePlugin plugin;
    private enum Screen {
        MAIN, WARS, WAR_DETAILS, TARGETS, DECLARE_CONFIRM, INVITES, INVITE_DETAILS,
        ALLY_TARGETS, PEACE_CONFIRM, LEAVE_CONFIRM, PARTICIPANTS
    }
    private static final class Holder implements InventoryHolder {
        final UUID viewer;
        final Screen screen;
        final long warId;
        final String nationId;
        final int page;
        List<String> ids=List.of();
        List<Long> warIds=List.of();
        final Inventory inv;
        Holder(Player player,Screen screen,int page,long warId,String nationId,String title) {
            this.viewer=player.getUniqueId();this.screen=screen;this.page=page;
            this.warId=warId;this.nationId=nationId;
            this.inv=Bukkit.createInventory(this,54,Component.text(title,NamedTextColor.GOLD));
        }
        @Override public @NotNull Inventory getInventory(){return inv;}
    }
    private record Draft(String targetId,String text) {}
    private final Map<UUID,Draft> drafts=new HashMap<>();
    private final Map<UUID,String> waitingForMessage=new java.util.concurrent.ConcurrentHashMap<>();

    public WarMenu(EarthSmpCorePlugin plugin){this.plugin=plugin;}
    private WarRepository db(){return plugin.getWars();}
    private static boolean manager(String role){return "LEADER".equals(role)||"DEPUTY".equals(role);}
    private void warn(Player p,String message){p.sendMessage(Component.text(message,NamedTextColor.YELLOW));}
    private void err(Player p,String context,SQLException ex){
        plugin.getLogger().log(Level.SEVERE,context,ex);
        warn(p,"Blad bazy danych. Sprawdz konsole serwera.");
    }
    public void openMain(Player p){
        try {
            var own=plugin.getNationDatabase().findMembership(p.getUniqueId());
            Holder h=new Holder(p,Screen.MAIN,0,0,"",plugin.getDisplayName() + " | Dyplomacja");
            h.inv.setItem(4,item(Material.IRON_SWORD,"Dyplomacja " + plugin.getDisplayName(),NamedTextColor.GOLD,
                "Panstwo: "+own.map(NationDatabase.Membership::nationName).orElse("brak")));
            h.inv.setItem(20,item(Material.MAP,"Aktywne wojny: "+db().activeWars().size(),NamedTextColor.YELLOW,
                "Zobacz konflikty i szczegoly stron"));
            h.inv.setItem(22,item(Material.NETHERITE_SWORD,"Wypowiedz wojne",NamedTextColor.RED,
                "Lider lub zastepca moze wypowiedziec wojne", "Powod jest opcjonalny"));
            h.inv.setItem(24,item(Material.PAPER,"Zaproszenia do wojen: "+db().invitationsFor(p.getUniqueId()).size(),
                NamedTextColor.GREEN,"Dolacz do sojusznikow w pierwszych 4 godzinach"));
            h.inv.setItem(49,back());p.openInventory(h.inv);
        }catch(SQLException e){err(p,"Open diplomacy hub",e);}
    }
    private void wars(Player p,int wanted){
        try{
            var wars=db().activeWars();int page=clamp(wanted,wars.size());
            Holder h=new Holder(p,Screen.WARS,page,0,"",plugin.getDisplayName() + " | Aktywne wojny");
            List<Long> ids=new ArrayList<>();
            for(int i=0;i<PAGE_SIZE && page*PAGE_SIZE+i<wars.size();i++){
                var w=wars.get(page*PAGE_SIZE+i);ids.add(w.id());
                h.inv.setItem(i,item(Material.IRON_SWORD,"#"+w.id()+" "+w.attackerName()+" vs "+w.defenderName(),
                    NamedTextColor.YELLOW,"Kliknij, zeby zobaczyc szczegoly"));
            }
            h.warIds=List.copyOf(ids);navigation(h.inv,page,wars.size());p.openInventory(h.inv);
        }catch(SQLException e){err(p,"List wars GUI",e);}
    }
    private void targets(Player p,int wanted){
        try{
            var own=plugin.getNationDatabase().findMembership(p.getUniqueId());
            if(own.isEmpty()||!manager(own.get().role())){warn(p,"Tylko lider lub zastepca wypowiada wojne.");openMain(p);return;}
            var choices=db().listNations().stream().filter(n->!n.id().equals(own.get().nationId())).toList();
            int page=clamp(wanted,choices.size());Holder h=new Holder(p,Screen.TARGETS,page,0,"","Wypowiedz wojne | Cel");
            List<String> ids=new ArrayList<>();
            for(int i=0;i<PAGE_SIZE&&page*PAGE_SIZE+i<choices.size();i++){
                var n=choices.get(page*PAGE_SIZE+i);ids.add(n.id());
                h.inv.setItem(i,item(Material.RED_BANNER,n.name(),NamedTextColor.RED,"Wybierz panstwo i potwierdz wojne"));
            }
            h.ids=List.copyOf(ids);navigation(h.inv,page,choices.size());p.openInventory(h.inv);
        }catch(SQLException e){err(p,"War target list",e);}
    }
    private void declaration(Player p,String targetId){
        try {
            var target=db().listNations().stream().filter(n->n.id().equals(targetId)).findFirst();
            if(target.isEmpty()){warn(p,"Panstwo nie istnieje.");targets(p,0);return;}
            drafts.computeIfAbsent(p.getUniqueId(),id->new Draft(targetId,""));
            var d=drafts.get(p.getUniqueId());
            if(!d.targetId().equals(targetId)){d=new Draft(targetId,"");drafts.put(p.getUniqueId(),d);}
            Holder h=new Holder(p,Screen.DECLARE_CONFIRM,0,0,targetId,plugin.getDisplayName() + " | Wypowiedz wojne");
            h.inv.setItem(13,item(Material.RED_BANNER,"Cel: "+target.get().name(),NamedTextColor.RED,
                "Wojna zacznie sie od razu po zatwierdzeniu"));
            h.inv.setItem(29,item(Material.WRITABLE_BOOK,"Dodaj powod (opcjonalny)",NamedTextColor.YELLOW,
                d.text().isBlank()?"Brak dodatkowego powodu":"Powod: "+cut(d.text(),55),
                "Kliknij, a nastepnie napisz wiadomosc na czacie", "Wpisz 'anuluj', by wyczyscic powod"));
            h.inv.setItem(11,confirm("Wypowiedz wojne"));h.inv.setItem(15,cancel());
            h.inv.setItem(49,back());p.openInventory(h.inv);
        }catch(SQLException e){err(p,"War declaration GUI",e);}
    }
    private void details(Player p,long warId){
        try {
            var maybe=db().find(warId);
            if(maybe.isEmpty() || !maybe.get().active()){warn(p,"Wojna nie jest juz aktywna.");wars(p,0);return;}
            var w=maybe.get();var own=plugin.getNationDatabase().findMembership(p.getUniqueId());
            var participation=own.isEmpty()?Optional.<WarRepository.Participant>empty():
                    db().participant(warId,own.get().nationId());
            boolean mgr=own.isPresent()&&manager(own.get().role());
            Holder h=new Holder(p,Screen.WAR_DETAILS,0,warId,"",plugin.getDisplayName() + " | Wojna #"+warId);
            h.inv.setItem(4,item(Material.IRON_SWORD,w.attackerName()+" vs "+w.defenderName(),NamedTextColor.GOLD,
                "Powod: "+(w.declaration().isBlank()?"brak":cut(w.declaration(),65)),
                "Okno sojusznikow: "+(w.joinWindowOpen(Instant.now().getEpochSecond())?"OTWARTE":"ZAMKNIETE")));
            h.inv.setItem(20,item(Material.PLAYER_HEAD,"Uczestnicy",NamedTextColor.GREEN,
                "Obie strony i dolaczeni sojusznicy"));
            if(mgr && participation.isPresent()) {
                var part=participation.get();
                if(w.joinWindowOpen(Instant.now().getEpochSecond()))
                    h.inv.setItem(22,item(Material.LIME_BANNER,"Zapros sojusznika",NamedTextColor.GREEN,
                        "Wybierz panstwo, aby wyslac zaproszenie"));
                if(part.primaryParty()) {
                    String offer=w.peaceOfferedBy();
                    boolean ownOffer=own.get().nationId().equals(offer);
                    h.inv.setItem(24,item(Material.WHITE_BANNER,
                        ownOffer?"Wycofaj propozycje pokoju":offer==null?"Zaproponuj pokoj":"Zaakceptuj pokoj",
                        NamedTextColor.AQUA,"Wymaga decyzji obu glownych stron"));
                } else {
                    h.inv.setItem(24,item(Material.BARRIER,"Opusc wojne",NamedTextColor.YELLOW,
                        "Sojusznik moze odejsc w kazdej chwili"));
                }
            }
            h.inv.setItem(49,back());p.openInventory(h.inv);
        }catch(SQLException e){err(p,"Open war details",e);}
    }
    private void people(Player p,long warId){
        try{
            var participants=db().participants(warId);
            Holder h=new Holder(p,Screen.PARTICIPANTS,0,warId,"",plugin.getDisplayName() + " | Strony wojny");
            for(int i=0;i<participants.size()&&i<45;i++){
                var x=participants.get(i);
                h.inv.setItem(i,item(x.side()==WarRepository.Side.ATTACKER?Material.RED_BANNER:Material.BLUE_BANNER,
                    x.nationName(),NamedTextColor.YELLOW,
                    x.side()==WarRepository.Side.ATTACKER?"Strona atakujaca":"Strona broniaca",
                    x.primaryParty()?"GLOWNA STRONA":"SOJUSZNIK / NAJEMNIK"));
            }
            h.inv.setItem(49,back());p.openInventory(h.inv);
        }catch(SQLException e){err(p,"Participants",e);}
    }
    private void allyTargets(Player p,long warId,int requestedPage){
        try{
            var own=plugin.getNationDatabase().findMembership(p.getUniqueId());
            var w=db().find(warId);
            if(own.isEmpty()||!manager(own.get().role())||w.isEmpty()||!w.get().joinWindowOpen(Instant.now().getEpochSecond())||
                    db().participant(warId,own.get().nationId()).isEmpty()){
                warn(p,"Brak uprawnien albo okno sojusznikow minelo.");details(p,warId);return;
            }
            var joined=db().participants(warId).stream().map(WarRepository.Participant::nationId).toList();
            var choices=db().listNations().stream().filter(n->!joined.contains(n.id())).toList();
            int page=clamp(requestedPage,choices.size());
            Holder h=new Holder(p,Screen.ALLY_TARGETS,page,warId,"",plugin.getDisplayName() + " | Zapros do wojny");
            List<String> ids=new ArrayList<>();
            for(int i=0;i<PAGE_SIZE&&i+page*PAGE_SIZE<choices.size();i++){
                var n=choices.get(i+page*PAGE_SIZE);ids.add(n.id());
                h.inv.setItem(i,item(Material.LIME_BANNER,n.name(),NamedTextColor.GREEN,"Kliknij, by zaprosic do wojny"));
            }
            h.ids=List.copyOf(ids);navigation(h.inv,page,choices.size());p.openInventory(h.inv);
        }catch(SQLException e){err(p,"Invite allies GUI",e);}
    }
    private void invites(Player p,int wanted){
        try{
            var list=db().invitationsFor(p.getUniqueId());int page=clamp(wanted,list.size());
            Holder h=new Holder(p,Screen.INVITES,page,0,"",plugin.getDisplayName() + " | Zaproszenia wojenne");
            List<Long> ids=new ArrayList<>();
            for(int i=0;i<PAGE_SIZE&&page*PAGE_SIZE+i<list.size();i++){
                var x=list.get(page*PAGE_SIZE+i);ids.add(x.warId());
                h.inv.setItem(i,item(Material.PAPER,"Wojna #"+x.warId(),NamedTextColor.GREEN,
                    x.attackerName()+" vs "+x.defenderName(),
                    "Zaproszenie na strone "+x.side()));
            }
            h.warIds=List.copyOf(ids);navigation(h.inv,page,list.size());p.openInventory(h.inv);
        }catch(SQLException e){err(p,"War invites GUI",e);}
    }
    private void invitation(Player p,long warId){
        try {
            boolean invited=db().invitationsFor(p.getUniqueId()).stream().anyMatch(i->i.warId()==warId);
            if(!invited){warn(p,"Zaproszenie wygaslo lub zostalo anulowane.");invites(p,0);return;}
            Holder h=new Holder(p,Screen.INVITE_DETAILS,0,warId,"",plugin.getDisplayName() + " | Decyzja sojuszu");
            h.inv.setItem(13,item(Material.PAPER,"Zaproszenie do wojny #"+warId,NamedTextColor.GOLD,
                "Dolaczenie jest mozliwe tylko w pierwszych 4h"));
            h.inv.setItem(11,confirm("Przyjmij zaproszenie"));
            h.inv.setItem(15,cancel("Odrzuc zaproszenie"));
            h.inv.setItem(49,back());p.openInventory(h.inv);
        }catch(SQLException e){err(p,"War invitation GUI",e);}
    }
    private void confirmWarAction(Player p,long warId,boolean peace){
        try {
            var w=db().find(warId);
            if(w.isEmpty()||!w.get().active()){wars(p,0);return;}
            Holder h=new Holder(p,peace?Screen.PEACE_CONFIRM:Screen.LEAVE_CONFIRM,0,warId,"",
                peace?plugin.getDisplayName() + " | Pokoj":plugin.getDisplayName() + " | Wycofanie");
            h.inv.setItem(13,item(Material.BOOK,peace?"Propozycja / przyjecie pokoju":"Wycofaj panstwo",
                NamedTextColor.YELLOW,"Wojna #"+warId,"Operacja wymaga potwierdzenia"));
            h.inv.setItem(11,confirm(peace?"Potwierdz pokoj":"Potwierdz wyjscie"));
            h.inv.setItem(15,cancel());h.inv.setItem(49,back());p.openInventory(h.inv);
        }catch(SQLException e){err(p,"War confirm GUI",e);}
    }
    @EventHandler
    public void click(InventoryClickEvent event){
        if(!(event.getView().getTopInventory().getHolder(false) instanceof Holder h))return;
        event.setCancelled(true);
        if(!(event.getWhoClicked() instanceof Player p) || !h.viewer.equals(p.getUniqueId()))return;
        if(event.getClickedInventory()!=h.inv)return;
        int slot=event.getRawSlot();if(slot<0||slot>=54)return;
        Runnable action=action(h,p,slot);
        if(action!=null) plugin.getServer().getScheduler().runTask(plugin,()->{
            if(p.isOnline()&&p.getOpenInventory().getTopInventory()==h.inv)action.run();
        });
    }
    private Runnable action(Holder h,Player p,int s){
        switch(h.screen){
            case MAIN -> {
                return switch(s){
                    case 20 -> ()->wars(p,0);
                    case 22 -> ()->targets(p,0);
                    case 24 -> ()->invites(p,0);
                    case 49 -> ()->plugin.getHubMenu().openMain(p);
                    default -> null;
                };
            }
            case WARS, INVITES -> {
                if(s<45&&s<h.warIds.size()){
                    long id=h.warIds.get(s);
                    return h.screen==Screen.WARS?()->details(p,id):()->invitation(p,id);
                }
                return switch(s){
                    case 45 -> h.page>0 ? ()->{if(h.screen==Screen.WARS)wars(p,h.page-1);else invites(p,h.page-1);} : null;
                    case 49 -> ()->openMain(p);
                    case 53 -> ()-> {if(h.screen==Screen.WARS)wars(p,h.page+1);else invites(p,h.page+1);};
                    default -> null;
                };
            }
            case TARGETS,ALLY_TARGETS -> {
                if(s<45&&s<h.ids.size()){
                    String id=h.ids.get(s);
                    if(h.screen==Screen.TARGETS) return ()->declaration(p,id);
                    return ()->{
                        try{plugin.getWarCommand().invite(p,h.warId,id);}catch(SQLException e){err(p,"Invite ally",e);}
                        details(p,h.warId);
                    };
                }
                return switch(s){
                    case 45 -> h.page>0 ? ()->{if(h.screen==Screen.TARGETS)targets(p,h.page-1);else allyTargets(p,h.warId,h.page-1);}:null;
                    case 49 -> h.screen==Screen.TARGETS?()->openMain(p):()->details(p,h.warId);
                    case 53 -> ()->{if(h.screen==Screen.TARGETS)targets(p,h.page+1);else allyTargets(p,h.warId,h.page+1);};
                    default -> null;
                };
            }
            case DECLARE_CONFIRM -> {
                return switch(s){
                    case 11 -> ()->{
                        Draft d=drafts.get(p.getUniqueId());
                        String reason=d!=null&&d.targetId().equals(h.nationId)?d.text():"";
                        try{plugin.getWarCommand().declare(p,h.nationId,reason);}catch(SQLException e){err(p,"Declare war",e);}
                        drafts.remove(p.getUniqueId());openMain(p);
                    };
                    case 29 -> ()->{waitingForMessage.put(p.getUniqueId(),h.nationId);p.closeInventory();
                        warn(p,"Napisz powod wojny na czacie (maks 200 znakow). Wpisz 'anuluj', by wyczyscic.");};
                    case 15,49 -> ()->{drafts.remove(p.getUniqueId());targets(p,0);};
                    default -> null;
                };
            }
            case WAR_DETAILS -> {
                return switch(s){
                    case 20 -> ()->people(p,h.warId);
                    case 22 -> ()->allyTargets(p,h.warId,0);
                    case 24 -> ()->{
                        try{
                            var own=plugin.getNationDatabase().findMembership(p.getUniqueId());
                            var war=db().find(h.warId);
                            if(own.isEmpty()||war.isEmpty()) {details(p,h.warId);return;}
                            var part=db().participant(h.warId,own.get().nationId());
                            if(part.isEmpty()) {details(p,h.warId);return;}
                            if(!part.get().primaryParty()) confirmWarAction(p,h.warId,false);
                            else if(own.get().nationId().equals(war.get().peaceOfferedBy())){
                                plugin.getWarCommand().cancelPeace(p,h.warId);details(p,h.warId);
                            }else confirmWarAction(p,h.warId,true);
                        }catch(SQLException e){err(p,"War action",e);}
                    };
                    case 49 -> ()->wars(p,0);
                    default -> null;
                };
            }
            case PARTICIPANTS -> {return s==49?()->details(p,h.warId):null;}
            case INVITE_DETAILS -> {
                return switch(s){
                    case 11,15 -> ()->{
                        try{plugin.getWarCommand().answer(p,h.warId,s==11);}catch(SQLException e){err(p,"Answer war invitation",e);}
                        invites(p,0);
                    };
                    case 49 -> ()->invites(p,0);
                    default -> null;
                };
            }
            case PEACE_CONFIRM,LEAVE_CONFIRM -> {
                return switch(s){
                    case 11 -> ()->{
                        try{
                            if(h.screen==Screen.PEACE_CONFIRM)plugin.getWarCommand().peace(p,h.warId);
                            else plugin.getWarCommand().leave(p,h.warId);
                        }catch(SQLException e){err(p,"War confirmation",e);}
                        details(p,h.warId);
                    };
                    case 15,49 -> ()->details(p,h.warId);
                    default -> null;
                };
            }
        }
        return null;
    }
    @EventHandler
    public void drag(InventoryDragEvent e){
        if(!(e.getView().getTopInventory().getHolder(false) instanceof Holder))return;
        if(e.getRawSlots().stream().anyMatch(i->i<e.getView().getTopInventory().getSize()))e.setCancelled(true);
    }
    @EventHandler
    public void onChat(AsyncChatEvent event){
        UUID id=event.getPlayer().getUniqueId();
        // The map is only modified on the main thread: read-only contains check is safe via immutable key set?
        // AsyncChatEvent may be async: schedule the GUI state mutation on the server thread.
        if(!waitingForMessage.containsKey(id))return;
        event.setCancelled(true);
        String text=PlainTextComponentSerializer.plainText().serialize(event.message()).strip();
        plugin.getServer().getScheduler().runTask(plugin,()->{
            String target=waitingForMessage.remove(id);if(target==null)return;
            Player p=Bukkit.getPlayer(id);if(p==null)return;
            if(text.equalsIgnoreCase("anuluj"))textDraft(id,target,"");
            else textDraft(id,target,text.length()>200?text.substring(0,200):text);
            declaration(p,target);
        });
    }
    private void textDraft(UUID player,String target,String text){drafts.put(player,new Draft(target,text));}
    @EventHandler public void quit(PlayerQuitEvent e){
        UUID id=e.getPlayer().getUniqueId();drafts.remove(id);waitingForMessage.remove(id);
    }
    private static int clamp(int wanted,int total){return Math.max(0,Math.min(wanted,Math.max(0,(total-1)/PAGE_SIZE)));}
    private static void navigation(Inventory inv,int page,int size){
        if(page>0)inv.setItem(45,item(Material.ARROW,"Poprzednia strona",NamedTextColor.YELLOW));
        if((page+1)*PAGE_SIZE<size)inv.setItem(53,item(Material.ARROW,"Nastepna strona",NamedTextColor.YELLOW));
        inv.setItem(49,back());
    }
    private static ItemStack item(Material material,String title,NamedTextColor color,String... lines){
        ItemStack stack=new ItemStack(material);ItemMeta meta=stack.getItemMeta();
        meta.displayName(Component.text(title,color));List<Component> lore=new ArrayList<>();
        for(String l:lines)lore.add(Component.text(l,NamedTextColor.GRAY));
        meta.lore(lore);stack.setItemMeta(meta);return stack;
    }
    private static ItemStack back(){return item(Material.OAK_DOOR,"Powrot",NamedTextColor.GREEN);}
    private static ItemStack confirm(String name){return item(Material.LIME_WOOL,name,NamedTextColor.GREEN);}
    private static ItemStack cancel(){return cancel("Anuluj");}
    private static ItemStack cancel(String name){return item(Material.RED_WOOL,name,NamedTextColor.RED);}
    private static String cut(String text,int n){return text.length()>n?text.substring(0,n-3)+"...":text;}
}
