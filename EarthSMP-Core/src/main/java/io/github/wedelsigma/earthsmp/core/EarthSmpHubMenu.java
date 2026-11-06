package io.github.wedelsigma.earthsmp.core;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

/**
 * One entry point for every player feature: /esmp.
 * This layer deliberately delegates writes to the existing command implementations,
 * which validate permissions and selection bounds themselves.
 */
public final class EarthSmpHubMenu implements Listener {
    private static final int PAGE_SIZE = 45;
    private final EarthSmpCorePlugin plugin;

    private enum Screen {
        HUB, TERRITORY, TERRITORY_CONFIRM_ADD, TERRITORY_CONFIRM_REMOVE,
        TERRITORY_OWN_REQUESTS, TERRITORY_CANCEL, TERRITORY_ADMIN_REQUESTS,
        TERRITORY_ADMIN_REVIEW, CORES, CORE_CONFIRM_FREE, CORE_CONFIRM_PICKUP,
        CORE_CONFIRM_CAPITAL, DIPLOMACY, HISTORY, SETTINGS
    }

    private static final class Holder implements InventoryHolder {
        final UUID viewer;
        final Screen screen;
        final int page;
        final long requestId;
        List<Long> requestIds = List.of();
        final Inventory inventory;

        Holder(Player player, Screen screen, int page, long requestId, String title) {
            this.viewer = player.getUniqueId();
            this.screen = screen;
            this.page = page;
            this.requestId = requestId;
            this.inventory = Bukkit.createInventory(this, 54, Component.text(title, NamedTextColor.GOLD));
        }

        @Override public @NotNull Inventory getInventory() { return inventory; }
    }

    public EarthSmpHubMenu(EarthSmpCorePlugin plugin) { this.plugin = plugin; }

    public void openMain(Player player) {
        try {
            Optional<NationDatabase.Membership> membership = plugin.getNationDatabase().findMembership(player.getUniqueId());
            int invites = plugin.getNationDatabase().findInvites(player.getUniqueId()).size();
            Holder h = new Holder(player, Screen.HUB, 0, 0L, "EarthSMP | Centrum");
            Inventory inv = h.inventory;
            if (membership.isPresent()) {
                var own = membership.get();
                inv.setItem(10, item(Material.WHITE_BANNER, own.nationName(), NamedTextColor.GOLD,
                        "Twoja ranga: " + own.role(), "Dostep do wszystkich systemow panstwa"));
            } else {
                inv.setItem(10, item(Material.GRAY_BANNER, "Nie nalezysz do panstwa", NamedTextColor.YELLOW,
                        "Otworz Panstwo, by zalozyc kraj", "lub przyjac zaproszenie"));
            }
            inv.setItem(12, item(Material.PLAYER_HEAD, "Panstwo", NamedTextColor.GREEN,
                    "Czlonkowie, role, zaproszenia", "Zarzadzanie i tworzenie kraju"));
            inv.setItem(14, item(Material.FILLED_MAP, "Terytoria", NamedTextColor.AQUA,
                    "Zaznaczanie chunkow i wnioski", "Zatwierdzanie granic przez admina"));
            inv.setItem(16, item(Material.LODESTONE, "Rdzenie", NamedTextColor.YELLOW,
                    "Ochrona 65x65, stan rdzeni", "Darmowy rdzen i przenoszenie"));
            inv.setItem(28, item(Material.PAPER, "Zaproszenia: " + invites, NamedTextColor.GREEN,
                    "Otworz otrzymane zaproszenia"));
            inv.setItem(30, item(Material.IRON_SWORD, "Dyplomacja", NamedTextColor.GOLD,
                    "Wypowiadaj wojny i zapraszaj sojusznikow", "Propozycje pokoju oraz lista konfliktow"));
            inv.setItem(32, item(Material.IRON_PICKAXE, "Oblezenia", NamedTextColor.GOLD,
                    "Godziny 10:00-00:00 i prog 20%", "Rozpocznij lub sprawdz legalnosc oblezenia"));
            inv.setItem(34, item(Material.COMPARATOR, "Ustawienia", NamedTextColor.GRAY,
                    "Konfiguracja panstwa: jeszcze niedostepna"));
            inv.setItem(49, item(Material.BARRIER, "Zamknij", NamedTextColor.RED));
            player.openInventory(inv);
        } catch (SQLException e) { error(player, "Opening main EarthSMP hub", e); }
    }

