package com.lmp.integration.sync.mapper;

import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderItem;
import com.lmp.integration.sync.SyncProperties;
import org.springframework.stereotype.Component;

import com.lmp.shared.pricing.MoneyUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Construit les payloads Sales Order et Sales Invoice à envoyer au système externe.
 * <p>
 * Les clés du payload correspondent aux champs attendus par le système externe.
 * La devise et la société sont centralisées via {@link SyncProperties}.
 * <p>
 * Stratégie TVA : les prix envoyés sont TTC (tax-inclusive). Le champ
 * {@code included_in_print_rate=1} indique au système externe que la taxe est
 * déjà incluse dans le prix, préservant ainsi le total identique entre LMP et l'ERP.
 */
@Component
public class OrderSyncMapper {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SyncProperties syncProperties;

    public OrderSyncMapper(SyncProperties syncProperties) {
        this.syncProperties = syncProperties;
    }

    /**
     * Construit le payload Sales Order à partir d'une commande LMP confirmée.
     */
    public Map<String, Object> toSalesOrderPayload(Order order) {
        Map<String, Object> payload = new LinkedHashMap<>();

        // Identification
        payload.put("naming_series", "SO-.YYYY.-");
        payload.put("company", syncProperties.getExternal().getCompany());
        payload.put("currency", syncProperties.getExternal().getCurrency());
        payload.put("transaction_date", formatDate(order.getCreatedAt()));
        payload.put("delivery_date", formatDate(order.getCreatedAt().plusDays(30)));

        // Client
        String customerName = resolveCustomerName(order);
        payload.put("customer", customerName);

        // Metadata
        payload.put("po_no", order.getId().toString());
        payload.put("order_type", "Shopping Cart");

        // Traçabilité bidirectionnelle — UUID LMP stocké côté système externe
        payload.put("lmp_order_id", order.getId().toString());

        // Services — pas de livraison physique requise (champ au niveau SO parent dans external ERP v17)
        payload.put("skip_delivery_note", 1);

        // Lignes d'articles
        List<Map<String, Object>> items = buildOrderItems(order, "Sales Order Item");
        payload.put("items", items);

        // Taxes (TVA incluse dans le prix)
        List<Map<String, Object>> taxes = buildTaxes(order);
        if (!taxes.isEmpty()) {
            payload.put("taxes", taxes);
        }

        return payload;
    }

    /**
     * Construit le payload Sales Invoice à partir d'une commande LMP confirmée.
     */
    public Map<String, Object> toSalesInvoicePayload(Order order) {
        Map<String, Object> payload = new LinkedHashMap<>();

        // Identification
        payload.put("naming_series", "SINV-.YYYY.-");
        payload.put("company", syncProperties.getExternal().getCompany());
        payload.put("currency", syncProperties.getExternal().getCurrency());
        payload.put("posting_date", formatDate(order.getPaidAt() != null ? order.getPaidAt() : order.getCreatedAt()));
        payload.put("due_date", formatDate(
                (order.getPaidAt() != null ? order.getPaidAt() : order.getCreatedAt()).plusDays(30)));

        // Client
        String customerName = resolveCustomerName(order);
        payload.put("customer", customerName);

        // TVA / autoliquidation
        if (Boolean.TRUE.equals(order.getVatReverseCharge())) {
            payload.put("tax_category", "Autoliquidation UE");
        }

        // Marquée comme payée
        payload.put("is_pos", 0);
        payload.put("update_stock", 0);

        // Lignes d'articles — avec lien vers le Sales Order pour que per_billed se mette à jour
        List<Map<String, Object>> items = buildOrderItems(order, "Sales Invoice Item");
        if (order.getExternalOrderId() != null) {
            for (Map<String, Object> item : items) {
                item.put("sales_order", order.getExternalOrderId());
            }
        }
        payload.put("items", items);

        // Taxes (TVA incluse dans le prix)
        List<Map<String, Object>> taxes = buildTaxes(order);
        if (!taxes.isEmpty()) {
            payload.put("taxes", taxes);
        }

        // Référence de commande externe (si disponible)
        if (order.getExternalOrderId() != null) {
            payload.put("po_no", order.getExternalOrderId());
        }

        // Payment Terms Template — paiement en plusieurs fois
        if (order.isInstallmentOrder() && order.getPaymentTermsTemplate() != null) {
            payload.put("payment_terms_template", order.getPaymentTermsTemplate());
        }

        // Traçabilité bidirectionnelle — UUID LMP stocké côté système externe
        payload.put("lmp_order_id", order.getId().toString());

        return payload;
    }

