package com.lmp.integration.sync.mapper;

import com.lmp.billing.domain.Quotation;
import com.lmp.billing.domain.QuotationItem;
import com.lmp.integration.sync.SyncProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * Construit les payloads Quotation à envoyer au système externe.
 * <p>
 * Réutilise {@link SyncMapperUtils} pour la logique TVA, dates et customer
 * (même stratégie que {@link OrderSyncMapper}).
 * <p>
 * Mapping external ERP :
 * <ul>
 *   <li>LMP DRAFT → external ERP Draft (docstatus=0)</li>
 *   <li>LMP SENT → external ERP Open (docstatus=1, submitted)</li>
 *   <li>LMP ACCEPTED → external ERP Ordered (via make_sales_order API)</li>
 *   <li>LMP REJECTED → external ERP Lost</li>
 *   <li>LMP EXPIRED → external ERP Expired</li>
 * </ul>
 */
@Component
public class QuotationSyncMapper {

    private final SyncProperties syncProperties;

    public QuotationSyncMapper(SyncProperties syncProperties) {
        this.syncProperties = syncProperties;
    }

    /**
     * Construit le payload Quotation pour création côté système externe.
     */
    public Map<String, Object> toQuotationPayload(Quotation quotation) {
        Map<String, Object> payload = new LinkedHashMap<>();

        // Identification
        payload.put("naming_series", "QTN-.YYYY.-");
        SyncMapperUtils.setCompanyFields(payload,
                syncProperties.getExternal().getCompany(),
                syncProperties.getExternal().getCurrency());
        payload.put("transaction_date", SyncMapperUtils.formatDate(quotation.getCreatedAt()));
        payload.put("quotation_to", "Customer");

        // Validité
        if (quotation.getValidUntil() != null) {
            payload.put("valid_till", SyncMapperUtils.formatDate(quotation.getValidUntil()));
        }

        // Client
        String customerName = SyncMapperUtils.resolveCustomerName(
                quotation.getUser(), quotation.getBillingName());
        payload.put("party_name", customerName);

        // Traçabilité bidirectionnelle
        payload.put("lmp_quotation_id", quotation.getId().toString());

        // Notes
        if (quotation.getNotes() != null) {
            payload.put("terms", quotation.getNotes());
        }

        // Lignes d'articles
        List<Map<String, Object>> items = buildItems(quotation);
        payload.put("items", items);

        // Taxes (TVA incluse dans le prix — même logique que OrderSyncMapper)
        boolean reverseCharge = Boolean.TRUE.equals(quotation.getVatReverseCharge());
        List<Map<String, Object>> taxes = SyncMapperUtils.buildTaxRows(
                quotation.getTotalAmount(),
                quotation.getAppliedVatRate(),
                reverseCharge,
                syncProperties.getExternal()::resolveTaxAccount
        );
        if (!taxes.isEmpty()) {
            payload.put("taxes", taxes);
        }

        // Reverse charge
        if (reverseCharge) {
            payload.put("tax_category", "Autoliquidation UE");
        }

        return payload;
    }

    /**
     * Construit le payload de mise à jour (notes, validité).
     */
    public Map<String, Object> toQuotationUpdatePayload(Quotation quotation) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (quotation.getNotes() != null) {
            payload.put("terms", quotation.getNotes());
        }
        if (quotation.getValidUntil() != null) {
            payload.put("valid_till", SyncMapperUtils.formatDate(quotation.getValidUntil()));
        }
        return payload;
    }

    // ==================== Helpers ====================

    private List<Map<String, Object>> buildItems(Quotation quotation) {
        List<Map<String, Object>> items = new ArrayList<>();
        boolean reverseCharge = Boolean.TRUE.equals(quotation.getVatReverseCharge());

        if (quotation.getItems() != null && !quotation.getItems().isEmpty()) {
            for (QuotationItem item : quotation.getItems()) {
                Map<String, Object> line = new LinkedHashMap<>();

                String itemCode = (item.getService() != null && item.getService().getExternalItemCode() != null)
                        ? item.getService().getExternalItemCode()
                        : (item.getService() != null ? item.getService().getTitle() : quotation.getTitle());
                line.put("doctype", "Quotation Item");
                line.put("item_code", itemCode);
                line.put("item_name", item.getDescription() != null ? item.getDescription() : quotation.getTitle());
                line.put("qty", item.getQuantity());
                line.put("rate", SyncMapperUtils.computeTtcRate(
                        item.getPrice(), quotation.getAppliedVatRate(), reverseCharge));

                items.add(line);
            }
        } else {
            // Fallback single-line
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("doctype", "Quotation Item");
            line.put("item_code", quotation.getTitle());
            line.put("item_name", quotation.getTitle());
            line.put("qty", 1);
            line.put("rate", quotation.getTotalAmount());
            items.add(line);
        }

        return items;
    }
}
