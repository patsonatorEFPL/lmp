package com.lmp.integration.sync;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Client générique pour communiquer avec un système externe.
 * <p>
 * Aucune référence à un ERP spécifique — piloté par configuration.
 * Implémentations :
 * <ul>
 *   <li>{@code NoOpExternalClient} — log uniquement (dev, tests, fallback)</li>
 *   <li>{@code RestExternalClient} — appels REST (production)</li>
 * </ul>
 */
public interface ExternalSystemClient {

    /**
     * Vérifie si le système externe est accessible.
     */
    boolean isAvailable();

    /**
     * Crée une entité dans le système externe.
     *
     * @param type type d'entité (CUSTOMER, ITEM, etc.)
     * @param data payload de l'entité à créer
     * @return réponse avec l'ID externe attribué
     */
    ExternalResponse createEntity(SyncEntityType type, Map<String, Object> data);

    /**
     * Met à jour une entité existante dans le système externe.
     *
     * @param type       type d'entité
     * @param externalId identifiant côté système externe
     * @param data       champs à mettre à jour
     */
    ExternalResponse updateEntity(SyncEntityType type, String externalId, Map<String, Object> data);

    /**
     * Récupère une entité par son ID externe.
     */
    ExternalResponse getEntity(SyncEntityType type, String externalId);

    /**
     * Supprime une entité du système externe.
     * <p>
     * Pour les documents soumissibles (Sales Order, Sales Invoice, Payment Entry),
     * l'implémentation doit annuler le document avant de le supprimer.
     *
     * @param type       type d'entité
     * @param externalId identifiant côté système externe
     * @return réponse de l'opération
     */
    ExternalResponse deleteEntity(SyncEntityType type, String externalId);

    /**
     * Liste les entités liées à une entité parente dans le système externe.
     * <p>
     * Utilisé pour la suppression en cascade : trouver les documents enfants
     * (factures, commandes, contacts) liés à un Customer avant sa suppression.
     *
     * @param type          type d'entité enfant à chercher
     * @param parentField   nom du champ de liaison côté système externe (ex: "customer")
     * @param parentId      identifiant de l'entité parente
     * @return liste des identifiants des entités liées
     */
    List<String> listLinkedEntityIds(SyncEntityType type, String parentField, String parentId);

    /**
     * Liste les entités modifiées depuis un instant donné (pour réconciliation).
     */
    List<Map<String, Object>> listEntities(SyncEntityType type, Instant modifiedSince);
}