    /**
     * Construit le payload de mise à jour d'un Sales Order (statut, notes).
     */
    public Map<String, Object> toSalesOrderUpdatePayload(Order order) {
        Map<String, Object> payload = new LinkedHashMap<>();

        if (order.getNotes() != null) {
            payload.put("notes", order.getNotes());
        }
        if (order.getAdminNotes() != null) {
            payload.put("remarks", order.getAdminNotes());
        }

        return payload;
    }

    // ==================== Helpers ====================

    private List<Map<String, Object>> buildOrderItems(Order order, String childDoctype) {
        List<Map<String, Object>> items = new ArrayList<>();

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (OrderItem item : order.getItems()) {
                Map<String, Object> line = new LinkedHashMap<>();

                // Item code — use external item code if linked, otherwise service name
                String itemCode = (item.getService() != null && item.getService().getExternalItemCode() != null)
                        ? item.getService().getExternalItemCode()
                        : (item.getService() != null ? item.getService().getTitle() : order.getServiceName());
                line.put("doctype", childDoctype);
                line.put("item_code", itemCode);
                line.put("item_name", order.getServiceName());
                line.put("qty", item.getQuantity());
                // Rate TTC — les taxes sont marquées included_in_print_rate=1, donc
                // l'ERP attend le prix TTC pour en extraire la part de taxe.
                // item.getPrice() est HT → on ajoute la TVA appliquée à la commande.
                BigDecimal unitHT = item.getPrice();
                BigDecimal vatRate = order.getAppliedVatRate();
                if (unitHT != null && vatRate != null && vatRate.compareTo(BigDecimal.ZERO) > 0
                        && !Boolean.TRUE.equals(order.getVatReverseCharge())) {
                    line.put("rate", MoneyUtils.multiply(unitHT, BigDecimal.ONE.add(vatRate)));
                } else {
                    line.put("rate", unitHT);
                }

                items.add(line);
            }
        } else {
            // Fallback : single-line order from serviceName + totalAmount (TTC)
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("doctype", childDoctype);
            line.put("item_code", order.getServiceName());
            line.put("item_name", order.getServiceName());
            line.put("qty", 1);
            // totalAmount est TTC — c'est le bon montant quand included_in_print_rate=1
            line.put("rate", order.getTotalAmount());
            items.add(line);
        }

        return items;
    }

    /**
     * Construit la table "taxes" pour Sales Order / Sales Invoice.
     * <p>
     * Utilise {@code included_in_print_rate = 1} pour que la taxe soit considérée
     * comme déjà incluse dans le prix des articles. Cela garantit que le
     * grand_total reste identique au totalAmount de LMP — aucun recalcul
     * ne modifie le prix final.
     * <p>
     * Si aucun taux de TVA n'est appliqué (reverse charge, taux = 0, ou absent),
     * aucune ligne de taxe n'est ajoutée.
     */
    private List<Map<String, Object>> buildTaxes(Order order) {
        // Pas de taxe si reverse charge
        if (Boolean.TRUE.equals(order.getVatReverseCharge())) {
            return List.of();
        }

        BigDecimal vatRate = order.getAppliedVatRate();
        if (vatRate == null || vatRate.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        // Convertir le taux décimal (0.2100) en pourcentage (21.00)
        BigDecimal vatPercent = vatRate.multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        // Calculer le montant de la taxe pour référence
        // totalAmount = TTC, taxAmount = TTC - (TTC / (1 + rate))
        BigDecimal totalTTC = order.getTotalAmount();
        BigDecimal netAmount = MoneyUtils.extractHt(totalTTC, vatRate);
        BigDecimal taxAmount = totalTTC.subtract(netAmount);

        Map<String, Object> taxRow = new LinkedHashMap<>();
        taxRow.put("charge_type", "On Net Total");
        taxRow.put("description", "TVA " + vatPercent.stripTrailingZeros().toPlainString() + "%");
        taxRow.put("rate", vatPercent);
        // La taxe est déjà incluse dans le prix des articles → le total ne change pas
        taxRow.put("included_in_print_rate", 1);
        taxRow.put("tax_amount", taxAmount);

        // Compte comptable de taxe — résolu dynamiquement selon le taux
        String taxAccount = syncProperties.getExternal().resolveTaxAccount(vatPercent);
        if (taxAccount != null && !taxAccount.isBlank()) {
            taxRow.put("account_head", taxAccount);
        }

        return List.of(taxRow);
    }

    /**
     * Résout le nom du Customer externe à partir de la commande.
     * Utilise l'external_customer_id du User si disponible.
     */
    private String resolveCustomerName(Order order) {
        if (order.getUser() != null && order.getUser().getExternalCustomerId() != null) {
            return order.getUser().getExternalCustomerId();
        }
        // Fallback sur le billing name ou le nom du service
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
