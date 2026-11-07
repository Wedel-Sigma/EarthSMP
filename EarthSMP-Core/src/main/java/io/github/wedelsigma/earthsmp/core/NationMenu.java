package io.github.wedelsigma.earthsmp.core;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

/** Nation GUI for invitations and management of existing members. */
public final class NationMenu implements Listener {

    private static final int PAGE_SIZE = 45;
    private final EarthSmpCorePlugin plugin;
    // Chat events may run asynchronously: map access must be thread safe.
    private final Map<UUID, Long> awaitingName = new ConcurrentHashMap<>();
    private static final long NAME_PROMPT_NANOS = java.util.concurrent.TimeUnit.MINUTES.toNanos(2);

    private enum Screen {
        MAIN, MEMBERS, MEMBER_DETAILS, CONFIRM_MEMBER_ACTION, INVITES, CONFIRM_INVITE,
        INVITE_PLAYERS, CONFIRM_SEND_INVITE, OUTGOING_INVITES, CONFIRM_WITHDRAW,
        CONFIRM_LEAVE, CONFIRM_TRANSFER, CONFIRM_DISBAND
    }

    private static final class MenuHolder implements InventoryHolder {
        private final Inventory inventory;
        private final UUID viewerId;
        private final Screen screen;
        private final int page;
        private final List<String> invitationNames;
        private final String selectedNation;
        private final List<UUID> memberIds;
        private final UUID selectedMember;
        private final NationDatabase.MemberAction selectedAction;

        private MenuHolder(UUID viewerId, Screen screen, int page, String title,
                           List<String> invitationNames, String selectedNation) {
            this(viewerId, screen, page, title, invitationNames, selectedNation,
                    List.of(), null, null);
        }

