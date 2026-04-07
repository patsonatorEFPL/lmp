package com.lmp.shared.web;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
            // #region agent log
            agentNdjson("H5", "resolve_null_request", "", "empty", null, null, null, null, null, "");
            // #endregion
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
                // #region agent log
                agentNdjson("H3_H4", "resolved_early_public", resolved, earlySources[i], hCf, hTrue, hXReal, hXff, hFwd, hRemote);
                // #endregion
                return resolved;
            }
        }

        String ip = firstPublicInXForwardedFor(hXff);
        if (!ip.isEmpty()) {
            logger.debug("[ClientIpResolver] Resolved IP from X-Forwarded-For: {}", ip);
            // #region agent log
            agentNdjson("H2", "resolved_xff_public", ip, "xff_first_public", hCf, hTrue, hXReal, hXff, hFwd, hRemote);
            // #endregion
            return ip;
        }

        String remote = hRemote != null ? hRemote.trim() : "";
        remote = InetRoutability.trimHostPort(remote);
        if (!remote.isEmpty()) {
            logger.debug("[ClientIpResolver] Falling back to RemoteAddr: {}", remote);
            // #region agent log
            String hid = InetRoutability.isPrivateOrNonRoutable(remote) ? "H1" : "H5";
            agentNdjson(hid, "resolved_remote_addr", remote, "remote_addr_fallback", hCf, hTrue, hXReal, hXff, hFwd, hRemote);
            // #endregion
            return remote;
        }
        logger.debug("[ClientIpResolver] No client IP resolved");
        // #region agent log
        agentNdjson("H1", "resolved_empty", "", "empty", hCf, hTrue, hXReal, hXff, hFwd, hRemote);
        // #endregion
        return "";
    }

    /**
     * NDJSON debug ingest (session e89717). Activé si {@code LMP_DEBUG_IP_LOG=true}.
     * Chemin : {@code LMP_DEBUG_LOG_PATH} ou {@code user.dir/debug-e89717.log}.
     */
    // #region agent log
    private static void agentNdjson(
            String hypothesisId,
            String message,
            String resolvedIp,
            String resolutionSource,
            String hCf,
            String hTrue,
            String hXReal,
            String hXff,
            String hFwd,
            String hRemote) {
        if (!Boolean.parseBoolean(System.getenv().getOrDefault("LMP_DEBUG_IP_LOG", "false"))) {
            return;
        }
        try {
            boolean hasXff = hXff != null && !hXff.isBlank();
            String xffFirst = "";
            boolean xffFirstPrivate = false;
            if (hasXff) {
                String[] parts = hXff.split(",");
                if (parts.length > 0) {
                    xffFirst = InetRoutability.trimHostPort(parts[0].trim());
                    xffFirstPrivate = !xffFirst.isEmpty() && InetRoutability.isPrivateOrNonRoutable(xffFirst);
                }
            }
            boolean remotePrivate = hRemote != null && InetRoutability.isPrivateOrNonRoutable(InetRoutability.trimHostPort(hRemote.trim()));
            String xffEsc = escapeJson(hasXff ? truncate(hXff, 240) : "");
            String fwdEsc = escapeJson(hFwd != null ? truncate(hFwd, 160) : "");
            long ts = System.currentTimeMillis();
            String json = "{\"sessionId\":\"e89717\",\"timestamp\":" + ts
                    + ",\"hypothesisId\":\"" + escapeJson(hypothesisId) + "\""
                    + ",\"location\":\"ClientIpResolver.resolve\""
                    + ",\"message\":\"" + escapeJson(message) + "\""
                    + ",\"data\":{"
                    + "\"resolvedIp\":\"" + escapeJson(resolvedIp) + "\""
                    + ",\"resolutionSource\":\"" + escapeJson(resolutionSource) + "\""
                    + ",\"hasCfHeader\":" + (hCf != null && !hCf.isBlank())
                    + ",\"hasXRealHeader\":" + (hXReal != null && !hXReal.isBlank())
                    + ",\"hasXff\":" + hasXff
                    + ",\"xffFirstSegmentPrivate\":" + xffFirstPrivate
                    + ",\"hasForwardedHeader\":" + (hFwd != null && !hFwd.isBlank())
                    + ",\"remoteAddrPrivate\":" + remotePrivate
                    + ",\"xffTruncated\":\"" + xffEsc + "\""
                    + ",\"forwardedTruncated\":\"" + fwdEsc + "\""
                    + "}}\n";
            Path path = debugLogPath();
            Files.writeString(path, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // never break request handling
        }
    }

    private static Path debugLogPath() {
        String override = System.getenv("LMP_DEBUG_LOG_PATH");
        if (override != null && !override.isBlank()) {
            return Path.of(override.trim());
        }
        return Path.of(System.getProperty("user.dir", "."), "debug-e89717.log");
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s == null ? "" : s;
        }
        return s.substring(0, max) + "…";
    }

    private static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
    // #endregion

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
