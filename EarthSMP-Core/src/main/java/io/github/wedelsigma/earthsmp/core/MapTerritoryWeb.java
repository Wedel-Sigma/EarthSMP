package io.github.wedelsigma.earthsmp.core;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/** Web territory operations execute ONLY on Bukkit's main thread.
 * Authorization is re-evaluated on every request against CURRENT membership and operator status.
 */
final class MapTerritoryWeb {
    record Reply(int status, String json) {}
    private final EarthSmpCorePlugin plugin;
    private final NationDatabase db;

    MapTerritoryWeb(EarthSmpCorePlugin plugin) {
        this.plugin = plugin;
        this.db = plugin.getNationDatabase();
    }

    private static Reply ok(String json) { return new Reply(200, json); }
    private static Reply error(int status, String message) {
        return new Reply(status, "{\"error\":" + MapPortalJson.quote(message) + "}");
    }

    private boolean admin(UUID viewer) {
        String id = viewer.toString();
        if (plugin.getConfig().getStringList("map-portal.admin-uuids").contains(id)) return true;
        Player online = Bukkit.getPlayer(viewer);
        if (online != null && online.hasPermission("earthsmp.admin")) return true;
        return Bukkit.getOfflinePlayer(viewer).isOp();
    }

    private Optional<NationDatabase.Membership> manager(UUID viewer) throws SQLException {
        var m = db.findMembership(viewer);
        if (m.isEmpty() || !("LEADER".equals(m.get().role()) || "DEPUTY".equals(m.get().role())))
            return Optional.empty();
        return m;
    }

    Reply overview(UUID viewer) throws SQLException {
        var membership = db.findMembership(viewer);
        boolean manager = membership.isPresent() &&
                ("LEADER".equals(membership.get().role()) || "DEPUTY".equals(membership.get().role()));
        boolean admin = admin(viewer);
        StringBuilder json = new StringBuilder("{\"nation\":");
        json.append(MapPortalJson.quote(membership.map(NationDatabase.Membership::nationName).orElse(null)));
        json.append(",\"manager\":").append(manager);
        json.append(",\"admin\":").append(admin);
        json.append(",\"own\":");
        json.append(requests(db.findOwnPendingTerritoryRequests(viewer)));
        json.append(",\"pending\":");
        json.append(admin ? requests(db.findPendingTerritoryRequests()) : "[]");
        return ok(json.append('}').toString());
    }

    private static String requests(List<NationDatabase.TerritoryRequest> values) {
        StringBuilder json = new StringBuilder("[");
        for (var r : values) {
            if (json.length() > 1) json.append(',');
            json.append("{\"id\":").append(r.id());
            json.append(",\"nation\":").append(MapPortalJson.quote(r.nationName()));
            json.append(",\"action\":").append(MapPortalJson.quote(r.action()));
            json.append(",\"count\":").append(r.size());
            json.append(",\"minX\":").append(r.minX());
            json.append(",\"maxX\":").append(r.maxX());
            json.append(",\"minZ\":").append(r.minZ());
            json.append(",\"maxZ\":").append(r.maxZ());
            json.append('}');
        }
        return json.append(']').toString();
    }

    Reply detail(UUID viewer, long id) throws SQLException {
        var request = db.findTerritoryRequest(id);
        if (request.isEmpty()) return error(404, "Nie znaleziono wniosku.");
        var membership = db.findMembership(viewer);
        if (!admin(viewer) && (membership.isEmpty()
                || !request.get().nationId().equals(membership.get().nationId()))) {
            return error(403, "Brak dostepu do wniosku.");
        }
        if (!"PENDING".equals(request.get().status())) return error(409, "Wniosek nie jest juz oczekujacy.");
        List<NationDatabase.TerritoryChunk> chunks = request.get().sparse()
                ? db.territoryRequestChunks(id) : List.of();
        List<NationDatabase.TerritoryChunk> conflicting = new java.util.ArrayList<>();
        if (request.get().sparse()) {
            var owners = db.ownersOfSelection(request.get().world(), chunks);
            for (var chunk : chunks) {
                var owner = owners.get(chunk);
                if (("ADD".equals(request.get().action()) && owner != null
                        && !owner.nationId().equals(request.get().nationId()))
                        || ("REMOVE".equals(request.get().action()) && (owner == null
                        || !owner.nationId().equals(request.get().nationId())))) {
                    conflicting.add(chunk);
                }
            }
        }
        return ok("{\"id\":" + id + ",\"nation\":" + MapPortalJson.quote(request.get().nationName())
                + ",\"action\":" + MapPortalJson.quote(request.get().action())
                + ",\"sparse\":" + request.get().sparse()
                + ",\"bounds\":[" + request.get().minX() + "," + request.get().minZ() + ","
                + request.get().maxX() + "," + request.get().maxZ() + "]"
                + ",\"chunks\":" + (request.get().sparse() ? chunkArray(chunks) : "[]")
                + ",\"conflicts\":" + chunkArray(conflicting) + "}");
    }

