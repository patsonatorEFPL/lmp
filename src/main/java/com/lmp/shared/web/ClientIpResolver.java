package com.lmp.shared.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Adresse IP client derrière un reverse proxy (X-Forwarded-For, X-Real-IP).
 */
public final class ClientIpResolver {

    private ClientIpResolver() {}

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isEmpty()) {
            return xri.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "";
    }
}
