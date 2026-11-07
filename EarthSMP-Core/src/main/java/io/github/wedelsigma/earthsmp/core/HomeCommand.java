package io.github.wedelsigma.earthsmp.core;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;

/**
 * Teleports to the current capital, even during war, after an uninterrupted
 * 30-second warmup. All callbacks must run on Paper's primary thread.
 */
public final class HomeCommand implements Listener {
    private static final int DAILY_LIMIT = 4;
    private static final String CANCELLED_NO_CHARGE = " Nie zuzyto limitu /home.";

    // Same native Minecraft Title presentation as the wartime respawn timer.
    private static final Title.Times COUNTDOWN_TIMES = Title.Times.times(
            Duration.ZERO, Duration.ofMillis(1400), Duration.ZERO);
    private static final Title.Times RESULT_TIMES = Title.Times.times(
            Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(300));

    private record Pending(Location origin, long deadlineNanos) { }

    private final EarthSmpCorePlugin plugin;
    private final Map<UUID, Pending> pending = new HashMap<>();
    private BukkitTask ticker;

    public HomeCommand(EarthSmpCorePlugin plugin) { this.plugin = plugin; }
    
    public boolean isWarmingUp(Player player) { return pending.containsKey(player.getUniqueId()); }

    public void execute(Player player) {
        if (pending.containsKey(player.getUniqueId())) {
            send(player, "Odliczanie /home juz trwa. Nie ruszaj sie!", NamedTextColor.YELLOW);
            return;
        }
        if (player.isDead()) {
            send(player, "Nie mozna teleportowac sie podczas smierci.", NamedTextColor.RED);
            return;
        }
        try {
            // Validate BEFORE showing a timer, then validate AGAIN at completion.
            Location destination = checkedDestination(player);
            if (destination == null) return;

            long deadline = System.nanoTime() + HomeWarmupRules.WARMUP_NANOS;
            pending.put(player.getUniqueId(), new Pending(player.getLocation().clone(), deadline));
            showCountdown(player, HomeWarmupRules.WARMUP_SECONDS);
            send(player, "Teleportacja /home za 30 sekund. Ruch albo otrzymanie obrazen anuluje teleportacje.",
                    NamedTextColor.GOLD);
            if (ticker == null) {
                ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
            }
        } catch (SQLException error) {
            plugin.getLogger().log(Level.SEVERE, "Home warmup SQL error", error);
            send(player, "Blad sprawdzania /home. Nie rozpoczęto teleportacji.", NamedTextColor.RED);
        }
    }

    /** Only reads current DB data: ownership, capital, current day and remaining uses. */
    private Location checkedDestination(Player player) throws SQLException {
        var db = plugin.getNationDatabase();
        var member = db.findMembership(player.getUniqueId());
        if (member.isEmpty()) {
            send(player, "Aby uzyc /home, musisz nalezec do panstwa.", NamedTextColor.YELLOW);
            return null;
        }
        var capital = db.capitalOf(member.get().nationId());
        if (capital.isEmpty()) {
            send(player, "Twoje panstwo nie ma postawionego rdzenia-stolicy.", NamedTextColor.YELLOW);
            return null;
        }
        var clock = plugin.getServerClock();
        Instant now = clock.now();
        int editionDay = clock.day(db.editionStart(), now);
        LocalDate dayKey = clock.serverDate(now);
        if (editionDay >= 3 && db.homeUses(player.getUniqueId(), dayKey) >= DAILY_LIMIT) {
            send(player, "Limit /home wyczerpany (4/4). Reset: " +
                    clock.nextReset(now).toLocalTime() + " czasu polskiego.", NamedTextColor.YELLOW);
            return null;
        }

        World world = Bukkit.getWorld(capital.get().world());
        if (world == null) {
            send(player, "Swiat stolicy jest niedostepny.", NamedTextColor.RED);
            return null;
        }
        Location destination = safeDestination(world, capital.get(), player.getLocation().getYaw());
        if (destination == null) {
            send(player, "Nie ma bezpiecznego miejsca przy stolicy. Nie pobrano teleportacji.", NamedTextColor.RED);
        }
        return destination;
    }

