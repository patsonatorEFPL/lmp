package com.lmp.shared.config.site;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire central de configuration du site.
 *
 * <p>Sources (par ordre de priorité) :</p>
 * <ol>
 *   <li>Variables d'environnement / properties Spring (ex: SITE_URL)</li>
 *   <li>Fichier {@code site-config.json} (reloadable à chaud)</li>
 *   <li>Base de données {@code site_config} (modifiable via admin)</li>
 *   <li>Valeurs dérivées de {@code lmp.site.url}</li>
 * </ol>
 *
 * <p>Toutes les lectures passent par un cache mémoire.
 * Le cache est invalidé lors d'un reload ou d'une mise à jour DB.</p>
 */
@Component
public class SiteConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(SiteConfigManager.class);

    private final SiteConfigRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final Environment environment;

    @Value("${lmp.site.url:http://localhost:8080}")
    private String siteUrl;

    @Value("${lmp.site.config.path:}")
    private String externalConfigPath;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private Map<String, String> fileConfig = Collections.emptyMap();
    private Map<String, String> derivedConfig = Collections.emptyMap();

    public SiteConfigManager(SiteConfigRepository repository,
                             ApplicationEventPublisher eventPublisher,
                             Environment environment) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        computeDerivedValues();
        reloadFileConfig();
        warmCache();
        logger.info("SiteConfigManager initialisé — siteUrl={}", siteUrl);
    }

    // ============ Lecture publique ============

    public String getString(String key) {
        return cache.computeIfAbsent(key, this::resolve);
    }

    public String getString(String key, String defaultValue) {
        String value = getString(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Lit un booléen de la config hiérarchique (env → file → DB → défaut).
     * Fail-open : valeur absente OU erreur de lecture ⇒ defaultValue —
     * une panne de config ne doit jamais couper silencieusement une intégration.
     * Toute valeur non nulle autre que {@code "true"} (insensible à la casse)
     * est traitée comme {@code false}.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            String value = getString(key);
            return value == null ? defaultValue : Boolean.parseBoolean(value.trim());
        } catch (RuntimeException e) {
            logger.warn("[SITE-CONFIG] getBoolean({}) en échec — fallback {}", key, defaultValue, e);
            return defaultValue;
        }
    }

    public String getBaseUrl() {
        return getString("app.base.url", siteUrl);
    }

    public String getFrontendUrl() {
        return getString("app.frontend.url", getBaseUrl());
    }

    public String getSiteName() {
        return getString("app.name", "LMP Digital Services");
    }

    public String getSupportEmail() {
        return getString("mail.from.support", "support@" + extractHost(getBaseUrl()));
    }

    public String getContactEmail() {
        return getString("mail.from.contact", "info@" + extractHost(getBaseUrl()));
    }

    public String getNoreplyEmail() {
        return getString("mail.from.noreply", "noreply@" + extractHost(getBaseUrl()));
    }

    public String getCompanyWebsite() {
        return getString("company.website", getBaseUrl());
    }

    public String getOauth2IssuerUri() {
        return getString("app.oauth2.issuer-uri", getBaseUrl());
    }

    public String getCorsAllowedOrigins() {
        return getString("app.cors.allowed-origins", getBaseUrl());
    }

    // ============ Administration ============

    /**
     * Met à jour une valeur en base de données et invalide le cache.
     */
    public void update(String key, String value, String description) {
        SiteConfigEntry entry = repository.findByKey(key)
            .orElse(new SiteConfigEntry(key, value, description));
        entry.setValue(value);
        if (description != null) {
            entry.setDescription(description);
        }
        repository.save(entry);

        // Invalider le cache et publier l'événement
        String oldValue = cache.get(key);
        cache.remove(key);
        eventPublisher.publishEvent(new SiteConfigChangedEvent(this, key, value));

        logger.info("SiteConfig mise à jour — {} : {} -> {}", key, oldValue, value);
    }

    /**
     * Recharge le fichier {@code site-config.json} et invalide le cache.
     */
    public void reloadFileConfig() {
        Map<String, String> previous = new ConcurrentHashMap<>(fileConfig);
        loadFileConfig();

        // Invalider les clés qui ont changé dans le fichier
        for (String key : fileConfig.keySet()) {
            if (!fileConfig.get(key).equals(previous.get(key))) {
                cache.remove(key);
                eventPublisher.publishEvent(new SiteConfigChangedEvent(this, key, fileConfig.get(key)));
            }
        }
        logger.info("site-config.json rechargé — {} clés", fileConfig.size());
    }

    /**
     * Recharge complètement (fichier + DB) et vide le cache.
     */
    public void reloadAll() {
        cache.clear();
        reloadFileConfig();
        warmCache();
        logger.info("SiteConfigManager reload complet effectué");
    }

    public Map<String, String> getAllFromDb() {
        Map<String, String> result = new ConcurrentHashMap<>();
        repository.findAllByOrderByKeyAsc().forEach(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    // ============ Résolution interne ============

    private String resolve(String key) {
        // 1. Variables d'environnement / properties Spring
        String envValue = environment.getProperty(key);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        // 2. Fichier site-config.json
        if (fileConfig.containsKey(key)) {
            return fileConfig.get(key);
        }

        // 3. Base de données — fail-soft : DB pas prête au boot (ou blip réseau)
        //    ne doit pas crasher le démarrage, on retombe sur le tier dérivé.
        try {
            Optional<SiteConfigEntry> dbEntry = repository.findByKey(key);
            if (dbEntry.isPresent() && dbEntry.get().getValue() != null) {
                return dbEntry.get().getValue();
            }
        } catch (RuntimeException e) {
            logger.warn("[SITE-CONFIG] Lecture DB indisponible pour '{}' — fallback dérivé/env : {}",
                    key, e.getMessage());
        }

        // 4. Valeurs dérivées de lmp.site.url
        return derivedConfig.get(key);
    }

    private void computeDerivedValues() {
        Map<String, String> derived = new ConcurrentHashMap<>();
        if (siteUrl == null || siteUrl.isBlank()) {
            this.derivedConfig = derived;
            return;
        }
        siteUrl = siteUrl.replaceAll("/+$", "");
        String host = extractHost(siteUrl);
        boolean isLocal = "localhost".equals(host) || "127.0.0.1".equals(host);
        String hostNoWww = host.startsWith("www.") ? host.substring(4) : host;

        derived.put("app.base.url", siteUrl);
        derived.put("app.frontend.url", siteUrl);
        derived.put("company.website", siteUrl);
        // Single-host monolith : issuer = site URL (auth pages servies sur le même host).
        derived.put("app.oauth2.issuer-uri", siteUrl);
        derived.put("app.cors.allowed-origins",
            isLocal
                ? "http://localhost:*"
                : "https://" + hostNoWww + ",https://www." + hostNoWww);

        derived.put("mail.from.noreply", "noreply@" + host);
        derived.put("mail.from.support", "support@" + host);
        derived.put("mail.replyto.support", "support@" + host);
        derived.put("company.email", "support@" + host);
        derived.put("company.team.email", "support@" + host);
        derived.put("company.admin.email", "admin@" + host);
        derived.put("lmp.sync.alert.admin-email", "admin@" + host);

        this.derivedConfig = Collections.unmodifiableMap(derived);
    }

    private void loadFileConfig() {
        Resource resource = resolveConfigResource();
        if (!resource.exists()) {
            this.fileConfig = Collections.emptyMap();
            return;
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(resource.getInputStream(), new TypeReference<>() {});
            Map<String, String> flattened = new ConcurrentHashMap<>();
            flatten(raw, "", flattened);
            this.fileConfig = Collections.unmodifiableMap(flattened);
        } catch (IOException e) {
            logger.warn("Impossible de charger site-config.json : {}", e.getMessage());
            this.fileConfig = Collections.emptyMap();
        }
    }

    private Resource resolveConfigResource() {
        if (externalConfigPath != null && !externalConfigPath.isBlank()) {
            return new FileSystemResource(externalConfigPath);
        }
        return new ClassPathResource("site-config.json");
    }

    private void flatten(Map<String, Object> source, String prefix, Map<String, String> target) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nested = (Map<String, Object>) map;
                flatten(nested, key, target);
            } else {
                target.put(key, String.valueOf(entry.getValue()));
            }
        }
    }

    private void warmCache() {
        // Pré-charger les clés fréquentes
        String[] keys = {
            "app.base.url", "app.frontend.url", "app.name", "app.oauth2.issuer-uri",
            "company.website", "mail.from.support", "mail.from.noreply",
            "company.email", "company.admin.email", "app.cors.allowed-origins"
        };
        for (String key : keys) {
            cache.put(key, resolve(key));
        }
    }

    private String extractHost(String url) {
        try {
            return URI.create(url).getHost();
        } catch (Exception e) {
            String stripped = url.replaceAll("^https?://", "");
            int slashIdx = stripped.indexOf('/');
            if (slashIdx > 0) stripped = stripped.substring(0, slashIdx);
            int portIdx = stripped.indexOf(':');
            if (portIdx > 0) stripped = stripped.substring(0, portIdx);
            return stripped;
        }
    }
}
