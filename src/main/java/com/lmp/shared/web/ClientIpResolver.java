package com.lmp.shared.web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Adresse IP client derrière reverse proxy / CDN (Forwarded, X-Forwarded-For, CF-Connecting-IP, etc.).
 *
 * <p>Ordre volontairement proche des usages Dokploy, Traefik, Nginx, Cloudflare : en-têtes « edge » d'abord,
 * puis chaîne {@code X-Forwarded-For} en prenant le <strong>premier hop public</strong> (ignore les segments
 * internes type 10.x / 172.16–31 / Docker) lorsque le bord a laissé une chaîne du type {@code 10.0.0.1, 203.0.113.4}.
 */
public final class ClientIpResolver {

    private static final Logger logger = LoggerFactory.getLogger(ClientIpResolver.class);

    private static final Pattern FORWARDED_FOR_CLAUSE = Pattern.compile(
            "(?i)(?:^|[;,]\\s*)for\\s*=\\s*(?:\"([^\"]+)\"|'([^']+)'|\\[([^]]+)]|([^;,\\s]+))");

    private ClientIpResolver() {}

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return "";
        }

        final String hCf = request.getHeader("CF-Connecting-IP");
        final String hTrue = request.getHeader("True-Client-IP");
        final String hXReal = request.getHeader("X-Real-IP");
        final String hXff = request.getHeader("X-Forwarded-For");
        final String hFwd = request.getHeader("Forwarded");
        final String hRemote = request.getRemoteAddr();

        // Debug: log all IP-related headers for troubleshooting
        if (logger.isDebugEnabled()) {
            logger.debug("[ClientIpResolver] Headers — CF-Connecting-IP: {}, True-Client-IP: {}, X-Real-IP: {}, X-Forwarded-For: {}, Forwarded: {}, RemoteAddr: {}",
                    hCf, hTrue, hXReal, hXff, hFwd, hRemote);
        }

        String[] earlyCandidates = {
                trimHeaderValue(hCf),
                trimHeaderValue(hTrue),
                trimHeaderValue(hXReal),
                parseForwardedHeader(hFwd)
        };
        final String[] earlySources = { "early_cf", "early_true_client", "early_x_real_ip", "forwarded_rfc7239" };
        for (int i = 0; i < earlyCandidates.length; i++) {
            String candidate = earlyCandidates[i];
            if (candidate.isBlank()) {
                continue;
            }
            if (InetRoutability.isPublicRoutable(candidate)) {
                String resolved = InetRoutability.trimHostPort(candidate);
                logger.debug("[ClientIpResolver] Resolved IP from early candidate ({}): {}", earlySources[i], candidate);
                return resolved;
            }
        }

        String ip = firstPublicInXForwardedFor(hXff);
        if (!ip.isEmpty()) {
            logger.debug("[ClientIpResolver] Resolved IP from X-Forwarded-For: {}", ip);
            return ip;
        }

        String remote = hRemote != null ? hRemote.trim() : "";
        remote = InetRoutability.trimHostPort(remote);
        if (!remote.isEmpty()) {
            logger.debug("[ClientIpResolver] Falling back to RemoteAddr: {}", remote);
            return remote;
        }
        logger.debug("[ClientIpResolver] No client IP resolved");
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
     * S’il n’y en a aucune, retourne une chaîne vide (on tentera {@code RemoteAddr} ensuite).
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
