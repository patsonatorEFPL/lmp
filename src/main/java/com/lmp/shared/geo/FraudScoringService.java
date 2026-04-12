package com.lmp.shared.geo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service de scoring anti-fraude TVA.
 *
 * <p>Calcule un score de fiabilité 0–100 (100 = fiable) en croisant plusieurs signaux :
 * IP country, VPN detection, timezone, geolocation, billing address, card country.
 *
 * <p><b>Ce service ne bloque jamais un paiement.</b> Il produit un score informatif
 * pour alerter l'admin sur les commandes suspectes.
 *
 * <p>Architecture extensible pour l'auto-reverse charge futur.
 */
@Service
public class FraudScoringService {

    private static final Logger logger = LoggerFactory.getLogger(FraudScoringService.class);

    /** Seuil d'alerte admin. */
    public static final int ALERT_THRESHOLD = 60;

    /**
     * Résultat du scoring.
     *
     * @param score  0–100 (100 = fiable)
     * @param flags  Liste des flags déclenchés
     * @param alert  {@code true} si le score est en dessous du seuil d'alerte
     */
    public record FraudResult(int score, List<String> flags, boolean alert) {}

    /**
     * Signaux d'entrée pour le calcul du score.
     */
    public record FraudSignals(
            /** Pays détecté par IP (ex: "FR"). Toujours disponible. */
            String ipCountry,
            /** Score VPN normalisé 0.0–1.0. */
            double vpnScore,
            /** Timezone du navigateur (ex: "Europe/Paris"). */
            String browserTimezone,
            /** Pays via géolocalisation navigateur (ex: "FR"). Peut être null si refusé. */
            String geoCountry,
            /** Pays de l'adresse de facturation Stripe (ex: "BE"). */
            String billingCountry,
            /** Pays de la carte bancaire (ex: "FR"). Null avant paiement. */
            String cardCountry,
            /** Géolocalisation refusée par l'utilisateur. */
            boolean geoLocationDenied
    ) {}

    /** Mapping pays → timezones attendues (les plus courantes). */
    private static final Map<String, List<String>> COUNTRY_TIMEZONES = Map.ofEntries(
            Map.entry("FR", List.of("Europe/Paris")),
            Map.entry("BE", List.of("Europe/Brussels")),
            Map.entry("CH", List.of("Europe/Zurich")),
            Map.entry("LU", List.of("Europe/Luxembourg")),
            Map.entry("DE", List.of("Europe/Berlin")),
            Map.entry("NL", List.of("Europe/Amsterdam")),
            Map.entry("ES", List.of("Europe/Madrid", "Atlantic/Canary")),
            Map.entry("IT", List.of("Europe/Rome")),
            Map.entry("PT", List.of("Europe/Lisbon", "Atlantic/Azores")),
            Map.entry("GB", List.of("Europe/London")),
            Map.entry("IE", List.of("Europe/Dublin")),
            Map.entry("AT", List.of("Europe/Vienna")),
            Map.entry("PL", List.of("Europe/Warsaw")),
            Map.entry("US", List.of("America/New_York", "America/Chicago", "America/Denver", "America/Los_Angeles",
                    "America/Anchorage", "Pacific/Honolulu")),
            Map.entry("CA", List.of("America/Toronto", "America/Vancouver", "America/Edmonton", "America/Winnipeg",
                    "America/Halifax", "America/St_Johns")),
            Map.entry("SN", List.of("Africa/Dakar")),
            Map.entry("MA", List.of("Africa/Casablanca")),
            Map.entry("NG", List.of("Africa/Lagos")),
            Map.entry("CI", List.of("Africa/Abidjan")),
            Map.entry("CM", List.of("Africa/Douala")),
            Map.entry("JP", List.of("Asia/Tokyo")),
            Map.entry("AU", List.of("Australia/Sydney", "Australia/Melbourne", "Australia/Brisbane",
                    "Australia/Perth", "Australia/Adelaide"))
    );

