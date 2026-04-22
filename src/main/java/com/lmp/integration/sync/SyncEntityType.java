package com.lmp.integration.sync;

/**
 * Types d'entités synchronisables avec un système externe.
 * Noms volontairement génériques — aucune référence à un ERP spécifique.
 */
public enum SyncEntityType {
    CUSTOMER,
    CONTACT,
    SALES_ORDER,
    SALES_INVOICE,
    PAYMENT,
    ITEM,
    ITEM_GROUP,
    ITEM_PRICE,
    PROJECT,
    TASK,
    ISSUE,
    COMMUNICATION,
    NOTIFICATION
}
