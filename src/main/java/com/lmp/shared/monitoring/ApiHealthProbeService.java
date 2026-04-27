package com.lmp.shared.monitoring;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.lmp.integration.sync.SyncProperties;
import com.lmp.shared.geo.GetIPIntelService;
import com.lmp.shared.geo.IPHubService;
import com.lmp.shared.geo.IpApiComGeoService;
import com.lmp.shared.geo.IpWhoIsGeoService;
import com.lmp.shared.vat.ViesVatValidationService;
import org.springframework.beans.factory.annotation.Value;

import com.stripe.StripeClient;
import com.stripe.param.BalanceRetrieveParams;

/**
 * Exécute des probes légers vers chaque API externe pour alimenter
 * le {@link ApiHealthRecorder} à la demande (déclenchement admin).
 *
 * <p>Chaque probe utilise un appel minimaliste (IP publique connue,
 * TVA connue valide) pour vérifier la connectivité sans effet de bord.
 */
@Service
public class ApiHealthProbeService {

    private static final Logger logger = LoggerFactory.getLogger(ApiHealthProbeService.class);

    /** IP publique bien connue — Google DNS. */
    private static final String PROBE_IP = "8.8.8.8";

    /** TVA valide connue — Commission européenne / Parlement européen. */
    private static final String PROBE_VAT = "BE0367302178";

    private final IpApiComGeoService ipApiComGeoService;
    private final IpWhoIsGeoService ipWhoIsGeoService;
    private final GetIPIntelService getIPIntelService;
    private final IPHubService ipHubService;
    private final ViesVatValidationService viesService;
    private final StripeClient stripeClient;
    private final ApiHealthRecorder recorder;
    private final RestTemplate fxProbeTemplate;
    private final SyncProperties syncProperties;

    @Value("${mailtrap.api.token:}")
    private String mailtrapApiToken;

    /** Frankfurter (même URL que FxRateCacheService). */
    private static final String FX_PROBE_URL =
            "https://api.frankfurter.app/latest?from=EUR&to=USD";

    /** Mailtrap accounts endpoint — lecture seule, gratuit. */
    private static final String MAILTRAP_ACCOUNTS_URL =
            "https://mailtrap.io/api/accounts";

