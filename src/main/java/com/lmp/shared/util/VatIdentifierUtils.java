package com.lmp.shared.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalisation et validation basique du format d'un identifiant TVA de type intracommunautaire
 * (préfixe pays ISO 3166-1 alpha-2 + partie nationale alphanumérique). Ne remplace pas VIES.
 */
public final class VatIdentifierUtils {

    private static final Pattern EU_STYLE_VAT = Pattern.compile("^[A-Z]{2}[0-9A-Z]{2,28}$");

    private VatIdentifierUtils() {}

    /**
     * Supprime espaces, points et tirets puis met en majuscules (usage courant d'affichage FR/BE).
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.replaceAll("[\\s.\\-]", "").toUpperCase(Locale.ROOT);
    }

    /**
     * Vérifie un format plausible (2 lettres pays + au moins 2 caractères alphanumériques), sans appel VIES.
     */
    public static boolean isPlausibleEuVatFormat(String normalized) {
        if (normalized == null || normalized.length() < 4) {
            return false;
        }
        return EU_STYLE_VAT.matcher(normalized).matches();
    }
}
