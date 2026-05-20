package com.lmp.support.signaling;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TurnCredentialServiceTest {

    @Test
    void issuesCredentialWithCorrectShape() {
        TurnCredentialService svc = new TurnCredentialService(
            "supersecret", "turn.lmp-services.ca", 3478, 5349, Duration.ofMinutes(15));

        TurnCredentialResponse creds = svc.issue("lmp-tech");

        long now = System.currentTimeMillis() / 1000L;
        assertThat(creds.expiryEpochSeconds()).isBetween(now + 800, now + 1000);
        assertThat(creds.username()).startsWith(String.valueOf(creds.expiryEpochSeconds()));
        assertThat(creds.username()).endsWith(":lmp-tech");
        assertThat(creds.credential()).isNotEmpty();
        assertThat(creds.uris()).contains(
            "turn:turn.lmp-services.ca:3478",
            "turn:turn.lmp-services.ca:3478?transport=tcp",
            "turns:turn.lmp-services.ca:5349");
    }

    @Test
    void credentialIsHmacSha1OfUsernameWithSecret() throws Exception {
        TurnCredentialService svc = new TurnCredentialService(
            "supersecret", "turn.lmp-services.ca", 3478, 5349, Duration.ofMinutes(15));
        TurnCredentialResponse creds = svc.issue("lmp-client");

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec("supersecret".getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        String expected = Base64.getEncoder().encodeToString(
            mac.doFinal(creds.username().getBytes(StandardCharsets.UTF_8)));

        assertThat(creds.credential()).isEqualTo(expected);
    }

    @Test
    void nullHintFallsBackToAnon() {
        TurnCredentialService svc = new TurnCredentialService(
            "s", "h", 3478, 5349, Duration.ofMinutes(5));
        TurnCredentialResponse creds = svc.issue(null);
        assertThat(creds.username()).endsWith(":anon");
    }

    @Test
    void blankHintFallsBackToAnon() {
        TurnCredentialService svc = new TurnCredentialService(
            "s", "h", 3478, 5349, Duration.ofMinutes(5));
        TurnCredentialResponse creds = svc.issue("   ");
        assertThat(creds.username()).endsWith(":anon");
    }

    @Test
    void missingSecretThrows() {
        TurnCredentialService svc = new TurnCredentialService(
            "", "h", 3478, 5349, Duration.ofMinutes(5));
        assertThatThrownBy(() -> svc.issue("anyone"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("COTURN_SECRET");
    }
}
