package com.lmp.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Persisted OAuth2 JWK signing key. Used by
 * {@code AuthorizationServerConfig.jwkSource()} as the multi-replica
 * source of truth when {@code app.oauth2.jwk.content} env var is unset
 * (M8 fix).
 *
 * <p>Rotation pattern : insert new row with {@code active=true} + flip
 * the previous one to {@code active=false}. Both stay in the JWKSet
 * until the old row is DELETEd, which gives consumers a grace window
 * to keep validating tokens signed with the previous key.</p>
 *
 * <p>jwk_json field contains the FULL JWK (including private key
 * material). Treat as a secret — never log, never expose via API.</p>
 */
@Entity
@Table(name = "oauth2_signing_key")
public class OAuth2SigningKey {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "key_id", length = 64, nullable = false, unique = true)
    private String keyId;

    @Column(name = "jwk_json", columnDefinition = "TEXT", nullable = false)
    private String jwkJson;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "rotated_at")
    private OffsetDateTime rotatedAt;

    public OAuth2SigningKey() {}

    public OAuth2SigningKey(String keyId, String jwkJson) {
        this.keyId = keyId;
        this.jwkJson = jwkJson;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }

    public String getJwkJson() { return jwkJson; }
    public void setJwkJson(String jwkJson) { this.jwkJson = jwkJson; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getRotatedAt() { return rotatedAt; }
    public void setRotatedAt(OffsetDateTime rotatedAt) { this.rotatedAt = rotatedAt; }
}
