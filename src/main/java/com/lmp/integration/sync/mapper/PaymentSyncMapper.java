package com.lmp.integration.sync.mapper;

import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderInstallment;
import com.lmp.integration.sync.SyncProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Construit les payloads Payment Entry à envoyer au système externe.
 * <p>
 * Un Payment Entry de type "Receive" est créé pour chaque commande confirmée
 * (payée via Stripe). Il est lié à la Sales Invoice pour que l'ERP marque
 * la facture comme "Payée" au lieu de "Impayée".
 * <p>
 * Champs clés :
 * - payment_type = "Receive" (réception d'argent du client)
 * - party_type = "Customer"
 * - paid_from = compte Receivable (Débiteurs)
 * - paid_to = compte Bank (configurable)
 * - references = [{reference_doctype: "Sales Invoice", reference_name: SINV-...}]
 */
@Component
public class PaymentSyncMapper {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SyncProperties syncProperties;

    public PaymentSyncMapper(SyncProperties syncProperties) {
        this.syncProperties = syncProperties;
    }

    /**
     * Construit le payload Payment Entry pour une commande LMP confirmée et payée.
     *
     * @param order          la commande LMP (doit avoir paidAt et externalInvoiceId)
     * @param salesInvoiceId le nom de la Sales Invoice dans le système externe (ex: SINV-2026-00005)
     * @param erpInvoiceTotal montant grand_total réel calculé par l'ERP (peut différer de LMP
     *                        à cause des arrondis TVA avec included_in_print_rate). Null = fallback
     *                        sur order.getTotalAmount().
     * @return le payload prêt à être envoyé au système externe
     */
    public Map<String, Object> toPaymentEntryPayload(Order order, String salesInvoiceId,
                                                      BigDecimal erpInvoiceTotal) {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("naming_series", "ACC-PAY-.YYYY.-");
        payload.put("payment_type", "Receive");
        payload.put("company", syncProperties.getExternal().getCompany());

        // Client
        String customerName = resolveCustomerName(order);
        payload.put("party_type", "Customer");
        payload.put("party", customerName);

        // Comptes comptables
        payload.put("paid_from", syncProperties.getExternal().getReceivableAccount());
        payload.put("paid_to", syncProperties.getExternal().getPaymentAccount());

        // Montant = utiliser le grand_total ERP si disponible (évite les écarts d'arrondi TVA)
        BigDecimal amount = erpInvoiceTotal != null ? erpInvoiceTotal : order.getTotalAmount();
        payload.put("paid_amount", amount);
        payload.put("received_amount", amount);

        // Devise
        payload.put("source_exchange_rate", 1);
        payload.put("target_exchange_rate", 1);

        // Mode de paiement
        String modeOfPayment = resolveModeOfPayment(order);
        if (modeOfPayment != null) {
            payload.put("mode_of_payment", modeOfPayment);
        }

        // Date de paiement
        LocalDateTime paymentDate = order.getPaidAt() != null ? order.getPaidAt() : order.getCreatedAt();
        payload.put("posting_date", formatDate(paymentDate));

        // Référence de transaction (Stripe Payment Intent ID)
        if (order.getStripePaymentIntentId() != null) {
            payload.put("reference_no", order.getStripePaymentIntentId());
            payload.put("reference_date", formatDate(paymentDate));
        } else {
            // Fallback : utiliser l'ID de commande LMP
            payload.put("reference_no", order.getId().toString());
            payload.put("reference_date", formatDate(paymentDate));
        }

        // Lien vers la Sales Invoice — permet à l'ERP de réconcilier et marquer "Payé"
        if (salesInvoiceId != null && !salesInvoiceId.isBlank()) {
            Map<String, Object> ref = new LinkedHashMap<>();
            ref.put("reference_doctype", "Sales Invoice");
            ref.put("reference_name", salesInvoiceId);
            ref.put("allocated_amount", amount);
            payload.put("references", List.of(ref));
        }

        // Remarque
        payload.put("remarks", String.format("Paiement Stripe pour commande LMP %s", order.getId()));

        return payload;
    }

    /**
     * Construit le payload Payment Entry pour une échéance spécifique d'un paiement en plusieurs fois.
     * <p>
     * Le champ {@code payment_term} sur la référence SINV permet à ERPNext de mettre à jour
     * le {@code paid_amount} de la bonne ligne du {@code payment_schedule} sur la facture.
     *
     * @param order          la commande LMP
     * @param installment    l'échéance payée
     * @param salesInvoiceId le nom de la Sales Invoice dans l'ERP
     * @param erpInstallmentAmount montant réel de l'échéance côté ERP (null = utiliser installment.getAmount())
     */
    public Map<String, Object> toInstallmentPaymentEntryPayload(Order order,
                                                                 OrderInstallment installment,
                                                                 String salesInvoiceId,
                                                                 BigDecimal erpInstallmentAmount) {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("naming_series", "ACC-PAY-.YYYY.-");
        payload.put("payment_type", "Receive");
        payload.put("company", syncProperties.getExternal().getCompany());

        // Client
        String customerName = resolveCustomerName(order);
        payload.put("party_type", "Customer");
        payload.put("party", customerName);

        // Comptes comptables
        payload.put("paid_from", syncProperties.getExternal().getReceivableAccount());
        payload.put("paid_to", syncProperties.getExternal().getPaymentAccount());

        // Montant de l'échéance
        BigDecimal amount = erpInstallmentAmount != null ? erpInstallmentAmount : installment.getAmount();
        payload.put("paid_amount", amount);
        payload.put("received_amount", amount);

        // Devise
        payload.put("source_exchange_rate", 1);
        payload.put("target_exchange_rate", 1);

        // Mode de paiement
        String modeOfPayment = resolveModeOfPayment(order);
        if (modeOfPayment != null) {
            payload.put("mode_of_payment", modeOfPayment);
        }

        // Date de paiement
        LocalDateTime paymentDate = installment.getPaidAt() != null ? installment.getPaidAt() : LocalDateTime.now();
        payload.put("posting_date", formatDate(paymentDate));

        // Référence de transaction (Stripe PaymentIntent de l'échéance)
        String refNo = installment.getStripePaymentIntentId() != null
                ? installment.getStripePaymentIntentId()
                : order.getId() + "-" + installment.getInstallmentNumber();
        payload.put("reference_no", refNo);
        payload.put("reference_date", formatDate(paymentDate));

        // Lien vers la Sales Invoice avec le payment_term
        if (salesInvoiceId != null && !salesInvoiceId.isBlank()) {
            Map<String, Object> ref = new LinkedHashMap<>();
            ref.put("reference_doctype", "Sales Invoice");
            ref.put("reference_name", salesInvoiceId);
            ref.put("allocated_amount", amount);
            // payment_term permet à ERPNext d'identifier quelle échéance du payment_schedule
            // est concernée et de mettre à jour son paid_amount / outstanding
            ref.put("payment_term", installment.getPaymentTerm());
            payload.put("references", List.of(ref));
        }

        // Remarque
        payload.put("remarks", String.format("Échéance %d/%d — Commande LMP %s",
                installment.getInstallmentNumber(),
                order.getInstallmentCount(),
                order.getId()));

        return payload;
    }

    /**
     * Résout le mode de paiement ERP en fonction de la méthode utilisée sur LMP.
     */
    private String resolveModeOfPayment(Order order) {
        if (order.getPaymentMethod() == null) return "Credit Card";
        return switch (order.getPaymentMethod().toLowerCase()) {
            case "card", "stripe", "credit_card" -> "Credit Card";
            case "bank_transfer", "wire", "sepa" -> "Wire Transfer";
            case "cash" -> "Cash";
            default -> "Credit Card";
        };
    }

    private String resolveCustomerName(Order order) {
        if (order.getUser() != null && order.getUser().getExternalCustomerId() != null) {
            return order.getUser().getExternalCustomerId();
        }
        if (order.getBillingName() != null && !order.getBillingName().isBlank()) {
            return order.getBillingName();
        }
        if (order.getUser() != null) {
            String name = "";
            if (order.getUser().getFirstName() != null) name += order.getUser().getFirstName();
            if (order.getUser().getLastName() != null) name += " " + order.getUser().getLastName();
            return name.trim().isEmpty() ? order.getUser().getEmail() : name.trim();
        }
        return "Guest Customer";
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalDate().format(DATE_FMT) : LocalDate.now().format(DATE_FMT);
    }
}
