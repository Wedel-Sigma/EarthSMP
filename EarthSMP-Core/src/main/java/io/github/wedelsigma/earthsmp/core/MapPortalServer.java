package io.github.wedelsigma.earthsmp.core;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.bukkit.Bukkit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Dedicated, same-origin web portal. No Paper/Bukkit/SQLite method is ever called on HTTP threads.
 * The unmodified squaremap public frontend remains available independently.
 */
final class MapPortalServer implements AutoCloseable {
    private static final Pattern PUBLIC_RESOURCE = Pattern.compile(
            "^/(?:tiles/settings\\.json|tiles/[a-zA-Z0-9_-]+/(?:settings\\.json|markers\\.json|[0-9]{1,2}/-?[0-9]{1,9}_-?[0-9]{1,9}\\.png)|images/icon/registered/[a-zA-Z0-9._-]+\\.png)$");
    private static final int MAX_PROXY_BYTES = 2_000_000;
    // squaremap returns HTTP 200 with an empty body for unrendered tiles.
    // A normal Leaflet image element displays a broken-image symbol in this case.
    private static final byte[] TRANSPARENT_PNG = java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGNgYGBgAAAABQABpfZFQAAAAABJRU5ErkJggg==");

    static byte[] pngOrTransparent(int httpStatus, byte[] bytes) {
        if (httpStatus == 200 && bytes.length > 0) return bytes;
        if (httpStatus == 200 || httpStatus == 404) return TRANSPARENT_PNG;
        return null;
    }
    private static final int MAX_REQUEST_BYTES = 900;
    private static final String COOKIE = "ESMP_MAP_SESSION";

    private final MapPortalAuth auth;
    private final EarthSmpCorePlugin plugin;
    private final String displayName;
    private final Logger log;
    private final URI origin;
    private final URI squaremap;
    private final HttpClient client;
    private final boolean secureCookies;
    private final HttpServer http;
    private final ExecutorService workers;
    private volatile MapPortalSnapshot snapshot = MapPortalSnapshot.empty();

