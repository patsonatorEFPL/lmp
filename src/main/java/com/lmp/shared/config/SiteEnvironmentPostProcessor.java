package com.lmp.shared.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Post-processor qui dérive automatiquement les propriétés du site
 * depuis une seule source de vérité : {@code lmp.site.url}.
 *
 * <p>Inspiré du modèle "Site" de Frappe/ERPNext : une seule URL de site
 * suffit à déduire le frontend, les emails, le CORS, etc.</p>
 *
 * <p>Les propriétés explicitement définies par l'utilisateur (variable d'env
 * ou fichier de config) conservent toujours la priorité.</p>
 */
public class SiteEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SITE_URL_KEY = "lmp.site.url";
    private static final String DERIVED_SOURCE_NAME = "lmp.site.derived";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String siteUrl = environment.getProperty(SITE_URL_KEY);
        if (siteUrl == null || siteUrl.isBlank()) {
            return;
        }

        // Normaliser : supprimer le slash terminal
        siteUrl = siteUrl.replaceAll("/+$", "");

        String host = extractHost(siteUrl);
        if (host == null || host.isBlank()) {
            return;
        }

        boolean isLocal = "localhost".equals(host) || "127.0.0.1".equals(host);
        String hostNoWww = host.startsWith("www.") ? host.substring(4) : host;

        Map<String, Object> derived = new HashMap<>();

        // URLs applicatives
        putIfAbsent(environment, derived, "app.base.url", siteUrl);
        putIfAbsent(environment, derived, "company.website", siteUrl);
        // app.frontend.url volontairement non-dérivé : sémantique "URL externe du
        // frontend en mode split (Angular SSR séparé)". Mode monolithique = vide.
        // Profil dev override avec http://localhost:4200, prod/staging laisse vide.

        // OAuth2 issuer : sous-domaine auth single-level pour rester couvert par
        // Cloudflare Universal SSL (gratuit). Convention :
        //   - root domain (lmp-services.ca)        → auth.lmp-services.ca
        //   - sub-domain (dev.lmp-services.ca)     → auth-dev.lmp-services.ca
        // Multi-level wildcards (auth.dev.*) nécessiteraient ACM payant — évité.
        String authHost = isLocal ? host : deriveAuthHost(hostNoWww);
        String issuerUri = isLocal ? siteUrl : "https://" + authHost;
        putIfAbsent(environment, derived, "app.oauth2.issuer-uri", issuerUri);

        // CORS : en localhost tout port permis ; sinon root + www + auth subdomain
        String corsOrigins;
        if (isLocal) {
            corsOrigins = "http://localhost:*";
        } else {
            corsOrigins = "https://" + hostNoWww
                    + ",https://www." + hostNoWww
                    + ",https://" + authHost;
        }
        putIfAbsent(environment, derived, "app.cors.allowed-origins", corsOrigins);

        // Cookie domain — partage de session cross-subdomain (auth.* ↔ dev.* ↔ apex).
        // Sur localhost, NE PAS définir (browsers rejettent Domain=localhost).
        if (!isLocal) {
            String rootDomain = extractRootDomain(hostNoWww);
            putIfAbsent(environment, derived, "server.servlet.session.cookie.domain", rootDomain);
        }

        // Frappe / ERPNext : URL dérivée comme "crm.<host>" en non-local, sinon localhost:8000.
        // Override possible via LMP_CRM_URL env var (Q1A : pointer vers Frappe partagée
        // depuis staging quand crm.dev.* n'existe pas).
        String crmUrl = environment.getProperty("lmp.crm.url");
        if (crmUrl == null || crmUrl.isBlank()) {
            crmUrl = isLocal ? "http://localhost:8000" : "https://crm." + hostNoWww;
            putIfAbsent(environment, derived, "lmp.crm.url", crmUrl);
        } else {
            crmUrl = crmUrl.replaceAll("/+$", "");
        }
        putIfAbsent(environment, derived, "app.oauth2.erp.redirect-uri",
                crmUrl + "/api/method/frappe.integrations.oauth2_logins.custom/lmp_sso");

        // Email domain — Q2B : utilise le ROOT domain pour les From headers (SPF/DKIM/
        // DMARC sont configurés sur la zone parente, pas sur les subdomains comme
        // dev.* ou auth-dev.*). Override via MAIL_DOMAIN env var possible.
        // Le contenu des emails (liens) utilise app.base.url séparément — ainsi un email
        // envoyé depuis staging contient des liens vers dev.* mais part de
        // noreply@lmp-services.ca (domaine vérifié auprès de Mailtrap/SES/etc.).
        String mailDomain = environment.getProperty("mail.domain");
        if (mailDomain == null || mailDomain.isBlank()) {
            mailDomain = isLocal ? "localhost" : extractRootDomain(hostNoWww);
            putIfAbsent(environment, derived, "mail.domain", mailDomain);
        }
        putIfAbsent(environment, derived, "mail.from.noreply", "noreply@" + mailDomain);
        putIfAbsent(environment, derived, "mail.from.support", "support@" + mailDomain);
        putIfAbsent(environment, derived, "mail.replyto.support", "support@" + mailDomain);

        putIfAbsent(environment, derived, "company.email", "support@" + mailDomain);
        putIfAbsent(environment, derived, "company.team.email", "support@" + mailDomain);
        putIfAbsent(environment, derived, "company.admin.email", "admin@" + mailDomain);

        putIfAbsent(environment, derived, "lmp.sync.alert.admin-email", "admin@" + mailDomain);

        if (!derived.isEmpty()) {
            MutablePropertySources propertySources = environment.getPropertySources();
            // Supprimer l'ancienne source dérivée si elle existe (hot-reload)
            if (propertySources.contains(DERIVED_SOURCE_NAME)) {
                propertySources.remove(DERIVED_SOURCE_NAME);
            }
            // Insérer juste avant les properties système pour que les vraies
            // variables d'environnement gardent la priorité
            propertySources.addLast(new MapPropertySource(DERIVED_SOURCE_NAME, derived));
        }
    }

    private void putIfAbsent(ConfigurableEnvironment env, Map<String, Object> map, String key, String value) {
        if (!env.containsProperty(key)) {
            map.put(key, value);
        }
    }

    /**
     * Dérive l'host auth single-level pour rester sous Cloudflare Universal SSL.
     * <ul>
     *   <li>{@code lmp-services.ca}      → {@code auth.lmp-services.ca}</li>
     *   <li>{@code dev.lmp-services.ca}  → {@code auth-dev.lmp-services.ca}</li>
     *   <li>{@code staging.lmpeo.com}    → {@code auth-staging.lmpeo.com}</li>
     * </ul>
     * On part du host SANS www. Si 2 labels → root domain → préfixe "auth.".
     * Si plus → premier label = env, rest = root → "auth-{env}.{root}".
     */
    /**
     * Root domain = 2 derniers labels (heuristique simple pour .ca/.com/etc.).
     * Note : ne gère pas Public Suffix List (.co.uk, .com.br) — étendre si besoin.
     */
    static String extractRootDomain(String hostNoWww) {
        if (hostNoWww == null || hostNoWww.isBlank()) {
            return hostNoWww;
        }
        String[] labels = hostNoWww.split("\\.");
        if (labels.length <= 2) {
            return hostNoWww;
        }
        return labels[labels.length - 2] + "." + labels[labels.length - 1];
    }

    static String deriveAuthHost(String hostNoWww) {
        if (hostNoWww == null || hostNoWww.isBlank()) {
            return hostNoWww;
        }
        String[] labels = hostNoWww.split("\\.");
        if (labels.length <= 2) {
            return "auth." + hostNoWww;
        }
        String firstLabel = labels[0];
        String rest = hostNoWww.substring(firstLabel.length() + 1);
        return "auth-" + firstLabel + "." + rest;
    }

    private String extractHost(String url) {
        try {
            return URI.create(url).getHost();
        } catch (Exception e) {
            // Fallback manuel si URI échoue
            String stripped = url.replaceAll("^https?://", "");
            int slashIdx = stripped.indexOf('/');
            if (slashIdx > 0) {
                stripped = stripped.substring(0, slashIdx);
            }
            int portIdx = stripped.indexOf(':');
            if (portIdx > 0) {
                stripped = stripped.substring(0, portIdx);
            }
            return stripped;
        }
    }
}
