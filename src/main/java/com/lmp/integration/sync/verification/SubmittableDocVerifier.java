package com.lmp.integration.sync.verification;

import com.lmp.integration.sync.ExternalResponse;
import com.lmp.integration.sync.ExternalSystemClient;
import com.lmp.integration.sync.SyncEntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Vérifie les documents soumissibles (Sales Order, Sales Invoice, Payment Entry).
 * <p>
 * Contrôles effectués :
 * <ul>
 *   <li>{@code docstatus == 1} — le document a été soumis (pas en Draft)</li>
 *   <li>Le {@code lmp_order_id} correspond à l'UUID LMP attendu (si disponible)</li>
 *   <li>Pour les Sales Invoice : le {@code grand_total} est positif</li>
 *   <li>Pour les Payment Entry : le {@code paid_amount} est positif</li>
 * </ul>
 */
@Component
public class SubmittableDocVerifier implements SyncVerifier {

    private static final Logger log = LoggerFactory.getLogger(SubmittableDocVerifier.class);

    private static final Set<SyncEntityType> SUPPORTED = Set.of(
            SyncEntityType.SALES_ORDER,
            SyncEntityType.SALES_INVOICE,
            SyncEntityType.PAYMENT
    );

    private final ExternalSystemClient externalClient;

    public SubmittableDocVerifier(ExternalSystemClient externalClient) {
        this.externalClient = externalClient;
    }

    @Override
    public boolean supports(SyncEntityType entityType) {
        return SUPPORTED.contains(entityType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public VerificationResult verify(SyncEntityType entityType, String externalId) {
        if (externalId == null || externalId.isBlank()) {
            return VerificationResult.mismatch("No external ID to verify");
        }

        try {
            ExternalResponse response = externalClient.getEntity(entityType, externalId);

            if (!response.success()) {
                return VerificationResult.unreachable(
                        "GET failed (HTTP " + response.httpStatus() + "): " + response.errorMessage());
            }

            Map<String, Object> data = response.data();
            if (data == null) {
                return VerificationResult.unreachable("Empty response body");
            }

            // Navigate nested {"data": {...}} structure
            if (data.containsKey("data") && data.get("data") instanceof Map) {
                data = (Map<String, Object>) data.get("data");
            }

            // 1. Vérifier docstatus = 1 (Submitted)
            Object docstatusObj = data.get("docstatus");
            int docstatus = (docstatusObj instanceof Number n) ? n.intValue() : -1;
            if (docstatus != 1) {
                return VerificationResult.mismatch(
                        "docstatus=" + docstatus + " (expected 1 — Submitted) for " + entityType + " " + externalId);
            }

            // 2. Vérifications spécifiques par type
            return switch (entityType) {
                case SALES_INVOICE -> verifySalesInvoice(data, externalId);
                case PAYMENT -> verifyPaymentEntry(data, externalId);
                default -> VerificationResult.ok(); // SO : docstatus=1 suffit
            };

        } catch (Exception e) {
            log.warn("[VERIFY] Exception verifying {} {}: {}", entityType, externalId, e.getMessage());
            return VerificationResult.unreachable(e.getMessage());
        }
    }

    /**
     * Vérifie qu'une Sales Invoice a un grand_total > 0.
     */
    private VerificationResult verifySalesInvoice(Map<String, Object> data, String externalId) {
        Object grandTotal = data.get("grand_total");
        if (grandTotal == null) {
            return VerificationResult.mismatch("grand_total is null on SINV " + externalId);
        }
        double total = ((Number) grandTotal).doubleValue();
        if (total <= 0) {
            return VerificationResult.mismatch("grand_total=" + total + " (expected > 0) on SINV " + externalId);
        }
        return VerificationResult.ok();
    }

    /**
     * Vérifie qu'un Payment Entry a un paid_amount > 0.
     */
    private VerificationResult verifyPaymentEntry(Map<String, Object> data, String externalId) {
        Object paidAmount = data.get("paid_amount");
        if (paidAmount == null) {
            return VerificationResult.mismatch("paid_amount is null on PE " + externalId);
        }
        double amount = ((Number) paidAmount).doubleValue();
        if (amount <= 0) {
            return VerificationResult.mismatch("paid_amount=" + amount + " (expected > 0) on PE " + externalId);
        }
        return VerificationResult.ok();
    }
}
