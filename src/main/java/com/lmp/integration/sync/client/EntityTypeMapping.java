package com.lmp.integration.sync.client;

import com.lmp.integration.sync.SyncEntityType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Mapping entre les types d'entités LMP et les noms de documents du système externe.
 * <p>
 * Ce mapping est le SEUL endroit où les noms du système externe apparaissent.
 * Les valeurs correspondent aux DocTypes du système cible.
 */
@Component
public class EntityTypeMapping {

    private static final Map<SyncEntityType, String> DEFAULT_MAPPING = Map.ofEntries(
            Map.entry(SyncEntityType.CUSTOMER, "Customer"),
            Map.entry(SyncEntityType.CONTACT, "Contact"),
            Map.entry(SyncEntityType.SALES_ORDER, "Sales Order"),
            Map.entry(SyncEntityType.SALES_INVOICE, "Sales Invoice"),
            Map.entry(SyncEntityType.PAYMENT, "Payment Entry"),
            Map.entry(SyncEntityType.ITEM, "Item"),
            Map.entry(SyncEntityType.ITEM_GROUP, "Item Group"),
            Map.entry(SyncEntityType.ITEM_PRICE, "Item Price"),
            Map.entry(SyncEntityType.PROJECT, "Project"),
            Map.entry(SyncEntityType.TASK, "Task"),
            Map.entry(SyncEntityType.ISSUE, "Issue"),
            Map.entry(SyncEntityType.COMMUNICATION, "Communication"),
            Map.entry(SyncEntityType.NOTIFICATION, "Notification Log"),
            Map.entry(SyncEntityType.QUOTATION, "Quotation")
    );

    /**
     * Convertit un type d'entité LMP en nom de document externe.
     */
    public String toExternalDocType(SyncEntityType type) {
        return DEFAULT_MAPPING.getOrDefault(type, type.name());
    }

    /**
     * Convertit un nom de document externe en type d'entité LMP.
     *
     * @return le type LMP correspondant, ou {@code null} si inconnu
     */
    public SyncEntityType fromExternalDocType(String docType) {
        return DEFAULT_MAPPING.entrySet().stream()
                .filter(e -> e.getValue().equalsIgnoreCase(docType))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
