package com.lmp.integration.sync.verification;

import com.lmp.integration.sync.SyncEntityType;

/**
 * Vérifie qu'une entité synchronisée existe réellement côté système externe
 * avec l'état attendu (docstatus, montants, etc.).
 * <p>
 * Chaque implémentation gère un ou plusieurs {@link SyncEntityType}.
 */
public interface SyncVerifier {

    /**
     * Types d'entités supportés par ce verifier.
     */
    boolean supports(SyncEntityType entityType);

    /**
     * Vérifie l'entité synchronisée côté système externe.
     *
     * @param entityType type de l'entité
     * @param externalId identifiant externe (nom du document)
     * @return résultat de la vérification
     */
    VerificationResult verify(SyncEntityType entityType, String externalId);
}