    public ApiHealthProbeService(IpApiComGeoService ipApiComGeoService,
                                  IpWhoIsGeoService ipWhoIsGeoService,
                                  GetIPIntelService getIPIntelService,
                                  IPHubService ipHubService,
                                  ViesVatValidationService viesService,
                                  StripeClient stripeClient,
                                  ApiHealthRecorder recorder,
                                  SyncProperties syncProperties) {
        this.ipApiComGeoService = ipApiComGeoService;
        this.ipWhoIsGeoService = ipWhoIsGeoService;
        this.getIPIntelService = getIPIntelService;
        this.ipHubService = ipHubService;
        this.viesService = viesService;
        this.stripeClient = stripeClient;
        this.recorder = recorder;
        this.syncProperties = syncProperties;
        this.fxProbeTemplate = new RestTemplateBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Lance un probe léger vers chaque API externe.
     *
     * @return Map API name → "ok" ou message d'erreur.
     */
    public Map<String, String> probeAll() {
        Map<String, String> results = new LinkedHashMap<>();

        // ip-api.com
        results.put("ip-api.com", probeQuietly("ip-api.com", () ->
                ipApiComGeoService.lookup(PROBE_IP)));

        // ipwho.is
        results.put("ipwho.is", probeQuietly("ipwho.is", () ->
                ipWhoIsGeoService.lookup(PROBE_IP)));

        // GetIPIntel
        results.put("GetIPIntel", probeQuietly("GetIPIntel", () ->
                getIPIntelService.lookupVpnScore(PROBE_IP)));

        // IPHub
        results.put("IPHub", probeQuietly("IPHub", () ->
                ipHubService.lookup(PROBE_IP)));

        // VIES
        results.put("VIES", probeQuietly("VIES", () ->
                viesService.validate(PROBE_VAT)));

        // FX Rates — probe direct car le cache ne re-fetch pas à chaque appel
        results.put("FX Rates", probeFxRates());

        // Mailtrap — probe via GET /api/accounts (lecture seule)
        results.put("Mailtrap", probeMailtrap());

        // Stripe — balance.retrieve() est gratuit et en lecture seule
        results.put("Stripe", probeStripe());

        // Sentry — heartbeat léger via le SDK
        results.put("Sentry", probeSentry());

        logger.info("[API-PROBE] Probe terminé : {}", results);
        return results;
    }

    private String probeQuietly(String name, Runnable task) {
        long t0 = System.currentTimeMillis();
        try {
            task.run();
            long latency = System.currentTimeMillis() - t0;
            // Enregistre le probe si le service n'a pas déjà enregistré
            // (certains services sortent tôt si non configurés, sans recorder.record)
            recorder.record(name, latency, true, null);
            return "ok";
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - t0;
            recorder.record(name, latency, false, e.getMessage());
            logger.warn("[API-PROBE] {} failed: {}", name, e.getMessage());
            return e.getMessage();
        }
    }

    /**
     * Probe Stripe via balance.retrieve() — gratuit, lecture seule.
     * Vérifie la connectivité réseau + validité de la clé API.
     */
    private String probeStripe() {
        long t0 = System.currentTimeMillis();
        try {
            stripeClient.balance().retrieve(BalanceRetrieveParams.builder().build());
            long latency = System.currentTimeMillis() - t0;
            recorder.record("Stripe", latency, true, null);
            return "ok";
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - t0;
            recorder.record("Stripe", latency, false, e.getMessage());
            logger.warn("[API-PROBE] Stripe failed: {}", e.getMessage());
            return e.getMessage();
        }
    }

    /**
     * Probe Mailtrap via GET /api/accounts — lecture seule, gratuit.
     * Vérifie la connectivité réseau + validité du token API.
     */
    private String probeMailtrap() {
        if (mailtrapApiToken == null || mailtrapApiToken.isBlank()) {
            recorder.record("Mailtrap", 0, false, "Token API non configuré");
            return "Token API non configuré";
        }
        long t0 = System.currentTimeMillis();
        try {
            var headers = new org.springframework.http.HttpHeaders();
            headers.set("Api-Token", mailtrapApiToken);
            headers.set("Accept", "application/json");
            var entity = new org.springframework.http.HttpEntity<>(null, headers);
            fxProbeTemplate.exchange(MAILTRAP_ACCOUNTS_URL,
                    org.springframework.http.HttpMethod.GET, entity, String.class);
            long latency = System.currentTimeMillis() - t0;
            recorder.record("Mailtrap", latency, true, null);
            return "ok";
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - t0;
            recorder.record("Mailtrap", latency, false, e.getMessage());
            logger.warn("[API-PROBE] Mailtrap failed: {}", e.getMessage());
            return e.getMessage();
        }
    }

    private String probeFxRates() {
        long t0 = System.currentTimeMillis();
        try {
            var resp = fxProbeTemplate.getForObject(FX_PROBE_URL, String.class);
            long latency = System.currentTimeMillis() - t0;
            if (resp != null && !resp.isBlank()) {
                recorder.record("FX Rates", latency, true, null);
                return "ok";
            }
            recorder.record("FX Rates", latency, false, "empty response");
            return "empty response";
        } catch (Exception e) {
            recorder.record("FX Rates", System.currentTimeMillis() - t0, false, e.getMessage());
            return e.getMessage();
        }
    }

    private String probeSentry() {
        String dsn = syncProperties.getAlert().getSentryDsn();
        if (dsn == null || dsn.isBlank()) {
            recorder.record("Sentry", 0, false, "DSN non configuré");
            return "DSN non configuré";
        }
        long t0 = System.currentTimeMillis();
        try {
            io.sentry.SentryEvent event = new io.sentry.SentryEvent();
            event.setLevel(io.sentry.SentryLevel.INFO);
            io.sentry.protocol.Message message = new io.sentry.protocol.Message();
            message.setFormatted("[PROBE] Sentry connectivity test");
            event.setMessage(message);
            event.setTag("probe", "true");
            io.sentry.Sentry.captureEvent(event);
            long latency = System.currentTimeMillis() - t0;
            recorder.record("Sentry", latency, true, null);
            return "ok";
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - t0;
            recorder.record("Sentry", latency, false, e.getMessage());
            logger.warn("[API-PROBE] Sentry failed: {}", e.getMessage());
            return e.getMessage();
        }
    }
}
