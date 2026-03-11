package com.lmp.auth.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Service de blocklist des emails jetables (disposable).
 * Charge une liste locale en fallback, puis tente de la mettre à jour
 * depuis GitHub toutes les 24h avec support ETag HTTP.
 */
@Component
public class DisposableEmailBlocklist {

    private static final Logger logger = LoggerFactory.getLogger(DisposableEmailBlocklist.class);

    private static final String REMOTE_URL =
            "https://raw.githubusercontent.com/ali-master/disposable-email-domains/master/data/domains.txt";
    private static final String LOCAL_FALLBACK = "disposable_email_blocklist.conf";
    private static final int CONNECT_TIMEOUT = 10_000;
    private static final int READ_TIMEOUT = 15_000;

    private volatile Set<String> blockedDomains = Collections.emptySet();
    private volatile String lastEtag = null;

    /**
     * Liste blanche de domaines légitimes qui ne doivent JAMAIS être bloqués,
     * même s'ils apparaissent dans une blocklist distante mal entretenue.
     */
    private static final Set<String> SAFELIST = Set.of(
            "gmail.com", "googlemail.com",
            "yahoo.com", "yahoo.fr", "yahoo.ca",
            "hotmail.com", "hotmail.fr", "hotmail.ca",
            "outlook.com", "outlook.fr",
            "live.com", "live.fr", "live.ca",
            "msn.com",
            "icloud.com", "me.com", "mac.com",
            "protonmail.com", "proton.me", "pm.me",
            "aol.com",
            "zoho.com",
            "yandex.com", "yandex.ru",
            "mail.com",
            "gmx.com", "gmx.fr",
            "fastmail.com",
            "tutanota.com", "tuta.io",
            "hey.com",
            "videotron.ca", "bell.net", "rogers.com", "shaw.ca", "telus.net",
            "sympatico.ca", "cogeco.ca",
            "orange.fr", "free.fr", "sfr.fr", "laposte.net", "wanadoo.fr",
            "bluewin.ch",
            "gmx.de", "web.de", "t-online.de"
    );

    @PostConstruct
    public void init() {
        // 1. Charger le fichier local en fallback
        loadLocalFallback();
        // 2. Tenter un fetch HTTP pour la version distante
        refreshFromRemote();
        logger.info("DisposableEmailBlocklist initialisée avec {} domaines bloqués", blockedDomains.size());
    }

    /**
     * Rafraîchit la blocklist toutes les 24h via HTTP ETag.
     */
    @Scheduled(fixedRate = 86_400_000) // 24h
    public void scheduledRefresh() {
        refreshFromRemote();
    }

    /**
     * Vérifie si un domaine email est jetable.
     *
     * @param email l'adresse email complète
     * @return true si le domaine est dans la blocklist
     */
    public boolean isDisposable(String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }
        String domain = email.substring(email.lastIndexOf('@') + 1).toLowerCase().trim();
        // Ne jamais bloquer les domaines légitimes connus
        if (SAFELIST.contains(domain)) {
            return false;
        }
        return blockedDomains.contains(domain);
    }

    /**
     * Retourne le nombre de domaines bloqués (utile pour les tests/monitoring).
     */
    public int size() {
        return blockedDomains.size();
    }

    private void loadLocalFallback() {
        try {
            ClassPathResource resource = new ClassPathResource(LOCAL_FALLBACK);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    Set<String> domains = reader.lines()
                            .map(String::trim)
                            .map(String::toLowerCase)
                            .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                            .collect(Collectors.toSet());
                    blockedDomains = Collections.unmodifiableSet(domains);
                    logger.info("Blocklist locale chargée : {} domaines", domains.size());
                }
            } else {
                logger.warn("Fichier blocklist local introuvable : {}", LOCAL_FALLBACK);
            }
        } catch (IOException e) {
            logger.error("Erreur lors du chargement de la blocklist locale : {}", e.getMessage());
        }
    }

    private void refreshFromRemote() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(REMOTE_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);

            // Envoyer le ETag si on en a un
            if (lastEtag != null) {
                conn.setRequestProperty("If-None-Match", lastEtag);
            }

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                logger.debug("Blocklist distante inchangée (304 Not Modified)");
                return;
            }

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    Set<String> newDomains = reader.lines()
                            .map(String::trim)
                            .map(String::toLowerCase)
                            .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                            .collect(Collectors.toCollection(HashSet::new));

                    if (!newDomains.isEmpty()) {
                        blockedDomains = Collections.unmodifiableSet(newDomains);
                        // Stocker le ETag
                        String etag = conn.getHeaderField("ETag");
                        if (etag != null) {
                            lastEtag = etag;
                        }
                        logger.info("Blocklist distante mise à jour : {} domaines", newDomains.size());
                    }
                }
            } else {
                logger.warn("Réponse inattendue lors du fetch blocklist : HTTP {}", responseCode);
            }

        } catch (Exception e) {
            logger.warn("Impossible de rafraîchir la blocklist distante (conserve la liste actuelle) : {}", e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