    private void openTerritory(Player player) {
        try {
            var membership = plugin.getNationDatabase().findMembership(player.getUniqueId());
            boolean manager = membership.isPresent() && isManager(membership.get().role());
            boolean admin = player.hasPermission("earthsmp.admin");
            var ownRequests = plugin.getNationDatabase().findOwnPendingTerritoryRequests(player.getUniqueId());
            Holder h = new Holder(player, Screen.TERRITORY, 0, 0L, "EarthSMP | Terytoria");
            Inventory inv = h.inventory;
            inv.setItem(10, item(Material.COMPASS, "Sprawdz chunk", NamedTextColor.GREEN,
                    "Pokazuje wlasciciela chunku", "na ktorym aktualnie stoisz"));
            inv.setItem(12, item(Material.RED_BANNER, "Rog 1 - tutaj", NamedTextColor.YELLOW,
                    "Zapisz pierwszy rog zaznaczenia", "Musisz byc w swiecie Earth"));
            inv.setItem(14, item(Material.LIME_BANNER, "Rog 2 - tutaj", NamedTextColor.YELLOW,
                    "Zapisz drugi rog zaznaczenia", "Nastepnie wybierz rodzaj wniosku"));
            inv.setItem(16, item(Material.MAP, "Podsumowanie zaznaczenia", NamedTextColor.AQUA,
                    "Wyswietl wspolrzedne w czacie", "Maksymalnie 10000 chunkow"));
            inv.setItem(28, item(manager ? Material.LIME_WOOL : Material.GRAY_DYE,
                    "Wniosek: dolacz teren", manager ? NamedTextColor.GREEN : NamedTextColor.GRAY,
                    manager ? "Wymaga akceptacji admina" : "Tylko lider lub zastepca"));
            inv.setItem(30, item(manager ? Material.RED_WOOL : Material.GRAY_DYE,
                    "Wniosek: oddaj teren", manager ? NamedTextColor.YELLOW : NamedTextColor.GRAY,
                    manager ? "Wymaga akceptacji admina" : "Tylko lider lub zastepca"));
            inv.setItem(32, item(Material.PAPER, "Wnioski kraju: " + ownRequests.size(), NamedTextColor.AQUA,
                    "Podglad i anulowanie wnioskow"));
            if (admin) {
                int pending = plugin.getNationDatabase().findPendingTerritoryRequests().size();
                inv.setItem(34, item(Material.WRITABLE_BOOK, "Panel admina: " + pending, NamedTextColor.GOLD,
                        "Zatwierdzaj lub odrzucaj wnioski"));
            }
            inv.setItem(49, back());
            player.openInventory(inv);
        } catch (SQLException e) { error(player, "Opening territory GUI", e); }
    }

    private void territoryConfirm(Player player, boolean add) {
        try {
            var own = plugin.getNationDatabase().findMembership(player.getUniqueId());
            if (own.isEmpty() || !isManager(own.get().role())) {
                warn(player, "Tylko lider lub zastepca moze skladac wnioski.");
                openTerritory(player);
                return;
            }
            Holder h = new Holder(player, add ? Screen.TERRITORY_CONFIRM_ADD : Screen.TERRITORY_CONFIRM_REMOVE,
                    0, 0L, "EarthSMP | Potwierdz wniosek");
            h.inventory.setItem(13, item(add ? Material.LIME_WOOL : Material.RED_WOOL,
                    add ? "Dolacz zaznaczony teren" : "Oddaj zaznaczony teren", NamedTextColor.YELLOW,
                    "Zaznaczenie musi miec dwa rogi", "Administrator podejmie decyzje"));
            h.inventory.setItem(11, confirm());
            h.inventory.setItem(15, cancel());
            h.inventory.setItem(49, back());
            player.openInventory(h.inventory);
        } catch (SQLException e) { error(player, "Opening territory confirmation", e); }
    }