    MapPortalServer(String bind, int port, String publicOrigin, String squaremapUrl,
                    MapPortalAuth auth, Logger log, EarthSmpCorePlugin plugin) throws IOException {
        this.auth = auth;
        this.plugin = plugin;
        // Snapshot config on the main thread; HTTP worker threads never access Bukkit config.
        this.displayName = plugin.getDisplayName();
        this.log = log;
        try {
            this.origin = validateOrigin(publicOrigin);
            this.squaremap = validateSquaremap(squaremapUrl);
        } catch (IllegalArgumentException error) { throw new IOException("Invalid map-portal configuration: " + error.getMessage(), error); }
        if (port < 1024 || port > 65535) throw new IOException("Map portal port must be 1024..65535");
        this.secureCookies = "https".equals(origin.getScheme());
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
        this.http = HttpServer.create(new InetSocketAddress(bind, port), 32);
        AtomicInteger threadNo = new AtomicInteger();
        this.workers = Executors.newFixedThreadPool(6, task -> {
            Thread t = new Thread(task, "earthsmp-map-" + threadNo.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        http.setExecutor(workers);
        http.createContext("/", this::serve);
        http.start();
        log.info("EarthSMP private map portal started: " + origin + " (bound " + bind + ":" + port + ")");
    }

    void update(MapPortalSnapshot value) { this.snapshot = value; }
    String url() { return origin.toString(); }

    @Override public void close() {
        http.stop(0);
        workers.shutdownNow();
        // Session database belongs to the plugin lifecycle and closes separately.
    }

    private static URI validateOrigin(String input) {
        URI u = URI.create(input);
        if (!("http".equals(u.getScheme()) || "https".equals(u.getScheme())) || u.getHost() == null
                || u.getPort() <= 0 || u.getPort() > 65535 || u.getUserInfo() != null
                || (u.getRawPath() != null && !u.getRawPath().isEmpty() && !"/".equals(u.getRawPath()))
                || u.getRawQuery() != null || u.getRawFragment() != null) {
            throw new IllegalArgumentException("public-origin must be e.g. http://127.0.0.1:8766 (without a path)");
        }
        return URI.create(u.getScheme() + "://" + u.getRawAuthority());
    }

    private static URI validateSquaremap(String input) {
        URI u = validateOrigin(input);
        // No SSRF: the proxy only talks to a loopback address, never arbitrary configured Internet hosts.
        if (!"http".equals(u.getScheme()) || !("127.0.0.1".equals(u.getHost()) || "localhost".equalsIgnoreCase(u.getHost()))) {
            throw new IllegalArgumentException("squaremap-internal-url must use http://127.0.0.1:PORT");
        }
        return u;
    }

    private void serve(HttpExchange exchange) throws IOException {
        try {
            headers(exchange);
            if (!hostAllowed(exchange)) { send(exchange, 403, "text/plain", "Forbidden host".getBytes(StandardCharsets.UTF_8)); return; }
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if ("GET".equals(method) && ("/".equals(path) || "/index.html".equals(path))) {
                resource(exchange, "/map-portal/index.html", "text/html; charset=utf-8"); return;
            }
            if ("GET".equals(method) && "/assets/portal.js".equals(path)) {
                resource(exchange, "/map-portal/portal.js", "text/javascript; charset=utf-8"); return;
            }
            if ("GET".equals(method) && "/assets/portal.css".equals(path)) {
                resource(exchange, "/map-portal/portal.css", "text/css; charset=utf-8"); return;
            }
            if ("GET".equals(method) && path.startsWith("/map-data/")) {
                proxyPublic(exchange, path.substring("/map-data".length())); return;
            }
            if ("POST".equals(method) && "/api/link".equals(path)) {
                if (!postAllowed(exchange)) return;
                login(exchange, true); return;
            }
            if ("POST".equals(method) && "/api/login".equals(path)) {
                if (!postAllowed(exchange)) return;
                login(exchange, false); return;
            }
            if ("POST".equals(method) && "/api/logout".equals(path)) {
                if (!postAllowed(exchange)) return;
                auth.logout(cookie(exchange));
                exchange.getResponseHeaders().add("Set-Cookie", expireCookie());
                json(exchange, 200, "{\"ok\":true}"); return;
            }
            if ("GET".equals(method) && "/api/positions".equals(path)) {
                Optional<UUID> viewer = auth.authenticate(cookie(exchange));
                if (viewer.isEmpty()) { json(exchange, 401, "{\"error\":\"Zaloguj sie nickiem i haslem.\"}"); return; }
                MapPortalSnapshot snap = snapshot;
                var visible = snap.visibleTo(viewer.get());
                if (visible.isEmpty()) { json(exchange, 403, "{\"error\":\"Twoje konto nie ma aktualnych uprawnien do mapy.\"}"); return; }
                json(exchange, 200, MapPortalJson.positions(viewer.get(), snap, visible.get())); return;
            }
            if ("GET".equals(method) && "/api/me".equals(path)) {
                Optional<UUID> viewer = auth.authenticate(cookie(exchange));
                json(exchange, viewer.isPresent() ? 200 : 401, viewer.isPresent() ? "{\"authenticated\":true}" : "{\"authenticated\":false}");
                return;
            }
            if (path.startsWith("/api/territory/")) {
                territory(exchange, path, method);
                return;
            }
            json(exchange, 404, "{\"error\":\"Not found\"}");
        } catch (Exception error) {
            log.log(Level.WARNING, "Map portal HTTP failure", error);
            try { json(exchange, 500, "{\"error\":\"Blad serwera mapy\"}"); } catch (IOException ignored) { }
        } finally {
            exchange.close();
        }
    }

    /** All territory access and mutation is executed on the Bukkit main thread.
     * HTTP threads only authenticate session cookies and parse untrusted input.
     */
    private void territory(HttpExchange ex, String path, String method) throws IOException, SQLException {
        Optional<UUID> authenticated = auth.authenticate(cookie(ex));
        if (authenticated.isEmpty()) {
            json(ex, 401, "{\"error\":\"Zaloguj sie na mapie.\"}");
            return;
        }
        UUID viewer = authenticated.get();
        if ("POST".equals(method) && !postAllowed(ex)) return;
        Map<String, String> fields = Map.of();
        if ("POST".equals(method)) {
            try { fields = readForm(ex, 80_000); }
            catch (IllegalArgumentException exception) {
                json(ex, 400, "{\"error\":" + MapPortalJson.quote(exception.getMessage()) + "}"); return;
            }
        }
        final Map<String, String> form = fields;
        MapTerritoryWeb.Reply result;
        if ("GET".equals(method) && "/api/territory/overview".equals(path)) {
            result = mainThread(() -> new MapTerritoryWeb(plugin).overview(viewer));
        } else if ("GET".equals(method) && "/api/territory/detail".equals(path)) {
            long id = requestId(ex.getRequestURI().getRawQuery());
            result = id > 0 ? mainThread(() -> new MapTerritoryWeb(plugin).detail(viewer, id))
                    : new MapTerritoryWeb.Reply(400, "{\"error\":\"Nieprawidlowe ID wniosku.\"}");
        } else if ("POST".equals(method) && "/api/territory/preview".equals(path)) {
            result = mainThread(() -> new MapTerritoryWeb(plugin).preview(viewer, form.get("chunks")));
        } else if ("POST".equals(method) && "/api/territory/submit".equals(path)) {
            result = mainThread(() -> new MapTerritoryWeb(plugin).submit(viewer, form.get("action"), form.get("chunks")));
        } else if ("POST".equals(method) && "/api/territory/review".equals(path)) {
            long id = formId(form);
            if (id < 1 || !("approve".equals(form.get("decision")) || "reject".equals(form.get("decision")))) {
                result = new MapTerritoryWeb.Reply(400, "{\"error\":\"Nieprawidlowa decyzja.\"}");
            } else result = mainThread(() -> new MapTerritoryWeb(plugin).review(viewer, id, "approve".equals(form.get("decision"))));
        } else if ("POST".equals(method) && "/api/territory/cancel".equals(path)) {
            long id = formId(form);
            result = id > 0 ? mainThread(() -> new MapTerritoryWeb(plugin).cancel(viewer, id))
                    : new MapTerritoryWeb.Reply(400, "{\"error\":\"Nieprawidlowe ID.\"}");
        } else result = new MapTerritoryWeb.Reply(404, "{\"error\":\"Not found\"}");
        json(ex, result.status(), result.json());
    }

    private static long formId(Map<String, String> form) { return requestId("id=" + form.get("id")); }
    private static long requestId(String query) {
        if (query == null || !query.matches("id=[1-9][0-9]{0,14}")) return -1;
        try { return Long.parseLong(query.substring(3)); }
        catch (NumberFormatException error) { return -1; }
    }

    private static Map<String, String> readForm(HttpExchange ex, int limit) throws IOException {
        byte[] bytes = ex.getRequestBody().readNBytes(limit + 1);
        if (bytes.length > limit) throw new IllegalArgumentException("Zbyt duzy formularz.");
        Map<String, String> result = new java.util.HashMap<>();
        try {
            for (String part : new String(bytes, StandardCharsets.UTF_8).split("&", -1)) {
                int eq = part.indexOf('=');
                if (eq < 0) throw new IllegalArgumentException("Nieprawidlowy formularz.");
                String key = URLDecoder.decode(part.substring(0, eq), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
                if (result.putIfAbsent(key, value) != null) throw new IllegalArgumentException("Powtorzone pole formularza.");
            }
        } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Nieprawidlowy formularz."); }
        return Map.copyOf(result);
    }

    private MapTerritoryWeb.Reply mainThread(Callable<MapTerritoryWeb.Reply> work) {
        if (!plugin.isEnabled()) return new MapTerritoryWeb.Reply(503, "{\"error\":\"Plugin jest wylaczony.\"}");
        var result = new CompletableFuture<MapTerritoryWeb.Reply>();
        try {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try { result.complete(work.call()); }
                catch (IllegalArgumentException invalid) {
                    result.complete(new MapTerritoryWeb.Reply(400,
                            "{\"error\":" + MapPortalJson.quote(invalid.getMessage()) + "}"));
                } catch (Exception error) { result.completeExceptionally(error); }
            });
            return result.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new MapTerritoryWeb.Reply(503, "{\"error\":\"Serwer zajety.\"}");
        } catch (TimeoutException error) {
            // A queued approval may still finish: the user must refresh requests before retrying.
            return new MapTerritoryWeb.Reply(503, "{\"error\":\"Uplynal czas odpowiedzi. Odswiez liste przed ponowna proba.\"}");
        } catch (ExecutionException | RuntimeException error) {
            log.log(Level.SEVERE, "Territory portal operation failed", error);
            return new MapTerritoryWeb.Reply(500, "{\"error\":\"Wystapil blad SQLite lub serwera.\"}");
        }
    }

    private boolean hostAllowed(HttpExchange ex) {
        String wanted = origin.getRawAuthority();
        String host = ex.getRequestHeaders().getFirst("Host");
        return host != null && host.equalsIgnoreCase(wanted);
    }

    private boolean postAllowed(HttpExchange ex) throws IOException {
        String got = ex.getRequestHeaders().getFirst("Origin");
        if (!origin.toString().equals(got)) {
            json(ex, 403, "{\"error\":\"Invalid Origin\"}"); return false;
        }
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase(java.util.Locale.ROOT).startsWith("application/x-www-form-urlencoded")) {
            json(ex, 415, "{\"error\":\"Unsupported content type\"}"); return false;
        }
        return true;
    }

    private void login(HttpExchange ex, boolean firstPairing) throws IOException, SQLException {
        byte[] body = ex.getRequestBody().readNBytes(MAX_REQUEST_BYTES + 1);
        if (body.length > MAX_REQUEST_BYTES) { json(ex, 413, "{\"error\":\"Request too large\"}"); return; }
        Map<String, String> fields = new java.util.HashMap<>();
        try {
            String input = new String(body, StandardCharsets.UTF_8);
            for (String pair : input.split("&", -1)) {
                int eq = pair.indexOf('=');
                if (eq < 0) continue;
                String name = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                if (fields.putIfAbsent(name, value) != null) {
                    json(ex, 400, "{\"error\":\"Duplicate field\"}"); return;
                }
            }
        } catch (IllegalArgumentException ignored) {
            json(ex, 400, "{\"error\":\"Invalid form\"}"); return;
        }
        String address = ex.getRemoteAddress().getAddress().getHostAddress();
        Optional<MapPortalAuth.SessionToken> result;
        if (firstPairing) {
            String password = fields.get("password");
            if (!MapPortalAuth.validPassword(password)) {
                json(ex, 400, "{\"error\":\"Haslo musi miec od 12 do 128 znakow.\"}"); return;
            }
            result = auth.redeem(fields.get("code"), password, address);
        } else {
            result = auth.login(fields.get("username"), fields.get("password"), address);
        }
        if (result.isEmpty()) {
            json(ex, 401, "{\"error\":\"Nieprawidlowe dane, zuzyty kod lub limit prob.\"}"); return;
        }
        ex.getResponseHeaders().add("Set-Cookie", COOKIE + "=" + result.get().token()
                + "; HttpOnly; SameSite=Strict; Path=/; Max-Age=" + MapPortalAuth.COOKIE_MAX_AGE
                + (secureCookies ? "; Secure" : ""));
        json(ex, 200, "{\"ok\":true}");
    }

    private String expireCookie() {
        return COOKIE + "=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0" + (secureCookies ? "; Secure" : "");
    }

    private String cookie(HttpExchange ex) {
        String source = ex.getRequestHeaders().getFirst("Cookie");
        if (source == null || source.length() > 4096) return null;
        for (String part : source.split(";")) {
            String item = part.trim();
            if (item.startsWith(COOKIE + "=")) return item.substring((COOKIE + "=").length());
        }
        return null;
    }

    private void proxyPublic(HttpExchange ex, String path) throws IOException {
        if (!PUBLIC_RESOURCE.matcher(path).matches()) { json(ex, 404, "{\"error\":\"Not found\"}"); return; }
        String target = squaremap + path;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(target)).timeout(Duration.ofSeconds(7))
                    .header("User-Agent", "EarthSMPCore-map-portal/1.8.5").GET().build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.body().length > MAX_PROXY_BYTES) { send(ex, 502, "text/plain", "Map resource too large".getBytes(StandardCharsets.UTF_8)); return; }
            if (path.endsWith(".png")) {
                byte[] tile = pngOrTransparent(response.statusCode(), response.body());
                if (tile != null) { send(ex, 200, "image/png", tile); return; }
            }
            if (response.statusCode() != 200) { send(ex, 502, "text/plain", "squaremap not ready".getBytes(StandardCharsets.UTF_8)); return; }
            send(ex, 200, path.endsWith(".png") ? "image/png" : "application/json; charset=utf-8", response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            send(ex, 503, "text/plain", "squaremap interrupted".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.log(Level.FINE, "squaremap proxy unavailable", e);
            send(ex, 502, "text/plain", "squaremap unavailable".getBytes(StandardCharsets.UTF_8));
        }
    }