        private MenuHolder(UUID viewerId, Screen screen, int page, String title,
                           List<String> invitationNames, String selectedNation,
                           List<UUID> memberIds, UUID selectedMember,
                           NationDatabase.MemberAction selectedAction) {
            this.viewerId = viewerId;
            this.screen = screen;
            this.page = page;
            this.invitationNames = List.copyOf(invitationNames);
            this.selectedNation = selectedNation;
            this.memberIds = List.copyOf(memberIds);
            this.selectedMember = selectedMember;
            this.selectedAction = selectedAction;
            this.inventory = Bukkit.createInventory(this,
                    screen == Screen.MAIN || screen == Screen.CONFIRM_INVITE
                            || screen == Screen.MEMBER_DETAILS || screen == Screen.CONFIRM_MEMBER_ACTION
                            || screen == Screen.CONFIRM_SEND_INVITE || screen == Screen.CONFIRM_WITHDRAW
                            || screen == Screen.CONFIRM_LEAVE || screen == Screen.CONFIRM_TRANSFER
                            || screen == Screen.CONFIRM_DISBAND ? 27 : 54,
                    Component.text(title, NamedTextColor.GOLD));
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    public NationMenu(EarthSmpCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player player) {
        try {
            Optional<NationDatabase.Membership> membership = plugin.getNationDatabase()
                    .findMembership(player.getUniqueId());
            int invitationCount = plugin.getNationDatabase().findInvites(player.getUniqueId()).size();
            MenuHolder holder = new MenuHolder(player.getUniqueId(), Screen.MAIN, 0,
                    plugin.getDisplayName() + " | Panstwo", List.of(), null);
            Inventory inventory = holder.getInventory();
            if (membership.isPresent()) {
                NationDatabase.Membership nation = membership.get();
                inventory.setItem(11, item(Material.WHITE_BANNER, nation.nationName(), NamedTextColor.GOLD,
                        "Twoja ranga: " + nation.role(), "Informacje o panstwie"));
                int members = plugin.getNationDatabase().findMembers(nation.nationId()).size();
                inventory.setItem(13, item(Material.PLAYER_HEAD, "Czlonkowie (" + members + ")",
                        NamedTextColor.GREEN, "Kliknij, by zobaczyc sklad panstwa"));
            } else {
                inventory.setItem(11, item(Material.EMERALD, "Zaloz panstwo", NamedTextColor.GREEN,
                        "Kliknij i wpisz nazwe panstwa na czacie"));
                inventory.setItem(13, item(Material.GRAY_DYE, "Czlonkowie", NamedTextColor.GRAY,
                        "Najpierw dolacz do panstwa"));
            }
            inventory.setItem(15, item(Material.PAPER, "Zaproszenia (" + invitationCount + ")",
                    NamedTextColor.YELLOW, "Kliknij, by otworzyc zaproszenia"));
            if (membership.isPresent()) {
                String role = membership.get().role();
                if (isManager(role)) {
                    inventory.setItem(18, item(Material.LIME_DYE, "Zapros gracza", NamedTextColor.GREEN,
                            "Wybierz gracza online bez panstwa"));
                    int outgoing = plugin.getNationDatabase().findOutgoingInvites(player.getUniqueId()).size();
                    inventory.setItem(24, item(Material.WRITABLE_BOOK, "Wyslane zaproszenia (" + outgoing + ")",
                            NamedTextColor.YELLOW, "Kliknij, by wycofac zaproszenie"));
                }
                inventory.setItem(26, "LEADER".equals(role)
                        ? item(Material.RED_DYE, "Rozwiaz panstwo", NamedTextColor.RED,
                                "Tylko podczas pokoju; wymaga potwierdzenia",
                                "Usuwa wszystkie rdzenie: wlasne i zdobyczne")
                        : item(Material.RED_DYE, "Opusc panstwo", NamedTextColor.RED,
                                "Wymaga potwierdzenia"));
            }
            inventory.setItem(22, item(Material.OAK_DOOR, "Powrot do centrum " + plugin.getDisplayName(), NamedTextColor.GREEN));
            player.openInventory(inventory);
        } catch (SQLException error) {
            reportError(player, "Opening nation main menu failed", error);
        }
    }

    private void openMembers(Player player, int requestedPage) {
        try {
            Optional<NationDatabase.Membership> membership = plugin.getNationDatabase()
                    .findMembership(player.getUniqueId());
            if (membership.isEmpty()) {
                player.sendMessage(Component.text("Nie nalezysz do zadnego panstwa.", NamedTextColor.YELLOW));
                openMain(player);
                return;
            }
            List<NationDatabase.NationMember> members = plugin.getNationDatabase()
                    .findMembers(membership.get().nationId());
            int page = clampPage(requestedPage, members.size());
            int from = page * PAGE_SIZE;
            List<UUID> memberIds = new ArrayList<>();
            for (int i = from; i < Math.min(from + PAGE_SIZE, members.size()); i++) {
                memberIds.add(members.get(i).playerUuid());
            }
            MenuHolder holder = new MenuHolder(player.getUniqueId(), Screen.MEMBERS, page,
                    plugin.getDisplayName() + " | Czlonkowie " + (page + 1), List.of(), null,
                    memberIds, null, null);
            Inventory inventory = holder.getInventory();
            for (int i = 0; i < memberIds.size(); i++) {
                NationDatabase.NationMember member = members.get(from + i);
                inventory.setItem(i, item(Material.PLAYER_HEAD, playerName(member.playerUuid()),
                        roleColor(member.role()), "Ranga: " + member.role(),
                        "Kliknij, by otworzyc szczegoly gracza"));
            }
            navigation(inventory, page > 0, from + PAGE_SIZE < members.size());
            player.openInventory(inventory);
        } catch (SQLException error) {
            reportError(player, "Opening nation members failed", error);
        }
    }

    private void openMemberDetails(Player player, UUID targetUuid, int returnPage) {
        try {
            Optional<NationDatabase.Membership> viewer = plugin.getNationDatabase()
                    .findMembership(player.getUniqueId());
            if (viewer.isEmpty()) {
                player.sendMessage(Component.text("Nie nalezysz juz do zadnego panstwa.", NamedTextColor.YELLOW));
                openMain(player);
                return;
            }
            Optional<NationDatabase.NationMember> selected = plugin.getNationDatabase()
                    .findMembers(viewer.get().nationId()).stream()
                    .filter(member -> member.playerUuid().equals(targetUuid))
                    .findFirst();
            if (selected.isEmpty()) {
                player.sendMessage(Component.text("Gracz nie nalezy juz do twojego panstwa.", NamedTextColor.YELLOW));
                openMembers(player, returnPage);
                return;
            }

            NationDatabase.NationMember target = selected.get();
            MenuHolder holder = new MenuHolder(player.getUniqueId(), Screen.MEMBER_DETAILS,
                    returnPage, plugin.getDisplayName() + " | " + playerName(targetUuid), List.of(), null,
                    List.of(), targetUuid, null);
            Inventory inventory = holder.getInventory();
            inventory.setItem(13, item(Material.PLAYER_HEAD, playerName(targetUuid),
                    roleColor(target.role()), "Ranga: " + target.role()));
            inventory.setItem(22, item(Material.ARROW, "Powrot do czlonkow", NamedTextColor.YELLOW));

            boolean manager = isManager(viewer.get().role());
            if (!manager) {
                inventory.setItem(4, item(Material.BOOK, "Tylko podglad", NamedTextColor.GRAY,
                        "Rangami zarzadzaja lider i zastepcy"));
            } else if (targetUuid.equals(player.getUniqueId())) {
                inventory.setItem(4, item(Material.BOOK, "To Ty", NamedTextColor.YELLOW,
                        "Nie mozesz zmieniac swojej rangi"));
            } else if ("LEADER".equals(target.role())) {
                inventory.setItem(4, item(Material.BOOK, "Lider", NamedTextColor.YELLOW,
                        "Nie mozna zmienic rangi ani wyrzucic lidera"));
            } else {
                if ("LEADER".equals(viewer.get().role())) {
                    inventory.setItem(4, item(Material.NETHER_STAR, "Przekaz przywodztwo", NamedTextColor.GOLD,
                            "Tylko lider; wymaga potwierdzenia"));
                }
                if ("MEMBER".equals(target.role())) {
                    inventory.setItem(10, item(Material.LIME_DYE, "Awansuj na zastepce",
                            NamedTextColor.GREEN, "Maksymalnie 2 zastepcow w panstwie"));
                } else if ("DEPUTY".equals(target.role())) {
                    inventory.setItem(16, item(Material.ORANGE_DYE, "Degraduj do czlonka",
                            NamedTextColor.YELLOW));
                }
                inventory.setItem(20, item(Material.RED_DYE, "Wyrzuc z panstwa",
                        NamedTextColor.RED, "Wymaga osobnego potwierdzenia"));
            }
            player.openInventory(inventory);
        } catch (SQLException error) {
            reportError(player, "Opening nation member details failed", error);
        }
    }

    private void openMemberConfirmation(Player player, UUID targetUuid, int returnPage,
                                        NationDatabase.MemberAction action) {
        // A GUI is only a request: the database checks all permissions again on confirmation.
        try {
            Optional<NationDatabase.Membership> viewer = plugin.getNationDatabase()
                    .findMembership(player.getUniqueId());
            if (viewer.isEmpty()) {
                openMain(player);
                return;
            }
            Optional<NationDatabase.NationMember> target = plugin.getNationDatabase()
                    .findMembers(viewer.get().nationId()).stream()
                    .filter(member -> member.playerUuid().equals(targetUuid))
                    .findFirst();
            if (target.isEmpty() || !isManager(viewer.get().role())
                    || player.getUniqueId().equals(targetUuid) || "LEADER".equals(target.get().role())) {
                player.sendMessage(Component.text("Nie mozesz zarzadzac tym graczem.", NamedTextColor.YELLOW));
                openMembers(player, returnPage);
                return;
            }
            if ((action == NationDatabase.MemberAction.PROMOTE && !"MEMBER".equals(target.get().role()))
                    || (action == NationDatabase.MemberAction.DEMOTE && !"DEPUTY".equals(target.get().role()))) {
                player.sendMessage(Component.text("Ranga gracza juz sie zmienila.", NamedTextColor.YELLOW));
                openMemberDetails(player, targetUuid, returnPage);
                return;
            }
            MenuHolder holder = new MenuHolder(player.getUniqueId(), Screen.CONFIRM_MEMBER_ACTION,
                    returnPage, plugin.getDisplayName() + " | Potwierdzenie", List.of(), null,
                    List.of(), targetUuid, action);
            Inventory inventory = holder.getInventory();
            inventory.setItem(13, item(Material.PLAYER_HEAD, playerName(targetUuid),
                    roleColor(target.get().role()), actionLabel(action)));
            inventory.setItem(11, item(Material.LIME_WOOL, "Potwierdz", NamedTextColor.GREEN,
                    "Wykonaj: " + actionLabel(action)));
            inventory.setItem(15, item(Material.RED_WOOL, "Anuluj", NamedTextColor.RED,
                    "Nie wprowadzaj zmian"));
            inventory.setItem(22, item(Material.ARROW, "Powrot", NamedTextColor.YELLOW));
            player.openInventory(inventory);
        } catch (SQLException error) {
            reportError(player, "Opening nation action confirmation failed", error);
        }
    }

    private void performMemberAction(Player player, UUID targetUuid, int returnPage,
                                     NationDatabase.MemberAction action) {
        try {
            NationDatabase.ManageResult result = plugin.getNationDatabase().manageMember(
                    player.getUniqueId(), targetUuid, action);
            switch (result) {
                case SUCCESS -> {
                    player.sendMessage(Component.text(playerName(targetUuid) + ": "
                            + actionLabel(action) + " - wykonano.", NamedTextColor.GREEN));
                    Player onlineTarget = Bukkit.getPlayer(targetUuid);
                    if (onlineTarget != null) {
                        plugin.getEventTitles().show(onlineTarget, "ZMIANA RANGI", actionLabel(action), NamedTextColor.GOLD);
                        onlineTarget.sendMessage(Component.text("W panstwie wykonano: "
                                + actionLabel(action) + " (przez " + player.getName() + ")",
                                NamedTextColor.YELLOW));
                    }
                }
                case NOT_AUTHORIZED -> player.sendMessage(Component.text(
                        "Nie masz uprawnien do zarzadzania czlonkami.", NamedTextColor.RED));
                case NOT_IN_SAME_NATION -> player.sendMessage(Component.text(
                        "Gracz nie nalezy juz do twojego panstwa.", NamedTextColor.YELLOW));
                case CANNOT_MODIFY_LEADER -> player.sendMessage(Component.text(
                        "Nie mozna modyfikowac lidera.", NamedTextColor.RED));
                case CANNOT_MODIFY_SELF -> player.sendMessage(Component.text(
                        "Nie mozesz zmieniac swojej rangi.", NamedTextColor.YELLOW));
                case INVALID_ROLE -> player.sendMessage(Component.text(
                        "Ta zmiana rangi nie jest juz mozliwa.", NamedTextColor.YELLOW));
                case DEPUTY_LIMIT -> player.sendMessage(Component.text(
                        "Panstwo moze miec maksymalnie 2 zastepcow.", NamedTextColor.YELLOW));
            }
            openMembers(player, returnPage);
        } catch (SQLException error) {
            reportError(player, "Nation member GUI action failed", error);
        }
    }

    private static String actionLabel(NationDatabase.MemberAction action) {
        return switch (action) {
            case PROMOTE -> "Awans na zastepce";
            case DEMOTE -> "Degradacja do czlonka";
            case KICK -> "Usuniecie z panstwa";
        };
    }

    private static String playerName(UUID uuid) {
        OfflinePlayer profile = Bukkit.getOfflinePlayer(uuid);
        return profile.getName() == null ? uuid.toString().substring(0, 8) : profile.getName();
    }

    private static boolean isManager(String role) {
        return "LEADER".equals(role) || "DEPUTY".equals(role);
    }

    public void openInvitations(Player player) {
        openInvites(player, 0);
    }

    private void openInvites(Player player, int requestedPage) {
        try {
            List<NationDatabase.NationInvite> invites = plugin.getNationDatabase()
                    .findInvites(player.getUniqueId());
            int page = clampPage(requestedPage, invites.size());
            List<String> pageNames = new ArrayList<>();
            MenuHolder holder;
            int from = page * PAGE_SIZE;
            for (int i = from; i < Math.min(from + PAGE_SIZE, invites.size()); i++) {
                pageNames.add(invites.get(i).nationName());
            }
            holder = new MenuHolder(player.getUniqueId(), Screen.INVITES, page,
                    plugin.getDisplayName() + " | Zaproszenia " + (page + 1), pageNames, null);
            Inventory inventory = holder.getInventory();
            for (int i = 0; i < pageNames.size(); i++) {
                inventory.setItem(i, item(Material.PAPER, pageNames.get(i), NamedTextColor.YELLOW,
                        "Kliknij, by przyjac lub odrzucic zaproszenie", "Zaproszenie wygasa po 24 godzinach"));
            }
            if (pageNames.isEmpty()) {
                inventory.setItem(22, item(Material.BOOK, "Brak zaproszen", NamedTextColor.GRAY));
            }
            navigation(inventory, page > 0, from + PAGE_SIZE < invites.size());
            player.openInventory(inventory);
        } catch (SQLException error) {
            reportError(player, "Opening nation invites failed", error);
        }
    }

    private void openInvitationDecision(Player player, String nationName) {
        // Revalidate: the invitation may expire between opening the list and clicking it.
        try {
            boolean active = plugin.getNationDatabase().findInvites(player.getUniqueId()).stream()
                    .anyMatch(invite -> invite.nationName().equalsIgnoreCase(nationName));
            if (!active) {
                player.sendMessage(Component.text("To zaproszenie nie jest juz aktywne.", NamedTextColor.YELLOW));
                openInvites(player, 0);
                return;
            }
            MenuHolder holder = new MenuHolder(player.getUniqueId(), Screen.CONFIRM_INVITE, 0,
                    plugin.getDisplayName() + " | " + nationName, List.of(), nationName);
            Inventory inventory = holder.getInventory();
            inventory.setItem(11, item(Material.LIME_WOOL, "Przyjmij", NamedTextColor.GREEN,
                    "Dolaczysz do panstwa: " + nationName));
            inventory.setItem(15, item(Material.RED_WOOL, "Odrzuc", NamedTextColor.RED,
                    "Odrzucisz zaproszenie: " + nationName));
            inventory.setItem(22, item(Material.ARROW, "Powrot", NamedTextColor.YELLOW));
            player.openInventory(inventory);
        } catch (SQLException error) {
            reportError(player, "Opening invite confirmation failed", error);
        }
    }

    /** Public for command routing and GUI chat prompt, always invoked on server thread. */
    public void createNation(Player player, String rawName) {
        String name = rawName.trim();
        if (!validNationName(name)) {
            player.sendMessage(Component.text("Nazwa: 3-32 znaki (litery, cyfry, spacje, _ lub -).", NamedTextColor.RED));
            return;
        }
        try {
            if (plugin.getNationDatabase().findMembership(player.getUniqueId()).isPresent()) {
                player.sendMessage(Component.text("Juz nalezysz do panstwa.", NamedTextColor.YELLOW));
            } else if (!plugin.getNationDatabase().createNation(name, player.getUniqueId())) {
                player.sendMessage(Component.text("Nazwa jest zajeta lub masz juz panstwo.", NamedTextColor.YELLOW));
            } else {
                player.sendMessage(Component.text("Utworzono panstwo " + name + ". Jestes liderem!", NamedTextColor.GREEN));
                player.sendMessage(Component.text("Odbierz darmowy rdzen: /esmp core free.", NamedTextColor.GRAY));
                plugin.getEventTitles().show(player, "PAŃSTWO UTWORZONE!", name, NamedTextColor.GREEN);
            }
        } catch (SQLException error) {
            reportError(player, "Nation creation failed", error);
        }
    }

    private static boolean validNationName(String name) {
        if (name.length() < 3 || name.length() > 32) return false;
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (!(Character.isLetterOrDigit(ch) || ch == ' ' || ch == '_' || ch == '-')) return false;
        }
        return true;
    }