    private void openTerritoryRequests(Player player, boolean admin, int requestedPage) {
        if (admin && !player.hasPermission("earthsmp.admin")) {
            warn(player, "Ten ekran jest dostepny tylko dla admina.");
            openTerritory(player);
            return;
        }
        try {
            List<NationDatabase.TerritoryRequest> requests = admin
                    ? plugin.getNationDatabase().findPendingTerritoryRequests()
                    : plugin.getNationDatabase().findOwnPendingTerritoryRequests(player.getUniqueId());
            int page = clamp(requestedPage, requests.size());
            Holder h = new Holder(player, admin ? Screen.TERRITORY_ADMIN_REQUESTS : Screen.TERRITORY_OWN_REQUESTS,
                    page, 0L, admin ? "EarthSMP | Wnioski admin" : "EarthSMP | Moje wnioski");
            int offset = page * PAGE_SIZE;
            List<Long> visibleIds = new ArrayList<>();
            for (int i = 0; i < PAGE_SIZE && offset + i < requests.size(); i++) {
                var r = requests.get(offset + i);
                visibleIds.add(r.id());
                h.inventory.setItem(i, item("ADD".equals(r.action()) ? Material.LIME_BANNER : Material.RED_BANNER,
                        "#" + r.id() + " | " + r.nationName(), NamedTextColor.YELLOW,
                        r.action() + " | " + r.size() + " chunkow",
                        "X " + r.minX() + ".." + r.maxX(), "Z " + r.minZ() + ".." + r.maxZ(),
                        admin ? "Kliknij: zatwierdz / odrzuc" : "Kliknij: anuluj wniosek"));
            }
            h.requestIds = List.copyOf(visibleIds);
            navigation(h.inventory, page, requests.size());
            player.openInventory(h.inventory);
        } catch (SQLException e) { error(player, "Opening territory requests", e); }
    }

    /** Use the ID from the original displayed slot; never reindex a changing DB list. */
    private static Long displayedRequestId(Holder holder, int slot) {
        return slot >= 0 && slot < holder.requestIds.size() ? holder.requestIds.get(slot) : null;
    }

    private void requestDetails(Player player, long requestId, boolean admin, int returnPage) {
        if (admin && !player.hasPermission("earthsmp.admin")) {
            warn(player, "Brak uprawnien administratora.");
            openTerritory(player);
            return;
        }
        try {
            List<NationDatabase.TerritoryRequest> requests = admin
                    ? plugin.getNationDatabase().findPendingTerritoryRequests()
                    : plugin.getNationDatabase().findOwnPendingTerritoryRequests(player.getUniqueId());
            var found = requests.stream().filter(r -> r.id() == requestId).findFirst();
            if (found.isEmpty()) {
                warn(player, "Wniosek nie jest juz dostepny.");
                openTerritoryRequests(player, admin, returnPage);
                return;
            }
            var r = found.get();
            Holder h = new Holder(player, admin ? Screen.TERRITORY_ADMIN_REVIEW : Screen.TERRITORY_CANCEL,
                    returnPage, requestId, "EarthSMP | Wniosek #" + requestId);
            h.inventory.setItem(13, item(Material.FILLED_MAP, r.nationName(), NamedTextColor.GOLD,
                    r.action() + " | " + r.size() + " chunkow", "X " + r.minX() + ".." + r.maxX(),
                    "Z " + r.minZ() + ".." + r.maxZ()));
            if (admin) {
                h.inventory.setItem(11, item(Material.LIME_WOOL, "Zatwierdz", NamedTextColor.GREEN,
                        "Zmieni rzeczywiste granice i mape"));
                h.inventory.setItem(15, item(Material.RED_WOOL, "Odrzuc", NamedTextColor.RED,
                        "Nie zmienia terytorium"));
            } else {
                h.inventory.setItem(11, item(Material.RED_WOOL, "Anuluj wniosek", NamedTextColor.RED));
                h.inventory.setItem(15, item(Material.GRAY_DYE, "Zachowaj wniosek", NamedTextColor.YELLOW));
            }
            h.inventory.setItem(49, back());
            player.openInventory(h.inventory);
        } catch (SQLException e) { error(player, "Opening territory request decision", e); }
    }