    /**
     * Calcule le score de fiabilité.
     *
     * @param signals les signaux collectés
     * @return résultat avec score, flags et indicateur d'alerte
     */
    public FraudResult score(FraudSignals signals) {
        logger.debug("[FRAUD-DEBUG] FraudScoringService.score() → signals: ipCountry={}, vpnScore={}, " +
                        "tz={}, geoCountry={}, billingCountry={}, cardCountry={}, geoDenied={}",
                signals.ipCountry(), signals.vpnScore(), signals.browserTimezone(),
                signals.geoCountry(), signals.billingCountry(), signals.cardCountry(),
                signals.geoLocationDenied());

        int score = 100;
        List<String> flags = new ArrayList<>();

        // ── 1. VPN detection ──────────────────────────────────────────────────
        // Seuils ajustés : avec 3 sources (ip-api poids 0.25, getipintel 0.40, iphub 0.35),
        // quand 2/3 confirment le VPN le score consensus est ~0.75 (ip-api dilue).
        // HIGH = 2+ sources unanimes, MEDIUM = consensus clair, SUSPECTED = signal faible.
        if (signals.vpnScore() > 0.85) {
            score -= 30;
            flags.add("VPN_DETECTED_HIGH");
            logger.debug("[FRAUD-DEBUG] Penalty -30: VPN_DETECTED_HIGH (vpnScore={})", signals.vpnScore());
        } else if (signals.vpnScore() > 0.60) {
            score -= 20;
            flags.add("VPN_DETECTED_MEDIUM");
            logger.debug("[FRAUD-DEBUG] Penalty -20: VPN_DETECTED_MEDIUM (vpnScore={})", signals.vpnScore());
        } else if (signals.vpnScore() > 0.40) {
            score -= 10;
            flags.add("VPN_SUSPECTED");
            logger.debug("[FRAUD-DEBUG] Penalty -10: VPN_SUSPECTED (vpnScore={})", signals.vpnScore());
        }

        // ── 2. Timezone mismatch ──────────────────────────────────────────────
        if (signals.browserTimezone() != null && signals.ipCountry() != null) {
            boolean tzMatch = isTimezoneConsistentWithCountry(signals.browserTimezone(), signals.ipCountry());
            if (!tzMatch) {
                score -= 10;
                flags.add("TZ_MISMATCH");
                logger.debug("[FRAUD-DEBUG] Penalty -10: TZ_MISMATCH (tz={}, ipCountry={})",
                        signals.browserTimezone(), signals.ipCountry());
            }
        }

        // ── 3. Billing country vs IP country ─────────────────────────────────
        if (signals.billingCountry() != null && signals.ipCountry() != null
                && !signals.billingCountry().equalsIgnoreCase(signals.ipCountry())) {
            score -= 15;
            flags.add("BILLING_IP_MISMATCH");
            logger.debug("[FRAUD-DEBUG] Penalty -15: BILLING_IP_MISMATCH (billing={}, ip={})",
                    signals.billingCountry(), signals.ipCountry());
        }

        // ── 4. Geolocation vs billing ────────────────────────────────────────
        if (signals.geoCountry() != null && signals.billingCountry() != null
                && !signals.geoCountry().equalsIgnoreCase(signals.billingCountry())) {
            score -= 20;
            flags.add("GEO_BILLING_MISMATCH");
            logger.debug("[FRAUD-DEBUG] Penalty -20: GEO_BILLING_MISMATCH (geo={}, billing={})",
                    signals.geoCountry(), signals.billingCountry());
        }

        // ── 5. Geolocation vs IP ─────────────────────────────────────────────
        if (signals.geoCountry() != null && signals.ipCountry() != null
                && !signals.geoCountry().equalsIgnoreCase(signals.ipCountry())) {
            score -= 10;
            flags.add("GEO_IP_MISMATCH");
            logger.debug("[FRAUD-DEBUG] Penalty -10: GEO_IP_MISMATCH (geo={}, ip={})",
                    signals.geoCountry(), signals.ipCountry());
        }

        // ── 6. Geolocation denied ────────────────────────────────────────────
        if (signals.geoLocationDenied()) {
            if (signals.vpnScore() > 0.40) {
                // Refus de géoloc + VPN suspecté/confirmé → très suspect
                score -= 15;
                flags.add("GEO_DENIED_WITH_VPN");
                logger.debug("[FRAUD-DEBUG] Penalty -15: GEO_DENIED_WITH_VPN");
            } else if (signals.ipCountry() != null && signals.billingCountry() != null
                    && !signals.ipCountry().equalsIgnoreCase(signals.billingCountry())) {
                // Refus de géoloc + IP et billing dans des pays différents → suspect
                score -= 10;
                flags.add("GEO_DENIED_WITH_MISMATCH");
                logger.debug("[FRAUD-DEBUG] Penalty -10: GEO_DENIED_WITH_MISMATCH");
            }
        }

        // ── 7. Card country vs billing country (post-payment) ───────────────
        if (signals.cardCountry() != null && signals.billingCountry() != null
                && !signals.cardCountry().equalsIgnoreCase(signals.billingCountry())) {
            score -= 15;
            flags.add("CARD_BILLING_MISMATCH");
            logger.debug("[FRAUD-DEBUG] Penalty -15: CARD_BILLING_MISMATCH (card={}, billing={})",
                    signals.cardCountry(), signals.billingCountry());
        }

        // ── Bonus ────────────────────────────────────────────────────────────
        boolean allCountriesMatch = signals.ipCountry() != null
                && signals.billingCountry() != null
                && signals.ipCountry().equalsIgnoreCase(signals.billingCountry())
                && (signals.geoCountry() == null || signals.geoCountry().equalsIgnoreCase(signals.ipCountry()));

        if (allCountriesMatch && signals.vpnScore() < 0.3) {
            score = Math.min(100, score + 5);
            logger.debug("[FRAUD-DEBUG] Bonus +5: ALL_COUNTRIES_MATCH + low VPN score");
        }

        if (signals.vpnScore() < 0.1 && signals.browserTimezone() != null
                && isTimezoneConsistentWithCountry(signals.browserTimezone(), signals.ipCountry())) {
            score = Math.min(100, score + 5);
            logger.debug("[FRAUD-DEBUG] Bonus +5: NO_VPN + TZ_MATCH");
        }

        // ── Clamp ────────────────────────────────────────────────────────────
        score = Math.max(0, Math.min(100, score));
        boolean alert = score < ALERT_THRESHOLD;

        logger.info("[FRAUD-SCORE] Final score={} alert={} flags={}", score, alert, flags);

        return new FraudResult(score, flags, alert);
    }

