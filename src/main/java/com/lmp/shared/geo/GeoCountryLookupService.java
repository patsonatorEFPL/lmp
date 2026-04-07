package com.lmp.shared.geo;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.lmp.shared.web.ClientIpResolver;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Résout le pays <strong>et la devise</strong> du client depuis son adresse IP.
 *
 * <h3>Cache Caffeine</h3>
 * <ul>
 *   <li><b>Clé</b> : Adresse IP (String)</li>
 *   <li><b>Valeur</b> : {@link GeoResolution} (pays + devise)</li>
 *   <li><b>TTL</b> : Configurable via {@code geoip.cache.ttl-minutes} (défaut 15 min)</li>
 *   <li><b>Taille max</b> : Configurable via {@code geoip.cache.max-size} (défaut 10 000 IPs)</li>
 *   <li><b>Éviction</b> : LRU automatique quand taille max atteinte</li>
 * </ul>
 *
 * <p><b>Note :</b> Changement d'IP = détection immédiate (nouvelle clé = cache miss).
 * Le TTL sert à rafraîchir les données et libérer la mémoire des IPs inactives.
 *
 * <h3>Chaîne de résolution (dans l'ordre)</h3>
 * <ol>
 *   <li><b>Override de test</b> {@code geoip.country-test-override} (dev/test uniquement)</li>
 *   <li><b>Cache Caffeine</b> — lookup IP en cache (hit = pas d'appel API)</li>
 *   <li><b>ipwho.is</b> — HTTPS, gratuit, retourne pays (devise = plan Premium uniquement !)</li>
 *   <li><b>ip-api.com</b> — HTTP (gratuit) ou HTTPS (clé {@code ipapi.com.key}),
 *       retourne pays + devise ; 45 req/min sur le plan gratuit</li>
 *   <li>Si une API retourne le pays sans devise, on garde ce pays en fallback</li>
 *   <li><b>CF-IPCountry</b> — en-tête Cloudflare, pays uniquement (devise = null)</li>
 *   <li><b>GeoLite2</b> — base locale {@code geoip.country-db}, pays uniquement (devise = null)</li>
 *   <li><b>Vide</b> → le service appelant utilise son {@code pricing.fallback-country}</li>
 * </ol>
 *
 * <p><b>Note :</b> ipwho.is plan gratuit ne retourne PAS la devise. ip-api.com est donc
 * prioritaire pour la devise si ipwho.is n'en fournit pas.
 *
 * <p>Quand seul le pays est disponible, c'est {@link com.lmp.shared.pricing.RegionalPricingService}
 * qui gère le fallback devise (configuration {@code pricing.region} ou devise de base EUR).
 */
@Service
public class GeoCountryLookupService {

    private static final Logger logger = LoggerFactory.getLogger(GeoCountryLookupService.class);

    private final Environment environment;
    private final DatabaseReader databaseReader;
    private final IpWhoIsGeoService ipWhoIsGeoService;
    private final IpApiComGeoService ipApiComGeoService;
    private final Cache<String, GeoResolution> geoIpCache;

    public GeoCountryLookupService(
            Environment environment,
            @Value("${geoip.country-db:}") String geoDbPath,
            IpWhoIsGeoService ipWhoIsGeoService,
            IpApiComGeoService ipApiComGeoService,
            Cache<String, GeoResolution> geoIpCache) {

        this.environment = environment;
        this.ipWhoIsGeoService = ipWhoIsGeoService;
        this.ipApiComGeoService = ipApiComGeoService;
        this.geoIpCache = geoIpCache;

        DatabaseReader reader = null;
        if (geoDbPath != null && !geoDbPath.isBlank()) {
            try {
                File f = new File(geoDbPath.trim());
                if (f.isFile()) {
                    reader = new DatabaseReader.Builder(f).build();
                    logger.info("[GeoIP] GeoLite2 database loaded: {}", f.getAbsolutePath());
                } else {
                    logger.warn("[GeoIP] GeoLite2 path configured but file not found: {}", f.getAbsolutePath());
                }
            } catch (IOException e) {
                logger.warn("[GeoIP] Could not open GeoLite2 database: {}", e.getMessage());
            }
        }
        this.databaseReader = reader;
    }

    /**
     * Résout le pays et, si possible, la devise du client.
     * Utilise le cache Caffeine pour éviter les appels API répétés sur la même IP.
     *
     * @return {@link GeoResolution} ou vide si aucune source ne répond.
     */
    public Optional<GeoResolution> resolve(HttpServletRequest request) {
        if (request == null) {
            return Optional.empty();
        }

        // ── 1. Override de test/dev ────────────────────────────────────────────
        // Formats acceptés : "SN" (pays seul) ou "SN:XOF" (pays + devise)
        String testOverride = environment.getProperty("geoip.country-test-override");
        if (testOverride != null && !testOverride.isBlank()) {
            String[] parts = testOverride.trim().toUpperCase().split(":");
            String country = parts[0];
            String currency = parts.length > 1 ? parts[1] : null;
            return Optional.of(new GeoResolution(country, currency));
        }

        String ip = ClientIpResolver.resolve(request);
        if (ip == null || ip.isBlank()) {
            return Optional.empty();
        }

        // ── 2. Cache Caffeine — évite les appels API répétés ──────────────────
        GeoResolution cached = geoIpCache.getIfPresent(ip);
        if (cached != null) {
            return Optional.of(cached);
        }

        // ── 3. ipwho.is — HTTPS, gratuit, retourne pays (devise = plan Premium uniquement) ─
        Optional<GeoResolution> ipWhoIsResult = ipWhoIsGeoService.lookup(ip);
        if (ipWhoIsResult.isPresent() && ipWhoIsResult.get().currencyCode() != null) {
            // ipwho.is a retourné pays ET devise → on utilise ce résultat
            GeoResolution result = ipWhoIsResult.get();
            geoIpCache.put(ip, result);
            return ipWhoIsResult;
        }

        // ── 4. ip-api.com — retourne pays + devise ────────────────────────────
        // Prioritaire si ipwho.is n'a pas fourni de devise (plan gratuit ipwho.is)
        Optional<GeoResolution> ipApiResult = ipApiComGeoService.lookup(ip);
        if (ipApiResult.isPresent() && ipApiResult.get().currencyCode() != null) {
            GeoResolution result = ipApiResult.get();
            geoIpCache.put(ip, result);
            return ipApiResult;
        }

        // Si ipwho.is avait un pays (sans devise), on le garde en fallback
        if (ipWhoIsResult.isPresent()) {
            GeoResolution result = ipWhoIsResult.get();
            geoIpCache.put(ip, result);
            return ipWhoIsResult;
        }

        // Si ip-api.com avait un pays (sans devise), on le garde en fallback
        if (ipApiResult.isPresent()) {
            GeoResolution result = ipApiResult.get();
            geoIpCache.put(ip, result);
            return ipApiResult;
        }

        // ── 5. En-tête Cloudflare (pays uniquement, devise non fournie) ───────
        String cf = request.getHeader("CF-IPCountry");
        if (cf != null && !cf.isBlank() && !"XX".equalsIgnoreCase(cf)) {
            GeoResolution result = GeoResolution.countryOnly(cf.trim().toUpperCase());
            geoIpCache.put(ip, result);
            return Optional.of(result);
        }

        // ── 6. Base GeoLite2 locale (pays uniquement, devise non fournie) ─────
        if (databaseReader != null && ip != null && !ip.isBlank()) {
            try {
                InetAddress addr = InetAddress.getByName(ip);
                if (!addr.isLoopbackAddress() && !addr.isAnyLocalAddress()) {
                    String code = databaseReader.country(addr).getCountry().getIsoCode();
                    if (code != null && !code.isBlank()) {
                        GeoResolution result = GeoResolution.countryOnly(code);
                        geoIpCache.put(ip, result);
                        return Optional.of(result);
                    }
                }
            } catch (IOException | GeoIp2Exception e) {
                logger.debug("[GeoIP] GeoLite2 lookup failed for {}: {}", ip, e.getMessage());
            }
        }

        return Optional.empty();
    }

    /**
     * Rétrocompatibilité : retourne uniquement le code pays.
     *
     * @deprecated Préférer {@link #resolve(HttpServletRequest)} qui fournit aussi la devise.
     */
    @Deprecated(forRemoval = true)
    public Optional<String> lookupCountryCode(HttpServletRequest request) {
        return resolve(request).map(GeoResolution::countryCode);
    }
}