    private void startCreatePrompt(Player player) {
        try {
            if (plugin.getNationDatabase().findMembership(player.getUniqueId()).isPresent()) {
                openMain(player);
                return;
            }
            awaitingName.put(player.getUniqueId(), System.nanoTime() + NAME_PROMPT_NANOS);
            player.closeInventory();
            player.sendMessage(Component.text("Wpisz nazwe nowego panstwa na czacie (3-32 znaki).", NamedTextColor.GREEN));
            player.sendMessage(Component.text("Wpisz 'anuluj', aby przerwac. Masz 2 minuty; wiadomosc nie bedzie publiczna.", NamedTextColor.YELLOW));
        } catch (SQLException error) {
            reportError(player, "Nation creation prompt failed", error);
        }
    }

    @EventHandler
    public void onNationNameChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        Long deadline = awaitingName.get(id);
        if (deadline == null) return;
        if (System.nanoTime() - deadline >= 0) {
            awaitingName.remove(id, deadline);
            return; // prompt expired; do not swallow unrelated chat
        }
        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        if (!awaitingName.remove(id, deadline)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (message.equalsIgnoreCase("anuluj") || message.equalsIgnoreCase("cancel")) {
                player.sendMessage(Component.text("Anulowano tworzenie panstwa.", NamedTextColor.YELLOW));
            } else {
                createNation(player, message);
            }
            openMain(player);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        awaitingName.remove(event.getPlayer().getUniqueId());
    }

