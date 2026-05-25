package com.lmp.auth.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.JSONObjectUtils;

import java.text.ParseException;
import java.util.List;
import java.util.function.Function;

import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration du Spring Authorization Server (OAuth2 / OIDC).
 *
 * Spring Boot = fournisseur d’identité. Les applications satellites (n8n, outils internes, etc.)
 * s’authentifient via OAuth2/OIDC lorsque leurs identifiants et l’URI de redirection sont configurés.
 *
 * Tables JDBC : oauth2_registered_client, oauth2_authorization, oauth2_authorization_consent
 * (créées par V2__oauth2_authorization_server.sql)
 */
@Configuration
public class AuthorizationServerConfig {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationServerConfig.class);

    @org.springframework.beans.factory.annotation.Value("${app.oauth2.external.client-id:oauth-external-client}")
    private String externalOAuthClientId;

    @org.springframework.beans.factory.annotation.Value("${app.oauth2.external.client-secret:}")
    private String externalOAuthClientSecret;

    @org.springframework.beans.factory.annotation.Value("${app.oauth2.external.redirect-uri:}")
    private String externalOAuthRedirectUri;

    @org.springframework.beans.factory.annotation.Value("${app.oauth2.erp.client-id:}")
    private String erpOAuthClientId;

    @org.springframework.beans.factory.annotation.Value("${app.oauth2.erp.client-secret:}")
    private String erpOAuthClientSecret;

    @org.springframework.beans.factory.annotation.Value("${app.oauth2.erp.redirect-uri:}")
    private String erpOAuthRedirectUri;

    @org.springframework.beans.factory.annotation.Value("${app.oauth2.issuer-uri:http://localhost:8080}")
    private String issuerUri;

    /**
     * JWK content inline (JSON string) — pattern 12-factor pour multi-replica.
     * Si défini (env var {@code OAUTH2_JWK_CONTENT}), prend précédence sur la DB :
     * toutes les répliques chargent la MÊME clé, immutable, déployable via
     * pipeline secrets. Sinon, fallback DB-backed (M8 fix) — voir
     * {@link #jwkSource}.
     *
     * <p>Le legacy {@code app.oauth2.jwk.path} file-based fallback a été retiré
     * dans le commit M8 : prod + staging utilisaient déjà env var, et la DB
     * couvre désormais le cas single-replica sans risque de drift entre
     * filesystems éphémères de containers.</p>
     */
    @org.springframework.beans.factory.annotation.Value("${app.oauth2.jwk.content:}")
    private String jwkContent;

    @Bean
    @Order(0) // Avant les autres SecurityFilterChains
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http,
                                                                       com.lmp.auth.repository.UserRepository userRepository) throws Exception {
        OAuth2AuthorizationServerConfigurer configurer = new OAuth2AuthorizationServerConfigurer();

        http
                .securityMatcher(configurer.getEndpointsMatcher())
                .with(configurer, server -> server
                        .oidc(oidc -> oidc
                                .userInfoEndpoint(userInfo -> userInfo
                                        .userInfoMapper(oidcUserInfoMapper(userRepository))
                                )
                        )
                )


                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
                .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()));

        return http.build();
    }

    /**
     * Repository JDBC pour les clients OAuth2 enregistrés.
     * Les clients sont persistés en base (table oauth2_registered_client).
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate,
                                                                  PasswordEncoder passwordEncoder) {
        JdbcRegisteredClientRepository repository = new JdbcRegisteredClientRepository(jdbcTemplate);

        if (repository.findByClientId(externalOAuthClientId) == null
                && StringUtils.hasText(externalOAuthRedirectUri)
                && StringUtils.hasText(externalOAuthClientSecret)) {
            RegisteredClient externalClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(externalOAuthClientId)
                    .clientSecret(passwordEncoder.encode(externalOAuthClientSecret))
                    .clientName("Client OAuth externe")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .redirectUri(externalOAuthRedirectUri)
                    .scope(OidcScopes.OPENID)
                    .scope(OidcScopes.PROFILE)
                    .scope(OidcScopes.EMAIL)
                    .clientSettings(ClientSettings.builder()
                            .requireAuthorizationConsent(true)
                            .build())
                    .tokenSettings(TokenSettings.builder()
                            .accessTokenTimeToLive(Duration.ofHours(1))
                            .refreshTokenTimeToLive(Duration.ofDays(30))
                            .build())
                    .build();

            repository.save(externalClient);
            logger.info("OAuth2 client satellite enregistré (client_id={})", externalOAuthClientId);
        } else if (repository.findByClientId(externalOAuthClientId) == null) {
            logger.debug(
                    "Enregistrement OAuth2 satellite ignoré : définir OAUTH2_EXTERNAL_REDIRECT_URI et OAUTH2_EXTERNAL_CLIENT_SECRET pour créer le client au démarrage.");
        }

        // --- Client ERP (SSO pour le staff) ---
        if (StringUtils.hasText(erpOAuthClientId)
                && repository.findByClientId(erpOAuthClientId) == null
                && StringUtils.hasText(erpOAuthRedirectUri)
                && StringUtils.hasText(erpOAuthClientSecret)) {
            RegisteredClient erpClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(erpOAuthClientId)
                    .clientSecret(passwordEncoder.encode(erpOAuthClientSecret))
                    .clientName("Système ERP (SSO staff)")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .redirectUri(erpOAuthRedirectUri)
                    .scope(OidcScopes.OPENID)
                    .scope(OidcScopes.PROFILE)
                    .scope(OidcScopes.EMAIL)
                    .clientSettings(ClientSettings.builder()
                            .requireAuthorizationConsent(false) // Staff SSO — pas de consent screen
                            // SECURITY (M10) : PKCE activé. OAuth 2.1 recommande PKCE pour
                            // TOUS les clients y compris confidentiels. Protège contre auth code
                            // interception (referer leak, log scraping, proxy intercept) même
                            // si client_secret est connu de l'attaquant.
                            // Frappe oauth2_client (>= v15) supporte PKCE natif via
                            // request_kwargs={"code_challenge_method": "S256"}.
                            // Si flow Frappe casse avec PKCE on : (a) downgrade requireProofKey(false)
                            // ET (b) configure code_challenge côté Frappe avant de re-activer.
                            .requireProofKey(true)
                            .build())
                    .tokenSettings(TokenSettings.builder()
                            // SECURITY (M10) : access TTL réduit 4h → 1h. Token compromis
                            // = fenêtre d'exploitation 4x plus courte. Refresh token 30j
                            // permet renouvellement transparent côté Frappe sans re-login user.
                            .accessTokenTimeToLive(Duration.ofHours(1))
                            .refreshTokenTimeToLive(Duration.ofDays(30))
                            .build())
                    .build();

            repository.save(erpClient);
            logger.info("OAuth2 client ERP enregistré (client_id={}, redirect={})",
                    erpOAuthClientId, erpOAuthRedirectUri);
        }

        return repository;
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate,
                                                            RegisteredClientRepository clientRepository) {
        // Use in-memory in dev to avoid Jackson serialization issues with immutable collections
        OAuth2AuthorizationService delegate = new InMemoryOAuth2AuthorizationService();
        return new OAuth2AuthorizationService() {
            @Override
            public void save(OAuth2Authorization authorization) {
                var code = authorization.getToken(OAuth2AuthorizationCode.class);
                logger.info(">>> AUTH-SERVICE SAVE id={} code={} principal={}",
                    authorization.getId(),
                    code != null ? code.getToken().getTokenValue() : null,
                    authorization.getPrincipalName());
                delegate.save(authorization);
            }
            @Override
            public void remove(OAuth2Authorization authorization) {
                delegate.remove(authorization);
            }
            @Override
            public OAuth2Authorization findById(String id) {
                return delegate.findById(id);
            }
            @Override
            public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
                OAuth2Authorization result = delegate.findByToken(token, tokenType);
                logger.info(">>> AUTH-SERVICE FIND token={} type={} found={}", token, tokenType, result != null);
                return result;
            }
        };
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate,
                                                                         RegisteredClientRepository clientRepository) {
        return new InMemoryOAuth2AuthorizationConsentService();
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> lmpOAuth2TokenCustomizer(
            com.lmp.auth.repository.UserRepository userRepository) {
        return new com.lmp.auth.oauth.LmpOAuth2TokenCustomizer(userRepository);
    }

    @Bean
    public Function<OidcUserInfoAuthenticationContext, OidcUserInfo> oidcUserInfoMapper(
            com.lmp.auth.repository.UserRepository userRepository) {
        return new com.lmp.auth.oauth.LmpOidcUserInfoMapper(userRepository);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(issuerUri)
                .build();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(
            com.lmp.auth.repository.OAuth2SigningKeyRepository signingKeyRepository,
            org.springframework.transaction.support.TransactionTemplate txTemplate) {
        // 1. Précédence à app.oauth2.jwk.content (env var) — pattern 12-factor.
        //    Toutes les répliques chargent la même clé. Pas de fichier sur disque.
        if (jwkContent != null && !jwkContent.isBlank()) {
            try {
                JWKSet jwkSet = JWKSet.parse(jwkContent);
                logger.info("JWK chargé depuis app.oauth2.jwk.content (env var) — multi-replica safe");
                return new ImmutableJWKSet<>(jwkSet);
            } catch (ParseException e) {
                throw new IllegalStateException("app.oauth2.jwk.content (OAUTH2_JWK_CONTENT) ne contient pas un JWKSet JSON valide", e);
            }
        }

        // 2. SECURITY (M8) : DB-backed key (multi-replica safe sans env var).
        //    Si la table contient au moins une clé active, on les expose toutes
        //    dans le JWKSet. La PREMIÈRE (most recent par createdAt DESC) sert
        //    pour signer ; les autres restent disponibles pour validation
        //    pendant la fenêtre de grâce d'une rotation.
        List<com.lmp.auth.domain.OAuth2SigningKey> dbKeys = signingKeyRepository.findByActiveTrueOrderByCreatedAtDesc();
        if (!dbKeys.isEmpty()) {
            try {
                List<JWK> jwks = new java.util.ArrayList<>(dbKeys.size());
                for (com.lmp.auth.domain.OAuth2SigningKey row : dbKeys) {
                    jwks.add(JWK.parse(row.getJwkJson()));
                }
                JWKSet jwkSet = new JWKSet(jwks);
                logger.info("JWK chargé depuis DB : {} clé(s) active(s), kid principal={} (multi-replica safe)",
                        dbKeys.size(), dbKeys.get(0).getKeyId());
                return new ImmutableJWKSet<>(jwkSet);
            } catch (ParseException e) {
                throw new IllegalStateException("Une entrée oauth2_signing_key contient un JWK JSON invalide", e);
            }
        }

        // 3. DB vide → générer une clé fresh et la persister sous transaction.
        //    Évite que deux replicas génèrent des clés différentes en concurrence
        //    (premier wins via UNIQUE(key_id) ; deuxième relit et utilise celle persistée).
        com.lmp.auth.domain.OAuth2SigningKey persistedRow = txTemplate.execute(status -> {
            List<com.lmp.auth.domain.OAuth2SigningKey> raceRecheck = signingKeyRepository.findByActiveTrueOrderByCreatedAtDesc();
            if (!raceRecheck.isEmpty()) {
                return raceRecheck.get(0);
            }
            KeyPair keyPair = generateRsaKey();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            String keyId = UUID.randomUUID().toString();
            RSAKey rsaKey = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(keyId)
                    .build();
            String jwkJson = JSONObjectUtils.toJSONString(rsaKey.toJSONObject());
            com.lmp.auth.domain.OAuth2SigningKey newKey = new com.lmp.auth.domain.OAuth2SigningKey(keyId, jwkJson);
            com.lmp.auth.domain.OAuth2SigningKey saved = signingKeyRepository.save(newKey);
            logger.info("M8 : nouvelle clé JWK générée + persistée DB (kid={}) — sera réutilisée sur tous les replicas + restarts", keyId);
            return saved;
        });
        try {
            JWKSet jwkSet = new JWKSet(JWK.parse(persistedRow.getJwkJson()));
            return new ImmutableJWKSet<>(jwkSet);
        } catch (ParseException e) {
            throw new IllegalStateException("JWK fraîchement généré illisible — ne devrait jamais arriver", e);
        }
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate RSA key pair", ex);
        }
    }
}
