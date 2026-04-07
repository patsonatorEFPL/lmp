package com.lmp.shared.geo;

/**
 * Résultat d'une résolution géographique IP.
 *
 * @param countryCode  Code pays ISO 3166-1 alpha-2 (ex. «&nbsp;SN&nbsp;»). Jamais null.
 * @param currencyCode Code devise ISO 4217 tel que renvoyé par l'API géo (ex. «&nbsp;XOF&nbsp;»).
 *                     Peut être {@code null} si la source utilisée ne fournit pas la devise
 *                     (CF-IPCountry, GeoLite2) — la résolution FX prendra le relais.
 */
public record GeoResolution(String countryCode, String currencyCode) {

    /** Construction sans devise (sources ne fournissant que le pays). */
    public static GeoResolution countryOnly(String countryCode) {
        return new GeoResolution(countryCode, null);
    }
}