    /** Single ticker for all players, no per-player scheduler tasks and no per-second chat spam. */
    private void tick() {
        long now = System.nanoTime();
        for (UUID id : new ArrayList<>(pending.keySet())) {
            Pending p = pending.get(id);
            if (p == null) continue;
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline() || player.isDead()) {
                clear(player, id, null);
                continue;
            }
            Location here = player.getLocation();
            if (here.getWorld() != p.origin().getWorld() || moved(p.origin(), here)) {
                clear(player, id, "Teleportacja /home anulowana: zmiana pozycji.");
                continue;
            }
            int seconds = HomeWarmupRules.remainingSeconds(p.deadlineNanos(), now);
            if (seconds == 0) {
                // Remove BEFORE teleport: our own PlayerTeleportEvent must not cancel the warmup.
                clear(player, id, null);
                complete(player);
            } else {
                showCountdown(player, seconds);
            }
        }
        stopTickerWhenIdle();
    }

    private void complete(Player player) {
        if (!player.isOnline() || player.isDead()) return;
        try {
            Location destination = checkedDestination(player);
            if (destination == null) return;
            // A different plugin may still cancel the teleport (e.g. respawn lock).
            if (!player.teleport(destination)) {
                send(player, "Teleportacja /home zostala zablokowana." + CANCELLED_NO_CHARGE,
                        NamedTextColor.YELLOW);
                return;
            }
            player.showTitle(Title.title(
                    Component.text("TELEPORTACJA!", NamedTextColor.GREEN),
                    Component.text("Wrociles do stolicy", NamedTextColor.GOLD), RESULT_TIMES));
            // Determine the server day at the moment the teleport actually succeeds.
            var clock = plugin.getServerClock();
            Instant now = clock.now();
            var db = plugin.getNationDatabase();
            int editionDay = clock.day(db.editionStart(), now);
            if (editionDay >= 3) {
                int count = db.recordHomeUse(player.getUniqueId(), clock.serverDate(now), DAILY_LIMIT);
                send(player, "Teleportowano do stolicy. Pozostalo /home: " + (DAILY_LIMIT - count)
                        + "/" + DAILY_LIMIT + ".", NamedTextColor.GREEN);
            } else {
                send(player, "Teleportowano do stolicy. /home bez limitu (dni 1-2 lub przed startem).",
                        NamedTextColor.GREEN);
            }
        } catch (SQLException error) {
            plugin.getLogger().log(Level.SEVERE, "Home completion SQL error", error);
            send(player, "Blad bazy /home. Zglos to administracji, sprawdzajac czy teleportacja nastapila.",
                    NamedTextColor.RED);
        }
    }

    private static void showCountdown(Player player, int remainingSeconds) {
        player.showTitle(Title.title(
                Component.text(Integer.toString(remainingSeconds), NamedTextColor.GOLD),
                Component.text("/home: ruch lub obrazenia anuluja", NamedTextColor.RED),
                COUNTDOWN_TIMES));
    }

    private static boolean moved(Location from, Location to) {
        return HomeWarmupRules.moved(from.getX(), from.getY(), from.getZ(),
                to.getX(), to.getY(), to.getZ());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Pending p = pending.get(event.getPlayer().getUniqueId());
        if (p == null || event.getTo() == null) return;
        if (event.getFrom().getWorld() != event.getTo().getWorld()
                || moved(event.getFrom(), event.getTo())) {
            clear(event.getPlayer(), event.getPlayer().getUniqueId(),
                    "Teleportacja /home anulowana: ruszyles sie.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getFinalDamage() <= 0.0) return;
        clear(player, player.getUniqueId(), "Teleportacja /home anulowana: otrzymales obrazenia.");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        clear(event.getPlayer(), event.getPlayer().getUniqueId(),
                "Teleportacja /home anulowana: inna teleportacja.");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        clear(event.getPlayer(), event.getPlayer().getUniqueId(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        clear(event.getEntity(), event.getEntity().getUniqueId(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        clear(event.getPlayer(), event.getPlayer().getUniqueId(),
                "Teleportacja /home anulowana: zmiana swiata.");
    }

    private void clear(Player player, UUID id, String message) {
        Pending previous = pending.remove(id);
        if (previous == null) return;
        if (player != null && player.isOnline()) {
            player.clearTitle();
            if (message != null) {
                player.showTitle(Title.title(
                        Component.text("ANULOWANO", NamedTextColor.RED),
                        Component.text("Nie zuzyto limitu /home", NamedTextColor.YELLOW),
                        RESULT_TIMES));
                send(player, message + CANCELLED_NO_CHARGE, NamedTextColor.YELLOW);
            }
        }
        stopTickerWhenIdle();
    }

    private void stopTickerWhenIdle() {
        if (pending.isEmpty() && ticker != null) {
            ticker.cancel();
            ticker = null;
        }
    }

    /** Called on plugin disable. Warmups live only in RAM and never survive restart. */
    public void shutdown() {
        for (UUID id : new ArrayList<>(pending.keySet())) {
            clear(Bukkit.getPlayer(id), id, null);
        }
        stopTickerWhenIdle();
    }

    /** Check the lodestone itself first; otherwise search a small accessible area. */
    static Location safeDestination(World world, NationDatabase.ProtectionCore core, float yaw) {
        if (world.getBlockAt(core.x(), core.y(), core.z()).getType() != Material.LODESTONE) return null;
        for (int radius = 0; radius <= 4; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    for (int dy = 1; dy <= 3; dy++) {
                        int x = core.x() + dx, y = core.y() + dy, z = core.z() + dz;
                        if (y < world.getMinHeight() + 1 || y + 1 >= world.getMaxHeight()) continue;
                        Block ground = world.getBlockAt(x, y - 1, z);
                        Block feet = world.getBlockAt(x, y, z);
                        Block head = world.getBlockAt(x, y + 1, z);
                        if (!ground.getType().isSolid() || !feet.getType().isAir() || !head.getType().isAir()) continue;
                        Location at = new Location(world, x + 0.5, y, z + 0.5, yaw, 0);
                        if (!world.getWorldBorder().isInside(at)) continue;
                        return at;
                    }
                }
            }
        }
        return null;
    }

    private static void send(Player player, String text, NamedTextColor color) {
        player.sendMessage(Component.text(text, color));
    }
}
