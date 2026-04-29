package com.lmp.shared.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

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
        putIfAbsent(environment, derived, "app.frontend.url", siteUrl);
        putIfAbsent(environment, derived, "company.website", siteUrl);

        // OAuth2 issuer : sous-domaine "auth.<host>" en non-local, sinon siteUrl
        // Permet d'avoir auth.lmp-services.ca en prod et auth.dev.lmp-services.ca en test
        // sans rien configurer manuellement.
        String issuerUri = isLocal ? siteUrl : "https://auth." + hostNoWww;
        putIfAbsent(environment, derived, "app.oauth2.issuer-uri", issuerUri);

        // CORS : en localhost tout port permis ; sinon root + www + auth subdomain
        String corsOrigins;
        if (isLocal) {
            corsOrigins = "http://localhost:*";
        } else {
            corsOrigins = "https://" + hostNoWww
                    + ",https://www." + hostNoWww
                    + ",https://auth." + hostNoWww;
        }
        putIfAbsent(environment, derived, "app.cors.allowed-origins", corsOrigins);

        // Frappe / ERPNext callback : dérivé de lmp.crm.url (par défaut crm.lmp-services.ca)
        // permet à un environnement isolé de pointer vers une autre instance Frappe.
        String crmUrl = environment.getProperty("lmp.crm.url");
        if (crmUrl != null && !crmUrl.isBlank()) {
            crmUrl = crmUrl.replaceAll("/+$", "");
            putIfAbsent(environment, derived, "app.oauth2.erp.redirect-uri",
                    crmUrl + "/api/method/frappe.integrations.oauth2_logins.custom/lmp_sso");
        }

        // Emails dérivés du host
        putIfAbsent(environment, derived, "mail.from.noreply", "noreply@" + host);
        putIfAbsent(environment, derived, "mail.from.support", "support@" + host);
        putIfAbsent(environment, derived, "mail.replyto.support", "support@" + host);

        putIfAbsent(environment, derived, "company.email", "support@" + host);
        putIfAbsent(environment, derived, "company.team.email", "support@" + host);
        putIfAbsent(environment, derived, "company.admin.email", "admin@" + host);

        putIfAbsent(environment, derived, "lmp.sync.alert.admin-email", "admin@" + host);

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