    private void openCores(Player player) {
        try {
            var member = plugin.getNationDatabase().findMembership(player.getUniqueId());
            var stock = plugin.getNationDatabase().coreStock(player.getUniqueId());
            boolean manager = member.isPresent() && isManager(member.get().role());
            boolean admin = player.hasPermission("earthsmp.admin");
            Holder h = new Holder(player, Screen.CORES, 0, 0L, "EarthSMP | Rdzenie");
            Inventory inv = h.inventory;
            inv.setItem(10, item(Material.COMPASS, "Czy jestem chroniony?", NamedTextColor.AQUA,
                    "Sprawdz ochrone aktualnej pozycji"));
            inv.setItem(12, item(Material.NETHER_STAR, "Stan rdzeni", NamedTextColor.GREEN,
                    "Darmowy: " + stock.free() + "/1", "Wykonane: " + stock.crafted() + "/2",
                    "Postawione: " + stock.placed(),
                    "Lacznie kontrolowane: " + stock.total()));
            inv.setItem(14, item(manager ? Material.LODESTONE : Material.GRAY_DYE,
                    "Odbierz darmowy rdzen", manager ? NamedTextColor.GREEN : NamedTextColor.GRAY,
                    "Tylko lider lub zastepca", "Jednorazowo na panstwo", "Bezpieczne potwierdzenie"));
            inv.setItem(16, item(Material.BOOK, "Crafting", NamedTextColor.YELLOW,
                    "Srodek: 1 jukebox", "Po bokach: 64x kazdego surowca",
                    "Wegiel, zelazo, miedz, redstone", "Lapis, zloto, diament, ametyst"));
            inv.setItem(28, item(manager ? Material.IRON_PICKAXE : Material.GRAY_DYE,
                    "Podnies wlasny rdzen", manager ? NamedTextColor.YELLOW : NamedTextColor.GRAY,
                    "Patrz na lodestone do 6 blokow", "Nie dotyczy stolicy i podbitych", "Wymaga wolnego slotu"));
            if (admin) {
                inv.setItem(30, item(Material.SPYGLASS, "Admin: audyt strefy 9x9", NamedTextColor.GOLD,
                        "Patrz na lodestone rdzenia"));
            }
            if (manager || admin) {
                inv.setItem(32, item(Material.GOLD_BLOCK, "Ustaw stolice", NamedTextColor.GOLD,
                        "Lider/zastepca: wlasne rdzenie, podczas pokoju",
                        "Admin: takze korekta innych panstw", "Wymaga potwierdzenia"));
            }
            inv.setItem(49, back());
            player.openInventory(inv);
        } catch (SQLException e) { error(player, "Opening protection-core GUI", e); }
    }

    private void coreConfirm(Player player, Screen action) {
        try {
            boolean manager = plugin.getNationDatabase().findMembership(player.getUniqueId())
                    .map(m -> isManager(m.role())).orElse(false);
            if (!manager && !(action == Screen.CORE_CONFIRM_CAPITAL && player.hasPermission("earthsmp.admin"))) {
                warn(player, "Tylko lider lub zastepca moze wykonac te operacje.");
                return;
            }
            String description = switch (action) {
                case CORE_CONFIRM_FREE -> "Odbierz jedyny darmowy rdzen panstwa";
                case CORE_CONFIRM_PICKUP -> "Podnies rdzen, na ktory patrzysz";
                case CORE_CONFIRM_CAPITAL -> "Zmien stolice na wskazany rdzen";
                default -> throw new IllegalArgumentException("Invalid core action");
            };
            Holder h = new Holder(player, action, 0, 0L, "EarthSMP | Potwierdz rdzen");
            h.inventory.setItem(13, item(Material.LODESTONE, description, NamedTextColor.YELLOW,
                    "Operacja ponownie sprawdzi wszystkie zasady"));
            h.inventory.setItem(11, confirm());
            h.inventory.setItem(15, cancel());
            h.inventory.setItem(49, back());
            player.openInventory(h.inventory);
        } catch (SQLException e) { error(player, "Opening core action confirmation", e); }
    }

