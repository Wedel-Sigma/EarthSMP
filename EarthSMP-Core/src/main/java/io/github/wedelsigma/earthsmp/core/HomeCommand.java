package io.github.wedelsigma.earthsmp.core;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/** Teleport to the nation's capital, allowed both in peace and during a war. */
public final class HomeCommand {
    private static final int DAILY_LIMIT = 4;
    private final EarthSmpCorePlugin plugin;

    public HomeCommand(EarthSmpCorePlugin plugin) { this.plugin = plugin; }

    public void execute(Player player) {
        if (player.isDead()) {
            send(player, "Nie mozna teleportowac sie podczas smierci.", NamedTextColor.RED);
            return;
        }
        try {
            var db = plugin.getNationDatabase();
            var member = db.findMembership(player.getUniqueId());
            if (member.isEmpty()) {
                send(player, "Aby uzyc /home, musisz nalezec do panstwa.", NamedTextColor.YELLOW);
                return;
            }
            var capital = db.capitalOf(member.get().nationId());
            if (capital.isEmpty()) {
                send(player, "Twoje panstwo nie ma postawionego rdzenia-stolicy.", NamedTextColor.YELLOW);
                return;
            }
            var clock = plugin.getServerClock();
            Instant now = clock.now();
            int editionDay = clock.day(db.editionStart(), now);
            LocalDate dayKey = clock.serverDate(now);
            boolean limited = editionDay >= 3;
            int used = limited ? db.homeUses(player.getUniqueId(), dayKey) : 0;
            if (limited && used >= DAILY_LIMIT) {
                send(player, "Limit /home wyczerpany (4/4). Reset: " +
                        clock.nextReset(now).toLocalTime() + " czasu polskiego.", NamedTextColor.YELLOW);
                return;
            }

            World world = Bukkit.getWorld(capital.get().world());
            if (world == null) {
                send(player, "Swiat stolicy jest niedostepny.", NamedTextColor.RED);
                return;
            }
            Location destination = safeDestination(world, capital.get(), player.getLocation().getYaw());
            if (destination == null) {
                send(player, "Nie ma bezpiecznego miejsca przy stolicy. Nie pobrano teleportacji.", NamedTextColor.RED);
                return;
            }
            // Teleport synchronously; unsuccessful attempts never consume a daily charge.
            if (!player.teleport(destination)) {
                send(player, "Teleportacja zostala anulowana. Nie pobrano teleportacji.", NamedTextColor.YELLOW);
                return;
            }
            if (limited) {
                int count = db.recordHomeUse(player.getUniqueId(), dayKey, DAILY_LIMIT);
                send(player, "Teleportowano do stolicy. Pozostalo /home: " + (DAILY_LIMIT - count)
                        + "/" + DAILY_LIMIT + ".", NamedTextColor.GREEN);
            } else {
                send(player, "Teleportowano do stolicy. /home bez limitu (dni 1-2 lub przed startem).",
                        NamedTextColor.GREEN);
            }
        } catch (SQLException error) {
            plugin.getLogger().log(Level.SEVERE, "Home teleport SQL error", error);
            send(player, "Blad zapisu /home. Zglos to administracji.", NamedTextColor.RED);
        }
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
