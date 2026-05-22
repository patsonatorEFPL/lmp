package com.lmp.support.signaling;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

/**
 * Issues short-lived TURN credentials following the coturn
 * {@code --use-auth-secret} convention: {@code username = "<expiry>:<hint>"},
 * {@code credential = Base64(HMAC-SHA1(secret, username))}. No DB round-trip.
 */
@Service
public class TurnCredentialService {

    private final String secret;
    private final String host;
    private final int port;
    private final int tlsPort;
    private final Duration ttl;

    public TurnCredentialService(
        @Value("${lmp.helpdesk.coturn.secret:}") String secret,
        @Value("${lmp.helpdesk.coturn.host:turn.lmp-services.ca}") String host,
        @Value("${lmp.helpdesk.coturn.port:3478}") int port,
        @Value("${lmp.helpdesk.coturn.tls-port:5349}") int tlsPort,
        @Value("${lmp.helpdesk.coturn.ttl:PT15M}") Duration ttl
    ) {
        this.secret = secret;
        this.host = host;
        this.port = port;
        this.tlsPort = tlsPort;
        this.ttl = ttl;
    }

    public TurnCredentialResponse issue(String usernameHint) {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("COTURN_SECRET not configured");
        }
        String hint = (usernameHint == null || usernameHint.isBlank()) ? "anon" : usernameHint;
        long expiry = (System.currentTimeMillis() / 1000L) + ttl.getSeconds();
        String username = expiry + ":" + hint;
        String credential = computeHmac(username);
        return new TurnCredentialResponse(
            username,
            credential,
            expiry,
            List.of(
                "turn:" + host + ":" + port,
                "turn:" + host + ":" + port + "?transport=tcp",
                "turns:" + host + ":" + tlsPort
            )
        );
    }

    private String computeHmac(String username) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] digest = mac.doFinal(username.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC computation failed", e);
        }
    }
}