    private void resource(HttpExchange ex, String path, String type) throws IOException {
        try (InputStream in = MapPortalServer.class.getResourceAsStream(path)) {
            if (in == null) { send(ex, 404, "text/plain", "Missing packaged resource".getBytes(StandardCharsets.UTF_8)); return; }
            byte[] bytes = in.readAllBytes();
            if ("/map-portal/index.html".equals(path)) {
                String html = new String(bytes, StandardCharsets.UTF_8);
                html = html.replace("{{SERVER_DISPLAY_NAME}}", escapeHtml(displayName));
                bytes = html.getBytes(StandardCharsets.UTF_8);
            }
            send(ex, 200, type, bytes);
        }
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static void json(HttpExchange ex, int status, String value) throws IOException {
        send(ex, status, "application/json; charset=utf-8", value.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange ex, int status, String type, byte[] bytes) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(status, bytes.length);
        try (var out = ex.getResponseBody()) { out.write(bytes); }
    }

    private static void headers(HttpExchange ex) {
        var h = ex.getResponseHeaders();
        h.set("Cache-Control", "no-store, private");
        h.set("Pragma", "no-cache");
        h.set("X-Content-Type-Options", "nosniff");
        h.set("X-Frame-Options", "DENY");
        h.set("Referrer-Policy", "no-referrer");
        h.set("Cross-Origin-Resource-Policy", "same-origin");
        h.set("Content-Security-Policy", "default-src 'none'; script-src 'self' https://unpkg.com; "
                + "style-src 'self' 'unsafe-inline' https://unpkg.com; img-src 'self' data: https://unpkg.com; "
                + "connect-src 'self'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'");
    }
}
