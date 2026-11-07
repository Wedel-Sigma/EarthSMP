package io.github.wedelsigma.earthsmp.core;

import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;

/** Only exact origins/hosts and explicitly trusted reverse-proxy addresses are accepted. */
final class MapPortalSecurity {
    private MapPortalSecurity() {}

    static URI validateOrigin(String input) {
        URI u = URI.create(input);
        String scheme = u.getScheme() == null ? "" : u.getScheme().toLowerCase(Locale.ROOT);
        if (!("http".equals(scheme) || "https".equals(scheme)) || u.getHost() == null
                || u.getPort() == 0 || u.getPort() < -1 || u.getPort() > 65535 || u.getUserInfo() != null
                || (u.getRawPath() != null && !u.getRawPath().isEmpty() && !"/".equals(u.getRawPath()))
                || u.getRawQuery() != null || u.getRawFragment() != null) {
            throw new IllegalArgumentException("public-origin must be e.g. https://map.example.com or http://127.0.0.1:8766");
        }
        // Preserve optional explicit port; normalize away a trailing slash in config.
        return URI.create(scheme + "://" + u.getRawAuthority());
    }

    private static int effectivePort(URI u) {
        return u.getPort() == -1 ? ("https".equalsIgnoreCase(u.getScheme()) ? 443 : 80) : u.getPort();
    }

    static boolean hostAllowed(URI origin, String suppliedHost) {
        if (suppliedHost == null || suppliedHost.isBlank()) return false;
        try {
            URI host = URI.create(origin.getScheme() + "://" + suppliedHost);
            return host.getHost() != null && host.getHost().equalsIgnoreCase(origin.getHost())
                    && effectivePort(host) == effectivePort(origin)
                    && host.getRawUserInfo() == null && (host.getRawPath() == null || host.getRawPath().isEmpty())
                    && host.getRawQuery() == null && host.getRawFragment() == null;
        } catch (IllegalArgumentException exception) { return false; }
    }

    static boolean originAllowed(URI origin, String suppliedOrigin) {
        if (suppliedOrigin == null || suppliedOrigin.isBlank()) return false;
        try {
            // The HTTP Origin header never contains a slash-terminated path.
            URI candidate = URI.create(suppliedOrigin);
            if (candidate.getRawPath() != null && !candidate.getRawPath().isEmpty()) return false;
            candidate = validateOrigin(suppliedOrigin);
            return candidate.getScheme().equalsIgnoreCase(origin.getScheme())
                    && candidate.getHost().equalsIgnoreCase(origin.getHost())
                    && effectivePort(candidate) == effectivePort(origin);
        } catch (IllegalArgumentException exception) { return false; }
    }

    /** Trust X-Real-IP ONLY when the connecting peer is explicitly configured as a trusted proxy.
     * The proxy MUST overwrite the inbound X-Real-IP header; it must not forward client-supplied values.
     */
    static String clientAddress(String socketIp, String trustedProxyIp, String realIpHeader) {
        if (socketIp == null) return "unknown";
        if (trustedProxyIp == null || trustedProxyIp.isBlank() || !socketIp.equalsIgnoreCase(trustedProxyIp))
            return socketIp;
        if (realIpHeader == null) return socketIp;
        String value = realIpHeader.trim();
        if (value.length() > 45 || value.isEmpty() || value.contains(",")) return socketIp;
        if (value.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}")) {
            for (String octet : value.split("\\.")) if (Integer.parseInt(octet) > 255) return socketIp;
            return value;
        }
        // No DNS lookups: IPv6 literals contain only colons and hexadecimal characters.
        if (value.contains(":") && value.matches("[0-9a-fA-F:]+")) {
            try {
                if (InetAddress.getByName(value).getAddress().length == 16) return value;
            } catch (Exception ignored) { }
        }
        return socketIp;
    }
}