    private void openComingSoon(Player player, Screen screen) {
        Holder h = new Holder(player, screen, 0, 0L, "EarthSMP | W przygotowaniu");
        switch (screen) {
            case DIPLOMACY -> h.inventory.setItem(13, item(Material.IRON_SWORD, "Dyplomacja - jeszcze nieaktywna",
                    NamedTextColor.YELLOW, "Wypowiadanie wojen i sojusze", "Nastepny etap: 1.5.0"));
            case HISTORY -> h.inventory.setItem(13, item(Material.BOOK, "Historia - jeszcze nieaktywna",
                    NamedTextColor.YELLOW, "Dziennik zdarzen bedziemy rozwijac pozniej"));
            case SETTINGS -> h.inventory.setItem(13, item(Material.COMPARATOR, "Ustawienia - jeszcze nieaktywne",
                    NamedTextColor.YELLOW, "Dalsze zarzadzanie krajem bedzie pozniej"));
            default -> throw new IllegalArgumentException("Unsupported placeholder screen");
        }
        h.inventory.setItem(49, back());
        player.openInventory(h.inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof Holder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!player.getUniqueId().equals(holder.viewer)) return;
        if (event.getClickedInventory() != holder.inventory) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= holder.inventory.getSize()) return;
        Runnable next = action(holder, player, slot);
        // Reopening/closing inside InventoryClickEvent is unsafe; defer to next tick.
        if (next != null) plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && player.getOpenInventory().getTopInventory() == holder.inventory) {
                next.run();
            }
        });
    }

    private Runnable action(Holder h, Player player, int slot) {
        switch (h.screen) {
            case HUB -> {
                return switch (slot) {
                    case 12 -> () -> plugin.getNationMenu().openMain(player);
                    case 28 -> () -> plugin.getNationMenu().openInvitations(player);
                    case 14 -> () -> openTerritory(player);
                    case 16 -> () -> openCores(player);
                    case 30 -> () -> plugin.getWarMenu().openMain(player);
                    case 32 -> () -> plugin.getSiegeMenu().open(player);
                    case 34 -> () -> openComingSoon(player, Screen.SETTINGS);
                    case 49 -> player::closeInventory;
                    default -> null;
                };
            }
            case TERRITORY -> {
                return switch (slot) {
                    case 10 -> () -> territory(player, "here");
                    case 12 -> () -> territory(player, "pos1");
                    case 14 -> () -> territory(player, "pos2");
                    case 16 -> () -> territory(player, "selection");
                    case 28 -> () -> territoryConfirm(player, true);
                    case 30 -> () -> territoryConfirm(player, false);
                    case 32 -> () -> openTerritoryRequests(player, false, 0);
                    case 34 -> player.hasPermission("earthsmp.admin")
                            ? () -> openTerritoryRequests(player, true, 0) : null;
                    case 49 -> () -> openMain(player);
                    default -> null;
                };
            }
            case TERRITORY_CONFIRM_ADD, TERRITORY_CONFIRM_REMOVE -> {
                return switch (slot) {
                    case 11 -> () -> {
                        territory(player, "propose", h.screen == Screen.TERRITORY_CONFIRM_ADD ? "add" : "remove");
                        openTerritory(player);
                    };
                    case 15, 49 -> () -> openTerritory(player);
                    default -> null;
                };
            }
            case TERRITORY_OWN_REQUESTS, TERRITORY_ADMIN_REQUESTS -> {
                boolean admin = h.screen == Screen.TERRITORY_ADMIN_REQUESTS;
                if (slot < PAGE_SIZE) return () -> {
                    if (admin && !player.hasPermission("earthsmp.admin")) {
                        warn(player, "Brak uprawnien administratora.");
                        openTerritory(player);
                        return;
                    }
                    Long id = displayedRequestId(h, slot);
                    if (id != null) requestDetails(player, id, admin, h.page);
                    else openTerritoryRequests(player, admin, h.page);
                };
                return switch (slot) {
                    case 45 -> h.page > 0 ? () -> openTerritoryRequests(player, admin, h.page - 1) : null;
                    case 49 -> () -> openTerritory(player);
                    case 53 -> () -> openTerritoryRequests(player, admin, h.page + 1);
                    default -> null;
                };
            }
            case TERRITORY_CANCEL, TERRITORY_ADMIN_REVIEW -> {
                return switch (slot) {
                    case 11 -> () -> {
                        if (h.screen == Screen.TERRITORY_CANCEL) territory(player, "cancel", Long.toString(h.requestId));
                        else territory(player, "approve", Long.toString(h.requestId));
                        openTerritoryRequests(player, h.screen == Screen.TERRITORY_ADMIN_REVIEW, h.page);
                    };
                    case 15 -> () -> {
                        if (h.screen == Screen.TERRITORY_ADMIN_REVIEW) {
                            territory(player, "reject", Long.toString(h.requestId));
                        }
                        openTerritoryRequests(player, h.screen == Screen.TERRITORY_ADMIN_REVIEW, h.page);
                    };
                    case 49 -> () -> openTerritoryRequests(player, h.screen == Screen.TERRITORY_ADMIN_REVIEW, h.page);
                    default -> null;
                };
            }
            case CORES -> {
                return switch (slot) {
                    case 10 -> () -> core(player, "here");
                    case 12 -> () -> core(player, "stock");
                    case 14 -> () -> coreConfirm(player, Screen.CORE_CONFIRM_FREE);
                    case 28 -> () -> coreConfirm(player, Screen.CORE_CONFIRM_PICKUP);
                    case 30 -> player.hasPermission("earthsmp.admin") ? () -> core(player, "zonecheck") : null;
                    case 32 -> () -> coreConfirm(player, Screen.CORE_CONFIRM_CAPITAL);
                    case 49 -> () -> openMain(player);
                    default -> null;
                };
            }
            case CORE_CONFIRM_FREE, CORE_CONFIRM_PICKUP, CORE_CONFIRM_CAPITAL -> {
                return switch (slot) {
                    case 11 -> () -> {
                        switch (h.screen) {
                            case CORE_CONFIRM_FREE -> core(player, "free");
                            case CORE_CONFIRM_PICKUP -> core(player, "pickup");
                            case CORE_CONFIRM_CAPITAL -> core(player, "capital");
                            default -> { }
                        }
                        openCores(player);
                    };
                    case 15, 49 -> () -> openCores(player);
                    default -> null;
                };
            }
            case DIPLOMACY, HISTORY, SETTINGS -> {
                return slot == 49 ? () -> openMain(player) : null;
            }
        }
        return null;
    }

    private void territory(Player player, String... args) {
        String[] full = new String[args.length + 1];
        full[0] = "territory";
        System.arraycopy(args, 0, full, 1, args.length);
        plugin.getTerritoryCommand().handle(player, full);
    }

    private void core(Player player, String action) {
        plugin.getCoreCommand().handle(player, new String[] {"core", action});
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof Holder)) return;
        if (event.getRawSlots().stream().anyMatch(slot -> slot < event.getView().getTopInventory().getSize())) {
            event.setCancelled(true);
        }
    }

    private static boolean isManager(String role) {
        return "LEADER".equals(role) || "DEPUTY".equals(role);
    }

    private static int clamp(int page, int count) {
        return Math.max(0, Math.min(page, Math.max(0, (count - 1) / PAGE_SIZE)));
    }

    private static void navigation(Inventory inv, int page, int total) {
        if (page > 0) inv.setItem(45, item(Material.ARROW, "Poprzednia strona", NamedTextColor.YELLOW));
        inv.setItem(49, back());
        if ((page + 1) * PAGE_SIZE < total) {
            inv.setItem(53, item(Material.ARROW, "Nastepna strona", NamedTextColor.YELLOW));
        }
    }

    private static ItemStack back() {
        return item(Material.OAK_DOOR, "Powrot", NamedTextColor.GREEN);
    }

    private static ItemStack confirm() {
        return item(Material.LIME_WOOL, "Potwierdz", NamedTextColor.GREEN);
    }

    private static ItemStack cancel() {
        return item(Material.RED_WOOL, "Anuluj", NamedTextColor.RED);
    }

    private static ItemStack item(Material material, String label, NamedTextColor color, String... description) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(label, color));
        List<Component> lore = new ArrayList<>();
        for (String text : description) lore.add(Component.text(text, NamedTextColor.GRAY));
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private static void warn(Player player, String message) {
        player.sendMessage(Component.text(message, NamedTextColor.YELLOW));
    }

    private void error(Player player, String context, SQLException exception) {
        plugin.getLogger().log(Level.SEVERE, context, exception);
        player.sendMessage(Component.text("Blad SQLite. Sprawdz konsole serwera.", NamedTextColor.RED));
    }
}
