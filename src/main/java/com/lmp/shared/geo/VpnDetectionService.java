package com.lmp.shared.geo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestre la détection VPN/proxy via plusieurs sources en parallèle.
 *
 * <p>Sources utilisées :
 * <ol>
 *   <li><b>ip-api.com</b> — champ {@code proxy} (booléen)</li>
 *   <li><b>GetIPIntel</b> — score probabiliste 0–1</li>
 *   <li><b>IPHub</b> — flag block 0/1/2</li>
 * </ol>
 *
 * <p>Retourne un {@link VpnCheckResult} avec un score normalisé 0.0–1.0
 * et la liste des sources consultées avec leurs résultats.
 */
@Service
public class VpnDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(VpnDetectionService.class);

    private final IpApiComGeoService ipApiComService;
    private final GetIPIntelService getIPIntelService;
    private final IPHubService ipHubService;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    /** Timeout global pour les appels parallèles. */
    private static final long TIMEOUT_SECONDS = 6;

    public VpnDetectionService(IpApiComGeoService ipApiComService,
                                GetIPIntelService getIPIntelService,
                                IPHubService ipHubService) {
        this.ipApiComService = ipApiComService;
        this.getIPIntelService = getIPIntelService;
        this.ipHubService = ipHubService;
    }

    /**
     * Résultat de la détection VPN multi-sources.
     *
     * @param normalizedScore Score consensus 0.0 (clean) → 1.0 (VPN certain)
     * @param vpnDetected     {@code true} si le score dépasse le seuil de 0.6
     * @param sources         Description textuelle des résultats par source (pour audit)
     */
    public record VpnCheckResult(
            double normalizedScore,
            boolean vpnDetected,
            String sources
    ) {}

    /**
     * Lance la détection VPN en parallèle sur les 3 sources et retourne un score consensus.
     *
     * @param ip adresse IP à vérifier
     * @return résultat agrégé
     */
    public VpnCheckResult check(String ip) {
        logger.debug("[FRAUD-DEBUG] VpnDetectionService.check() → IP={}", ip);

        // ── Lancer les 3 sources en parallèle ────────────────────────────────
        CompletableFuture<Optional<Boolean>> ipApiFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Optional<GeoResolution> res = ipApiComService.lookup(ip);
                return res.map(GeoResolution::vpnDetected);
            } catch (Exception e) {
                logger.warn("[FRAUD-DEBUG] ip-api.com async error: {}", e.getMessage());
                return Optional.empty();
            }
        }, executor);

        CompletableFuture<Optional<Double>> getIPIntelFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return getIPIntelService.lookupVpnScore(ip);
            } catch (Exception e) {
                logger.warn("[FRAUD-DEBUG] GetIPIntel async error: {}", e.getMessage());
                return Optional.empty();
            }
        }, executor);

        CompletableFuture<Optional<IPHubService.IPHubResult>> ipHubFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return ipHubService.lookup(ip);
            } catch (Exception e) {
                logger.warn("[FRAUD-DEBUG] IPHub async error: {}", e.getMessage());
                return Optional.empty();
            }
        }, executor);

        // ── Attendre tous les résultats avec timeout ─────────────────────────
        Optional<Boolean> ipApiResult = Optional.empty();
        Optional<Double> getIPIntelResult = Optional.empty();
        Optional<IPHubService.IPHubResult> ipHubResult = Optional.empty();

        try {
            CompletableFuture.allOf(ipApiFuture, getIPIntelFuture, ipHubFuture)
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("[FRAUD-DEBUG] VPN detection timeout or error: {}", e.getMessage());
        }

        try { ipApiResult = ipApiFuture.getNow(Optional.empty()); } catch (Exception ignored) {}
        try { getIPIntelResult = getIPIntelFuture.getNow(Optional.empty()); } catch (Exception ignored) {}
        try { ipHubResult = ipHubFuture.getNow(Optional.empty()); } catch (Exception ignored) {}

        // ── Calculer le score consensus pondéré ──────────────────────────────
        double totalWeight = 0;
        double weightedSum = 0;
        List<String> sourceParts = new ArrayList<>();

        // ip-api.com (poids 0.25 — booléen simple, moins nuancé)
        if (ipApiResult.isPresent()) {
            boolean proxy = ipApiResult.get();
            double score = proxy ? 1.0 : 0.0;
            totalWeight += 0.25;
            weightedSum += score * 0.25;
            sourceParts.add("ip-api:" + proxy);
            logger.debug("[FRAUD-DEBUG] ip-api.com contribution: proxy={} → score={} weight=0.25", proxy, score);
        }

        // GetIPIntel (poids 0.40 — score probabiliste, le plus précis)
        if (getIPIntelResult.isPresent()) {
            double score = getIPIntelResult.get();
            totalWeight += 0.40;
            weightedSum += score * 0.40;
            sourceParts.add("getipintel:" + String.format("%.3f", score));
            logger.debug("[FRAUD-DEBUG] GetIPIntel contribution: score={} weight=0.40", score);
        }

        // IPHub (poids 0.35 — block flag fiable)
        if (ipHubResult.isPresent()) {
            int block = ipHubResult.get().block();
            double score = switch (block) {
                case 1 -> 1.0;   // Non-résidentiel (VPN/proxy)
                case 2 -> 0.6;   // Warning (peut être résidentiel)
                default -> 0.0;  // Résidentiel (safe)
            };
            totalWeight += 0.35;
            weightedSum += score * 0.35;
            sourceParts.add("iphub:" + block);
            logger.debug("[FRAUD-DEBUG] IPHub contribution: block={} → score={} weight=0.35", block, score);
        }

        // Score normalisé : si aucune source n'a répondu, score = 0
        double normalizedScore = totalWeight > 0 ? weightedSum / totalWeight : 0.0;
        boolean vpnDetected = normalizedScore > 0.6;
        String sources = String.join(",", sourceParts);

        logger.info("[FRAUD-DEBUG] VPN check result for {} → normalizedScore={} vpnDetected={} sources={}",
                ip, String.format("%.3f", normalizedScore), vpnDetected, sources);

        return new VpnCheckResult(normalizedScore, vpnDetected, sources);
    }
}
