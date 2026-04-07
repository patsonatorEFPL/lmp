package com.lmp.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    @Test
    void xForwardedFor_skipsPrivatePrefix() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "10.0.0.1, 172.16.0.5, 203.0.113.9");
        assertThat(ClientIpResolver.resolve(req)).isEqualTo("203.0.113.9");
    }

    @Test
    void cfConnectingIp_takesPrecedenceOverPrivateXff() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "10.0.0.1");
        req.addHeader("CF-Connecting-IP", "198.51.100.77");
        assertThat(ClientIpResolver.resolve(req)).isEqualTo("198.51.100.77");
    }

    @Test
    void forwardedHeader_parsed() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Forwarded", "for=198.51.100.1;proto=https;host=example.com");
        assertThat(ClientIpResolver.resolve(req)).isEqualTo("198.51.100.1");
    }

    @Test
    void skipsPrivateXRealIp_usesForwarded() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Real-IP", "10.0.0.1");
        req.addHeader("Forwarded", "for=198.51.100.2;proto=https");
        assertThat(ClientIpResolver.resolve(req)).isEqualTo("198.51.100.2");
    }

    @Test
    void xRealIp_whenNoPublicXff() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "10.0.0.1");
        req.addHeader("X-Real-IP", "192.0.2.44");
        assertThat(ClientIpResolver.resolve(req)).isEqualTo("192.0.2.44");
    }

    @Test
    void fallsBackToRemoteAddr() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("203.0.113.55");
        assertThat(ClientIpResolver.resolve(req)).isEqualTo("203.0.113.55");
    }

    @Test
    void firstPublicInXForwardedFor_onlyPrivate_returnsEmpty() {
        assertThat(ClientIpResolver.firstPublicInXForwardedFor("10.1.2.3, 172.18.0.1")).isEmpty();
    }
}
