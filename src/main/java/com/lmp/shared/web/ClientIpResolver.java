package com.lmp.shared.web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Adresse IP client derrière reverse proxy / CDN (Forwarded, X-Forwarded-For, CF-Connecting-IP, etc.).
 *
 * <p>Ordre volontairement proche des usages Dokploy, Traefik, Nginx, Cloudflare : en-têtes « edge » d’abord,
 * puis chaîne {@code X-Forwarded-For} en prenant le <strong>premier hop public</strong> (ignore les segments
 * internes type 10.x / 172.16–31 / Docker) lorsque le bord a laissé une chaîne du type {@code 10.0.0.1, 203.0.113.4}.
 */
public final class ClientIpResolver {

    private static final Pattern FORWARDED_FOR_CLAUSE = Pattern.compile(
            "(?i)(?:^|[;,]\\s*)for\\s*=\\s*(?:\"([^\"]+)\"|'([^']+)'|\\[([^]]+)]|([^;,\\s]+))");

    private ClientIpResolver() {}

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return "";
        }

        String[] earlyCandidates = {
                trimHeaderValue(request.getHeader("CF-Connecting-IP")),
                trimHeaderValue(request.getHeader("True-Client-IP")),
                trimHeaderValue(request.getHeader("X-Real-IP")),
                parseForwardedHeader(request.getHeader("Forwarded"))
        };
        for (String candidate : earlyCandidates) {
            if (candidate.isBlank()) {
                continue;
            }
            if (InetRoutability.isPublicRoutable(candidate)) {
                return InetRoutability.trimHostPort(candidate);
            }
        }

        String ip = firstPublicInXForwardedFor(request.getHeader("X-Forwarded-For"));
        if (!ip.isEmpty()) {
            return ip;
        }

        String remote = request.getRemoteAddr() != null ? request.getRemoteAddr().trim() : "";
        remote = InetRoutability.trimHostPort(remote);
        if (!remote.isEmpty()) {
            return remote;
        }
        return "";
    }

    private static String trimHeaderValue(String h) {
        if (h == null) {
            return "";
        }
        String t = h.trim();
        int comma = t.indexOf(',');
        if (comma > 0) {
            t = t.substring(0, comma).trim();
        }
        return t;
    }

    /**
     * RFC 7239 {@code Forwarded} — prend la première clause {@code for=} (côté client).
     */
    static String parseForwardedHeader(String forwarded) {
        if (forwarded == null || forwarded.isBlank()) {
            return "";
        }
        Matcher m = FORWARDED_FOR_CLAUSE.matcher(forwarded);
        if (!m.find()) {
            return "";
        }
        for (int g = 1; g <= m.groupCount(); g++) {
            String cap = m.group(g);
            if (cap != null && !cap.isBlank()) {
                return cap.trim();
            }
        }
        return "";
    }

    /**
     * Parcourt les segments de {@code X-Forwarded-For} et retourne le premier IP <strong>publique</strong>.
     * S’il n’y en a aucune, retourne le premier segment non vide (comportement de secours).
     */
    static String firstPublicInXForwardedFor(String xff) {
        if (xff == null || xff.isBlank()) {
            return "";
        }
        for (String part : xff.split(",")) {
            String host = InetRoutability.trimHostPort(part.trim());
            if (host.isEmpty()) {
                continue;
            }
            if (InetRoutability.isPublicRoutable(host)) {
                return host;
            }
        }
        return "";
    }
}