    private void openInvitePlayers(Player player, int requestedPage) {
        try {
            Optional<NationDatabase.Membership> nation = plugin.getNationDatabase().findMembership(player.getUniqueId());
            if (nation.isEmpty() || !isManager(nation.get().role())) {
                openMain(player);
                return;
            }
            List<UUID> candidates = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.getUniqueId().equals(player.getUniqueId())
                        && plugin.getNationDatabase().findMembership(online.getUniqueId()).isEmpty()) {
                    candidates.add(online.getUniqueId());
                }
            }
            candidates.sort(Comparator.comparing(NationMenu::playerName, String.CASE_INSENSITIVE_ORDER));
            int page = clampPage(requestedPage, candidates.size());
            int from = page * PAGE_SIZE;
            List<UUID> pageIds = candidates.subList(from, Math.min(from + PAGE_SIZE, candidates.size()));
            MenuHolder holder = new MenuHolder(player.getUniqueId(), Screen.INVITE_PLAYERS, page,
                    plugin.getDisplayName() + " | Zapros gracza", List.of(), null, pageIds, null, null);
            Inventory inv = holder.getInventory();
            for (int i = 0; i < pageIds.size(); i++) {
                inv.setItem(i, item(Material.PLAYER_HEAD, playerName(pageIds.get(i)), NamedTextColor.GREEN,
                        "Kliknij, by wyslac zaproszenie"));
            }
            if (pageIds.isEmpty()) inv.setItem(22, item(Material.BOOK, "Brak graczy online bez panstwa", NamedTextColor.GRAY));
            navigation(inv, page > 0, from + PAGE_SIZE < candidates.size());
            player.openInventory(inv);
        } catch (SQLException error) {
            reportError(player, "Opening invitation player list failed", error);
        }
    }

    private void openInviteConfirmation(Player player, UUID targetUuid, int page) {
        try {
            Player target = Bukkit.getPlayer(targetUuid);
            Optional<NationDatabase.Membership> nation = plugin.getNationDatabase().findMembership(player.getUniqueId());
            if (target == null || !target.isOnline() || nation.isEmpty() || !isManager(nation.get().role())
                    || plugin.getNationDatabase().findMembership(targetUuid).isPresent()) {
                player.sendMessage(Component.text("Ten gracz nie jest juz dostepny do zaproszenia.", NamedTextColor.YELLOW));
                openInvitePlayers(player, page);
                return;
            }
            MenuHolder holder = new MenuHolder(player.getUniqueId(), Screen.CONFIRM_SEND_INVITE,
                    page, plugin.getDisplayName() + " | Zaproszenie", List.of(), null, List.of(), targetUuid, null);
            holder.getInventory().setItem(13, item(Material.PLAYER_HEAD, playerName(targetUuid), NamedTextColor.GOLD));
            holder.getInventory().setItem(11, item(Material.LIME_WOOL, "Wyslij zaproszenie", NamedTextColor.GREEN,
                    "Wazne 24 godziny"));
            holder.getInventory().setItem(15, item(Material.RED_WOOL, "Anuluj", NamedTextColor.RED));
            player.openInventory(holder.getInventory());
        } catch (SQLException error) {
            reportError(player, "Confirming invite failed", error);
        }
    }

    private void sendInvitation(Player player, UUID targetUuid, int page) {
        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null || !target.isOnline()) {
            player.sendMessage(Component.text("Gracz jest juz offline.", NamedTextColor.YELLOW));
            openInvitePlayers(player, page);
            return;
        }
        try {
            NationDatabase.InviteResult result = plugin.getNationDatabase().invite(player.getUniqueId(), targetUuid);
            switch (result) {
                case SENT -> {
                    NationDatabase.Membership nation = plugin.getNationDatabase().findMembership(player.getUniqueId()).orElseThrow();
                    player.sendMessage(Component.text("Zaproszenie wyslane do " + target.getName() + ".", NamedTextColor.GREEN));
                    plugin.getEventTitles().show(target, "ZAPROSZENIE!", nation.nationName(), NamedTextColor.GOLD);
                    target.sendMessage(Component.text("Zaproszenie do panstwa " + nation.nationName()
                            + "! Otworz /earthsmp -> Zaproszenia", NamedTextColor.GREEN));
                }
                case NOT_AUTHORIZED -> player.sendMessage(Component.text("Nie masz uprawnien.", NamedTextColor.RED));
                case TARGET_ALREADY_MEMBER -> player.sendMessage(Component.text("Gracz ma juz panstwo.", NamedTextColor.YELLOW));
                case SELF_INVITE -> player.sendMessage(Component.text("Nie mozesz zaprosic siebie.", NamedTextColor.YELLOW));
            }
            openMain(player);
        } catch (SQLException error) {
            reportError(player, "Sending GUI invitation failed", error);
        }
    }

    private void openOutgoingInvites(Player player, int requestedPage) {
        try {
            List<NationDatabase.OutgoingInvite> invites = plugin.getNationDatabase()
                    .findOutgoingInvites(player.getUniqueId());
            int page = clampPage(requestedPage, invites.size());
            int from = page * PAGE_SIZE;
            List<UUID> pageIds = new ArrayList<>();
            for (int i = from; i < Math.min(from + PAGE_SIZE, invites.size()); i++) {
                pageIds.add(invites.get(i).targetUuid());
            }
            MenuHolder holder = new MenuHolder(player.getUniqueId(), Screen.OUTGOING_INVITES, page,
                    plugin.getDisplayName() + " | Wyslane zaproszenia", List.of(), null, pageIds, null, null);
            Inventory inv = holder.getInventory();
            for (int i = 0; i < pageIds.size(); i++) {
                inv.setItem(i, item(Material.PAPER, playerName(pageIds.get(i)), NamedTextColor.YELLOW,
                        "Kliknij, by wycofac zaproszenie"));
            }
            if (pageIds.isEmpty()) inv.setItem(22, item(Material.BOOK, "Brak wyslanych zaproszen", NamedTextColor.GRAY));
            navigation(inv, page > 0, from + PAGE_SIZE < invites.size());
            player.openInventory(inv);
        } catch (SQLException error) {
            reportError(player, "Opening outgoing invitations failed", error);
        }
    }

    private void openWithdrawConfirmation(Player player, UUID targetUuid, int page) {
        MenuHolder holder = new MenuHolder(player.getUniqueId(), Screen.CONFIRM_WITHDRAW, page,
                plugin.getDisplayName() + " | Wycofanie zaproszenia", List.of(), null, List.of(), targetUuid, null);
        holder.getInventory().setItem(13, item(Material.PAPER, playerName(targetUuid), NamedTextColor.YELLOW));
        holder.getInventory().setItem(11, item(Material.RED_WOOL, "Wycofaj", NamedTextColor.RED));
        holder.getInventory().setItem(15, item(Material.LIME_WOOL, "Anuluj", NamedTextColor.GREEN));
        player.openInventory(holder.getInventory());
    }

    private void withdrawInvitation(Player player, UUID targetUuid, int page) {
        try {
            NationDatabase.WithdrawResult result = plugin.getNationDatabase()
                    .withdrawInvite(player.getUniqueId(), targetUuid);
            switch (result) {
                case REMOVED -> player.sendMessage(Component.text("Wycofano zaproszenie.", NamedTextColor.GREEN));
                case NOT_AUTHORIZED -> player.sendMessage(Component.text("Brak uprawnien.", NamedTextColor.RED));
                case NOT_FOUND -> player.sendMessage(Component.text("Zaproszenie nie jest juz aktywne.", NamedTextColor.YELLOW));
            }
            openOutgoingInvites(player, page);
        } catch (SQLException error) {
            reportError(player, "Withdrawing GUI invitation failed", error);
        }
    }

    public void openDisbandConfirmation(Player player) {
        try {
            Optional<NationDatabase.Membership> m = plugin.getNationDatabase().findMembership(player.getUniqueId());
            if (m.isEmpty() || !"LEADER".equals(m.get().role())) {
                player.sendMessage(Component.text("Tylko lider moze rozwiazac panstwo.", NamedTextColor.RED));
                return;
            }
            if (plugin.getNationDatabase().nationAtWar(m.get().nationId())) {
                player.sendMessage(Component.text("Nie mozna rozwiazac panstwa podczas wojny.", NamedTextColor.RED));
                return;
            }
            long count = plugin.getNationDatabase().allProtectionCores().stream()
                    .filter(c -> c.nationId().equals(m.get().nationId())).count();
            MenuHolder h = new MenuHolder(player.getUniqueId(), Screen.CONFIRM_DISBAND, 0,
                    plugin.getDisplayName() + " | Rozwiaz panstwo", List.of(), null);
            h.getInventory().setItem(13, item(Material.BARRIER, "USUN: " + m.get().nationName(),
                    NamedTextColor.RED, "Zniknie " + count + " rdzeni (takze zdobyte)",
                    "Terytoria stana sie neutralne; czlonkowie straca przynaleznosc",
                    "Budowle pozostana na miejscu", "NIEODWRACALNE"));
            h.getInventory().setItem(11, item(Material.RED_WOOL, "Potwierdz rozwiazanie", NamedTextColor.RED));
            h.getInventory().setItem(15, item(Material.LIME_WOOL, "Anuluj", NamedTextColor.GREEN));
            player.openInventory(h.getInventory());
        } catch (SQLException error) { reportError(player, "Opening disband confirmation", error); }
    }

    private void performDisband(Player player) {
        try {
            var member = plugin.getNationDatabase().findMembership(player.getUniqueId());
            if (member.isEmpty() || !"LEADER".equals(member.get().role())) {
                player.sendMessage(Component.text("Tylko lider moze rozwiazac panstwo.", NamedTextColor.RED));
                openMain(player); return;
            }
            if (plugin.getNationDatabase().nationAtWar(member.get().nationId())) {
                player.sendMessage(Component.text("Nie mozna rozwiazac panstwa podczas wojny.", NamedTextColor.RED));
                openMain(player); return;
            }
            List<Block> anchors = new ArrayList<>();
            for (var core : plugin.getNationDatabase().allProtectionCores()) {
                if (!core.nationId().equals(member.get().nationId())) continue;
                World world = Bukkit.getWorld(core.world());
                if (world == null) {
                    player.sendMessage(Component.text("Swiat rdzenia nie jest zaladowany. Operacja przerwana.", NamedTextColor.RED));
                    return;
                }
                Block anchor = world.getBlockAt(core.x(), core.y(), core.z());
                if (anchor.getType() != Material.LODESTONE) {
                    player.sendMessage(Component.text("Brak lodestone rdzenia " + core.id(), NamedTextColor.RED));
                    return;
                }
                anchors.add(anchor);
            }
            try {
                for (Block anchor : anchors) anchor.setType(Material.AIR, false);
                var result = plugin.getNationDatabase().disbandNation(player.getUniqueId());
                if (result != NationDatabase.DisbandResult.DISBANDED) {
                    for (Block anchor : anchors) anchor.setType(Material.LODESTONE, false);
                    player.sendMessage(Component.text("Nie mozna rozwiazac panstwa.", NamedTextColor.RED));
                    openMain(player); return;
                }
            } catch (SQLException | RuntimeException failure) {
                for (Block anchor : anchors) {
                    try { anchor.setType(Material.LODESTONE, false); }
                    catch (RuntimeException e) { failure.addSuppressed(e); }
                }
                throw failure;
            }
            plugin.getCoreProtection().reload();
            plugin.refreshTerritoryMap();
            plugin.refreshCoreMap();
            player.sendMessage(Component.text("Panstwo rozwiazane; usunieto " + anchors.size()
                    + " rdzeni. Zwykle budowle pozostaly.", NamedTextColor.GREEN));
            plugin.getEventTitles().show(player, "PAŃSTWO ROZWIĄZANE", "Usunięto " + anchors.size() + " rdzeni", NamedTextColor.RED);
            openMain(player);
        } catch (SQLException error) {
            reportError(player, "Disband nation failed", error);
        } catch (RuntimeException error) {
            plugin.getLogger().log(Level.SEVERE, "Disband nation failed", error);
            player.sendMessage(Component.text("Blad rozwiazania panstwa, sprawdz konsole i backup.", NamedTextColor.RED));
        }
    }

    private void openLeaveConfirmation(Player player) {
        try {
            Optional<NationDatabase.Membership> membership = plugin.getNationDatabase().findMembership(player.getUniqueId());
            if (membership.isEmpty() || "LEADER".equals(membership.get().role())) {
                player.sendMessage(Component.text("Lider musi najpierw przekazac przywodztwo.", NamedTextColor.YELLOW));
                openMain(player);
                return;
            }
            MenuHolder holder = new MenuHolder(player.getUniqueId(), Screen.CONFIRM_LEAVE, 0,
                    plugin.getDisplayName() + " | Opusc panstwo", List.of(), null);
            holder.getInventory().setItem(13, item(Material.WHITE_BANNER, membership.get().nationName(), NamedTextColor.GOLD));
            holder.getInventory().setItem(11, item(Material.RED_WOOL, "Tak, opuszczam", NamedTextColor.RED));
            holder.getInventory().setItem(15, item(Material.LIME_WOOL, "Anuluj", NamedTextColor.GREEN));
            player.openInventory(holder.getInventory());
        } catch (SQLException error) {
            reportError(player, "Opening leave nation confirmation failed", error);
        }
    }

    private void performLeave(Player player) {
        try {
            NationDatabase.LeaveResult result = plugin.getNationDatabase().leaveNation(player.getUniqueId());
            switch (result) {
                case LEFT -> player.sendMessage(Component.text("Opusciles panstwo.", NamedTextColor.GREEN));
                case NOT_MEMBER -> player.sendMessage(Component.text("Nie nalezysz do panstwa.", NamedTextColor.YELLOW));
                case LEADER_MUST_TRANSFER -> player.sendMessage(Component.text("Lider nie moze opuscic panstwa bez przekazania wladzy.", NamedTextColor.RED));
            }
            openMain(player);
        } catch (SQLException error) {
            reportError(player, "Leaving nation from GUI failed", error);
        }
    }

    public void openTransferConfirmation(Player player, UUID targetUuid, int returnPage) {
        try {
            Optional<NationDatabase.Membership> actor = plugin.getNationDatabase().findMembership(player.getUniqueId());
            Optional<NationDatabase.Membership> target = plugin.getNationDatabase().findMembership(targetUuid);
            if (actor.isEmpty() || !"LEADER".equals(actor.get().role())
                    || target.isEmpty() || !actor.get().nationId().equals(target.get().nationId())
                    || targetUuid.equals(player.getUniqueId())) {
                player.sendMessage(Component.text("Nie mozna przekazac przywodztwa temu graczowi.", NamedTextColor.YELLOW));
                openMain(player);
                return;
            }
            MenuHolder holder = new MenuHolder(player.getUniqueId(), Screen.CONFIRM_TRANSFER, returnPage,
                    plugin.getDisplayName() + " | Nowy lider", List.of(), null, List.of(), targetUuid, null);
            holder.getInventory().setItem(13, item(Material.PLAYER_HEAD, playerName(targetUuid), NamedTextColor.GOLD,
                    "Po zmianie bedziesz zwyklym czlonkiem"));
            holder.getInventory().setItem(11, item(Material.LIME_WOOL, "Przekaz przywodztwo", NamedTextColor.GREEN));
            holder.getInventory().setItem(15, item(Material.RED_WOOL, "Anuluj", NamedTextColor.RED));
            player.openInventory(holder.getInventory());
        } catch (SQLException error) {
            reportError(player, "Opening transfer confirmation failed", error);
        }
    }

    private void performTransfer(Player player, UUID targetUuid) {
        try {
            NationDatabase.TransferResult result = plugin.getNationDatabase()
                    .transferLeadership(player.getUniqueId(), targetUuid);
            switch (result) {
                case TRANSFERRED -> {
                    player.sendMessage(Component.text("Nowy lider: " + playerName(targetUuid), NamedTextColor.GREEN));
                    plugin.getEventTitles().show(player, "PRZEKAZANO WŁADZĘ", playerName(targetUuid), NamedTextColor.GOLD);
                    Player onlineTarget = Bukkit.getPlayer(targetUuid);
                    if (onlineTarget != null) {
                        onlineTarget.sendMessage(Component.text("Zostales nowym liderem panstwa!", NamedTextColor.GREEN));
                        plugin.getEventTitles().show(onlineTarget, "NOWY LIDER!", "Przejąłeś przywództwo", NamedTextColor.GREEN);
                    }
                }
                case NOT_LEADER -> player.sendMessage(Component.text("Nie jestes juz liderem.", NamedTextColor.RED));
                case INVALID_TARGET -> player.sendMessage(Component.text("Nieprawidlowy nowy lider.", NamedTextColor.YELLOW));
            }
            openMain(player);
        } catch (SQLException error) {
            reportError(player, "Leadership transfer failed", error);
        }
    }

    private static void navigation(Inventory inventory, boolean prev, boolean next) {
        if (prev) inventory.setItem(45, item(Material.ARROW, "Poprzednia strona", NamedTextColor.YELLOW));
        inventory.setItem(49, item(Material.OAK_DOOR, "Menu glowne", NamedTextColor.GREEN));
        if (next) inventory.setItem(53, item(Material.ARROW, "Nastepna strona", NamedTextColor.YELLOW));
    }

    private static int clampPage(int wanted, int count) {
        return Math.max(0, Math.min(wanted, Math.max(0, (count - 1) / PAGE_SIZE)));
    }

    private static NamedTextColor roleColor(String role) {
        return switch (role) {
            case "LEADER" -> NamedTextColor.GOLD;
            case "DEPUTY" -> NamedTextColor.AQUA;
            default -> NamedTextColor.WHITE;
        };
    }

    private static ItemStack item(Material material, String name, NamedTextColor color, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color));
        List<Component> lines = new ArrayList<>();
        for (String text : lore) lines.add(Component.text(text, NamedTextColor.GRAY));
        meta.lore(lines);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof MenuHolder menu)) return;
        // Always prevent moving items into/out of these inventories, including shift-clicks.
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!player.getUniqueId().equals(menu.viewerId)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= menu.getInventory().getSize()) return;

        // InventoryClickEvent must not open/close an inventory directly.
        Runnable next = switch (menu.screen) {
            case MAIN -> switch (slot) {
                case 11 -> () -> {
                    try {
                        if (plugin.getNationDatabase().findMembership(player.getUniqueId()).isEmpty()) startCreatePrompt(player);
                    } catch (SQLException error) { reportError(player, "Checking creation eligibility failed", error); }
                };
                case 13 -> () -> openMembers(player, 0);
                case 15 -> () -> openInvites(player, 0);
                case 18 -> () -> openInvitePlayers(player, 0);
                case 24 -> () -> openOutgoingInvites(player, 0);
                case 26 -> () -> {
                    try {
                        var m = plugin.getNationDatabase().findMembership(player.getUniqueId());
                        if (m.isPresent() && "LEADER".equals(m.get().role())) openDisbandConfirmation(player);
                        else openLeaveConfirmation(player);
                    } catch (SQLException e) { reportError(player, "Opening exit GUI", e); }
                };
                case 22 -> () -> plugin.getHubMenu().openMain(player);
                default -> null;
            };
            case MEMBERS -> {
                if (slot < menu.memberIds.size()) {
                    UUID targetUuid = menu.memberIds.get(slot);
                    yield () -> openMemberDetails(player, targetUuid, menu.page);
                }
                yield switch (slot) {
                    case 45 -> menu.page > 0 ? () -> openMembers(player, menu.page - 1) : null;
                    case 49 -> () -> openMain(player);
                    case 53 -> () -> openMembers(player, menu.page + 1);
                    default -> null;
                };
            }
            case MEMBER_DETAILS -> switch (slot) {
                case 4 -> () -> openTransferConfirmation(player, menu.selectedMember, menu.page);
                case 10 -> () -> openMemberConfirmation(player, menu.selectedMember, menu.page,
                        NationDatabase.MemberAction.PROMOTE);
                case 16 -> () -> openMemberConfirmation(player, menu.selectedMember, menu.page,
                        NationDatabase.MemberAction.DEMOTE);
                case 20 -> () -> openMemberConfirmation(player, menu.selectedMember, menu.page,
                        NationDatabase.MemberAction.KICK);
                case 22 -> () -> openMembers(player, menu.page);
                default -> null;
            };
            case CONFIRM_MEMBER_ACTION -> switch (slot) {
                case 11 -> () -> performMemberAction(player, menu.selectedMember, menu.page, menu.selectedAction);
                case 15, 22 -> () -> openMemberDetails(player, menu.selectedMember, menu.page);
                default -> null;
            };
            case INVITES -> {
                if (slot < menu.invitationNames.size()) {
                    String nationName = menu.invitationNames.get(slot);
                    yield () -> openInvitationDecision(player, nationName);
                }
                yield switch (slot) {
                    case 45 -> menu.page > 0 ? () -> openInvites(player, menu.page - 1) : null;
                    case 49 -> () -> openMain(player);
                    case 53 -> () -> openInvites(player, menu.page + 1);
                    default -> null;
                };
            }
            case CONFIRM_INVITE -> switch (slot) {
                case 11 -> () -> respondToInvite(player, menu.selectedNation, true);
                case 15 -> () -> respondToInvite(player, menu.selectedNation, false);
                case 22 -> () -> openInvites(player, 0);
                default -> null;
            };
            case INVITE_PLAYERS -> {
                if (slot < menu.memberIds.size()) {
                    UUID target = menu.memberIds.get(slot);
                    yield () -> openInviteConfirmation(player, target, menu.page);
                }
                yield switch (slot) {
                    case 45 -> menu.page > 0 ? () -> openInvitePlayers(player, menu.page - 1) : null;
                    case 49 -> () -> openMain(player);
                    case 53 -> () -> openInvitePlayers(player, menu.page + 1);
                    default -> null;
                };
            }
            case CONFIRM_SEND_INVITE -> switch (slot) {
                case 11 -> () -> sendInvitation(player, menu.selectedMember, menu.page);
                case 15 -> () -> openInvitePlayers(player, menu.page);
                default -> null;
            };
            case OUTGOING_INVITES -> {
                if (slot < menu.memberIds.size()) {
                    UUID target = menu.memberIds.get(slot);
                    yield () -> openWithdrawConfirmation(player, target, menu.page);
                }
                yield switch (slot) {
                    case 45 -> menu.page > 0 ? () -> openOutgoingInvites(player, menu.page - 1) : null;
                    case 49 -> () -> openMain(player);
                    case 53 -> () -> openOutgoingInvites(player, menu.page + 1);
                    default -> null;
                };
            }
            case CONFIRM_WITHDRAW -> switch (slot) {
                case 11 -> () -> withdrawInvitation(player, menu.selectedMember, menu.page);
                case 15 -> () -> openOutgoingInvites(player, menu.page);
                default -> null;
            };
            case CONFIRM_LEAVE -> switch (slot) {
                case 11 -> () -> performLeave(player);
                case 15 -> () -> openMain(player);
                default -> null;
            };
            case CONFIRM_DISBAND -> switch (slot) {
                case 11 -> () -> performDisband(player);
                case 15 -> () -> openMain(player);
                default -> null;
            };
            case CONFIRM_TRANSFER -> switch (slot) {
                case 11 -> () -> performTransfer(player, menu.selectedMember);
                case 15 -> () -> openMemberDetails(player, menu.selectedMember, menu.page);
                default -> null;
            };
        };
        // Ignore a queued click if its menu has already been replaced/closed.
        if (next != null) plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && player.getOpenInventory().getTopInventory() == menu.getInventory()) {
                next.run();
            }
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof MenuHolder)) return;
        if (event.getRawSlots().stream().anyMatch(slot -> slot < event.getView().getTopInventory().getSize())) {
            event.setCancelled(true);
        }
    }

    private void respondToInvite(Player player, String nation, boolean accept) {
        try {
            if (accept) {
                NationDatabase.AcceptResult result = plugin.getNationDatabase()
                        .acceptInvite(player.getUniqueId(), nation);
                switch (result) {
                    case JOINED -> player.sendMessage(Component.text(
                            "Dolaczono do panstwa: " + nation, NamedTextColor.GREEN));
                    case ALREADY_MEMBER -> player.sendMessage(Component.text(
                            "Juz nalezysz do panstwa.", NamedTextColor.YELLOW));
                    case NO_ACTIVE_INVITE -> player.sendMessage(Component.text(
                            "Zaproszenie wygaslo lub zostalo wycofane.", NamedTextColor.YELLOW));
                }
                openMain(player);
            } else {
                boolean removed = plugin.getNationDatabase().declineInvite(player.getUniqueId(), nation);
                player.sendMessage(Component.text(removed ? "Odrzucono zaproszenie: " + nation
                        : "Zaproszenie nie jest juz aktywne.", NamedTextColor.YELLOW));
                openInvites(player, 0);
            }
        } catch (SQLException error) {
            reportError(player, "Nation invite GUI action failed", error);
        }
    }

    private void reportError(Player player, String context, SQLException error) {
        plugin.getLogger().log(Level.SEVERE, context, error);
        player.sendMessage(Component.text("Blad bazy danych. Sprawdz konsole serwera.", NamedTextColor.RED));
    }
}