    Reply preview(UUID viewer, String raw) throws SQLException {
        if (manager(viewer).isEmpty()) return error(403, "Tylko lider lub zastepca moze skladac wnioski.");
        List<NationDatabase.TerritoryChunk> selected = MapChunkSelection.parse(raw);
        World world = earth();
        if (world == null || !inside(world, selected)) return error(400, "Wybor poza worldborderem Earth.");
        var membership = db.findMembership(viewer).orElseThrow();
        Map<NationDatabase.TerritoryChunk, NationDatabase.TerritoryOwner> owners = db.ownersOfSelection(world.getName(), selected);
        StringBuilder output = new StringBuilder("{\"chunks\":[");
        for (var c : selected) {
            var o = owners.get(c);
            String status = o == null ? "NEUTRAL" :
                    o.nationId().equals(membership.nationId()) ? "OWN" : "FOREIGN";
            if (output.charAt(output.length()-1) != '[') output.append(',');
            output.append("{\"x\":").append(c.x()).append(",\"z\":").append(c.z());
            output.append(",\"owner\":").append(MapPortalJson.quote(o == null ? null : o.nationName()));
            output.append(",\"status\":").append(MapPortalJson.quote(status)).append('}');
        }
        return ok(output.append("]}").toString());
    }

    Reply submit(UUID viewer, String action, String raw) throws SQLException {
        if (!"ADD".equals(action) && !"REMOVE".equals(action)) return error(400, "Nieznana operacja.");
        var membership = manager(viewer);
        if (membership.isEmpty()) return error(403, "Tylko lider lub zastepca moze skladac wnioski.");
        List<NationDatabase.TerritoryChunk> selected = MapChunkSelection.parse(raw);
        World world = earth();
        if (world == null || !inside(world, selected)) return error(400, "Wybor poza worldborderem Earth.");
        // Fail closed at submission. Repeated validation during admin approval handles races.
        Map<NationDatabase.TerritoryChunk, NationDatabase.TerritoryOwner> owners = db.ownersOfSelection(world.getName(), selected);
        for (var chunk : selected) {
            var o = owners.get(chunk);
            if ("ADD".equals(action) && o != null && !o.nationId().equals(membership.get().nationId()))
                return error(409, "Zaznaczono cudze terytorium. Wybierz neutralne lub wlasne chunki.");
            if ("REMOVE".equals(action) && (o == null || !o.nationId().equals(membership.get().nationId())))
                return error(409, "Do oddania wybierz tylko wlasne chunki.");
        }
        var id = db.submitSelectedTerritoryRequest(viewer, world.getName(), "ADD".equals(action), selected);
        return id.isPresent() ? ok("{\"ok\":true,\"id\":" + id.get() + "}")
                : error(403, "Utracono uprawnienia lidera lub zastepcy.");
    }

    Reply review(UUID viewer, long id, boolean approve) throws SQLException {
        if (!admin(viewer)) return error(403, "Tylko administrator moze zatwierdzac wnioski.");
        var outcome = db.reviewTerritory(id, "WEB:" + viewer, approve);
        if (outcome == NationDatabase.TerritoryDecision.CONFLICT)
            return error(409, "Konflikt: wlasciciel przynajmniej jednego chunka zmienil sie.");
        if (outcome != (approve ? NationDatabase.TerritoryDecision.APPROVED : NationDatabase.TerritoryDecision.REJECTED))
            return error(404, "Wniosek nie istnieje lub zostal juz rozpatrzony.");
        if (approve) plugin.refreshTerritoryMap();
        return ok("{\"ok\":true}");
    }

    Reply cancel(UUID viewer, long id) throws SQLException {
        var result = db.cancelTerritoryRequest(id, viewer);
        return result == NationDatabase.TerritoryDecision.CANCELLED ? ok("{\"ok\":true}")
                : error(result == NationDatabase.TerritoryDecision.NOT_AUTHORIZED ? 403 : 404,
                        "Brak uprawnien lub wniosek juz nie istnieje.");
    }

    private World earth() {
        return Bukkit.getWorld(plugin.getConfig().getString("world-name", "Earth"));
    }

    private static boolean inside(World world, List<NationDatabase.TerritoryChunk> selection) {
        var border = world.getWorldBorder();
        for (var chunk : selection) {
            if (!border.isInside(new Location(world, chunk.x() * 16.0 + 8, 64,
                    chunk.z() * 16.0 + 8))) return false;
        }
        return true;
    }

    private static String chunkArray(List<NationDatabase.TerritoryChunk> values) {
        StringBuilder output = new StringBuilder("[");
        for (var chunk : values) {
            if (output.length() > 1) output.append(',');
            output.append('[').append(chunk.x()).append(',').append(chunk.z()).append(']');
        }
        return output.append(']').toString();
    }
}
