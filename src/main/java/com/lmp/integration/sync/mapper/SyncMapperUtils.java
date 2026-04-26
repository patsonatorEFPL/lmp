package com.lmp.integration.sync.mapper;

import com.lmp.auth.domain.User;
import com.lmp.shared.pricing.MoneyUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Helpers partagés par tous les SyncMappers (Order, Quotation, Invoice…).
 * <p>
 * Centralise la logique TVA, formatage dates, résolution customer, et champs company.
 * Évite la duplication entre OrderSyncMapper et QuotationSyncMapper.
 */
public final class SyncMapperUtils {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private SyncMapperUtils() {}

    // ==================== Company ====================

    /**
     * Injecte les champs company et currency dans un payload.
     * Paramétré pour supporter le multi-company futur.
     */
    public static void setCompanyFields(Map<String, Object> payload, String company, String currency) {
        payload.put("company", company);
        payload.put("currency", currency);
    }

    // ==================== Customer ====================

    /**
     * Résout le nom du Customer externe. Priorité :
     * 1. externalCustomerId du User
     * 2. billingName
     * 3. firstName + lastName
     * 4. email
     * 5. "Guest Customer" (fallback)
     */
    public static String resolveCustomerName(User user, String billingName) {
        if (user != null && user.getExternalCustomerId() != null) {
            return user.getExternalCustomerId();
        }
        if (billingName != null && !billingName.isBlank()) {
            return billingName;
        }
        if (user != null) {
            String name = "";
            if (user.getFirstName() != null) name += user.getFirstName();
            if (user.getLastName() != null) name += " " + user.getLastName();
            return name.trim().isEmpty() ? user.getEmail() : name.trim();
        }
        return "Guest Customer";
    }

    // ==================== Dates ====================

    public static String formatDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalDate().format(DATE_FMT) : LocalDate.now().format(DATE_FMT);
    }

    // ==================== TVA ====================

    /**
     * Calcule le prix TTC à partir du prix HT et du taux de TVA.
     * Retourne le prix HT si reverse charge ou pas de TVA.
     */
    public static BigDecimal computeTtcRate(BigDecimal unitHt, BigDecimal vatRate, boolean reverseCharge) {
        if (unitHt == null) return BigDecimal.ZERO;
        if (reverseCharge || vatRate == null || vatRate.compareTo(BigDecimal.ZERO) <= 0) {
            return unitHt;
        }
        return MoneyUtils.multiply(unitHt, BigDecimal.ONE.add(vatRate));
    }

    /**
     * Construit la table "taxes" avec included_in_print_rate=1.
     * Retourne une liste vide si pas de TVA applicable.
     *
     * @param totalTtc montant total TTC
     * @param vatRate  taux décimal (ex: 0.2100)
     * @param reverseCharge autoliquidation UE
     * @param taxAccountResolver résolveur de compte comptable (taux% → compte)
     */
    public static List<Map<String, Object>> buildTaxRows(BigDecimal totalTtc, BigDecimal vatRate,
                                                          boolean reverseCharge,
                                                          java.util.function.Function<BigDecimal, String> taxAccountResolver) {
        if (reverseCharge) return List.of();
        if (vatRate == null || vatRate.compareTo(BigDecimal.ZERO) <= 0) return List.of();

        BigDecimal vatPercent = vatRate.multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal netAmount = MoneyUtils.extractHt(totalTtc, vatRate);
        BigDecimal taxAmount = totalTtc.subtract(netAmount);

        Map<String, Object> taxRow = new LinkedHashMap<>();
        taxRow.put("doctype", "Sales Taxes and Charges");
        taxRow.put("charge_type", "On Net Total");
        taxRow.put("description", "TVA " + vatPercent.stripTrailingZeros().toPlainString() + "%");
        taxRow.put("rate", vatPercent);
        taxRow.put("included_in_print_rate", 1);
        taxRow.put("tax_amount", taxAmount);

        String taxAccount = taxAccountResolver.apply(vatPercent);
        if (taxAccount != null && !taxAccount.isBlank()) {
            taxRow.put("account_head", taxAccount);
        }

        return List.of(taxRow);
    }
}
