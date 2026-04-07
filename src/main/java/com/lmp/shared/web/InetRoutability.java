package com.lmp.shared.web;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Détermine si une IP est utilisable pour une géolocalisation publique (pas loopback / RFC1918 / ULA IPv6, etc.).
 */
public final class InetRoutability {

    private InetRoutability() {}

    public static boolean isPrivateOrNonRoutable(String ip) {
        if (ip == null || ip.isBlank()) {
            return true;
        }
        try {
            return isPrivateOrNonRoutable(InetAddress.getByName(trimHostPort(ip.trim())));
        } catch (UnknownHostException e) {
            return true;
        }
    }

    public static boolean isPublicRoutable(String ip) {
        return !isPrivateOrNonRoutable(ip);
    }

    static boolean isPrivateOrNonRoutable(InetAddress a) {
        if (a.isLoopbackAddress() || a.isAnyLocalAddress()) {
            return true;
        }
        if (a.isLinkLocalAddress() || a.isMulticastAddress()) {
            return true;
        }
        if (a.isSiteLocalAddress()) {
            return true;
        }
        if (a instanceof Inet6Address ia6) {
            byte[] b = ia6.getAddress();
            if (b.length >= 1 && (b[0] & 0xfe) == 0xfc) {
                return true;
            }
        }
        if (a instanceof Inet4Address) {
            byte[] b = a.getAddress();
            int o0 = b[0] & 0xff;
            int o1 = b[1] & 0xff;
            if (o0 == 100 && o1 >= 64 && o1 <= 127) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retire un éventuel port IPv4 {@code a.b.c.d:port} ou une zone IPv6 {@code fe80::1%eth0}.
     */
    static String trimHostPort(String raw) {
        if (raw.isEmpty()) {
            return raw;
        }
        if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2) {
            raw = raw.substring(1, raw.length() - 1).trim();
        }
        int pct = raw.indexOf('%');
        if (pct >= 0) {
            raw = raw.substring(0, pct);
        }
        if (raw.startsWith("[")) {
            int end = raw.indexOf(']');
            if (end > 1) {
                return raw.substring(1, end);
            }
        }
        int colon = raw.indexOf(':');
        if (colon > 0 && raw.indexOf(':', colon + 1) < 0) {
            return raw.substring(0, colon);
        }
        return raw;
    }
}
