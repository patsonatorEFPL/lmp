package com.lmp.shared.monitoring;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.lmp.shared.geo.GetIPIntelService;
import com.lmp.shared.geo.IPHubService;
import com.lmp.shared.geo.IpApiComGeoService;
import com.lmp.shared.geo.IpWhoIsGeoService;
import com.lmp.shared.vat.ViesVatValidationService;

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
    private final ApiHealthRecorder recorder;
    private final RestTemplate fxProbeTemplate;

    /** Frankfurter (même URL que FxRateCacheService). */
    private static final String FX_PROBE_URL =
            "https://api.frankfurter.app/latest?from=EUR&to=USD";

    public ApiHealthProbeService(IpApiComGeoService ipApiComGeoService,
                                  IpWhoIsGeoService ipWhoIsGeoService,
                                  GetIPIntelService getIPIntelService,
                                  IPHubService ipHubService,
                                  ViesVatValidationService viesService,
                                  ApiHealthRecorder recorder) {
        this.ipApiComGeoService = ipApiComGeoService;
        this.ipWhoIsGeoService = ipWhoIsGeoService;
        this.getIPIntelService = getIPIntelService;
        this.ipHubService = ipHubService;
        this.viesService = viesService;
        this.recorder = recorder;
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

        // Mailtrap — pas de probe sûr sans envoyer un email
        results.put("Mailtrap", "skipped");

        // Stripe — pas de probe sûr sans clé / appel facturable
        results.put("Stripe", "skipped");

        logger.info("[API-PROBE] Probe terminé : {}", results);
        return results;
    }

    private String probeQuietly(String name, Runnable task) {
        try {
            task.run();
            return "ok";
        } catch (Exception e) {
            logger.warn("[API-PROBE] {} failed: {}", name, e.getMessage());
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
}