    /**
     * Recalcule le score avec un nouveau signal card country (post-paiement).
     */
    public FraudResult recalculateWithCardCountry(FraudSignals originalSignals, String cardCountry) {
        logger.debug("[FRAUD-DEBUG] Recalculating score with cardCountry={}", cardCountry);
        return score(new FraudSignals(
                originalSignals.ipCountry(),
                originalSignals.vpnScore(),
                originalSignals.browserTimezone(),
                originalSignals.geoCountry(),
                originalSignals.billingCountry(),
                cardCountry,
                originalSignals.geoLocationDenied()
        ));
    }

    /**
     * Vérifie si un timezone est cohérent avec un pays.
     */
    private boolean isTimezoneConsistentWithCountry(String timezone, String countryCode) {
        if (timezone == null || countryCode == null) return true;

        List<String> expectedTzs = COUNTRY_TIMEZONES.get(countryCode.toUpperCase());
        if (expectedTzs == null) {
            // Pays non mappé : utiliser le fallback Java TimeZone
            String[] available = TimeZone.getAvailableIDs();
            for (String tz : available) {
                if (tz.equalsIgnoreCase(timezone)) {
                    // On ne peut pas vérifier sans mapping → pas de pénalité
                    return true;
                }
            }
            return true;
        }

        return expectedTzs.stream().anyMatch(tz -> tz.equalsIgnoreCase(timezone));
    }
}
