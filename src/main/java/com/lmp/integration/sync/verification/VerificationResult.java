package com.lmp.integration.sync.verification;

import java.time.LocalDateTime;

/**
 * Résultat d'une vérification post-sync.
 * Immutable — utilisé par les verifiers pour communiquer le résultat.
 */
public record VerificationResult(
        boolean verified,
        String errorMessage
) {

    /** Vérification réussie — le document existe côté externe avec le bon état. */
    public static VerificationResult ok() {
        return new VerificationResult(true, null);
    }

    /** Vérification échouée — le document existe mais avec un état inattendu. */
    public static VerificationResult mismatch(String reason) {
        return new VerificationResult(false, reason);
    }

    /** Vérification impossible — le document n'est pas récupérable. */
    public static VerificationResult unreachable(String reason) {
        return new VerificationResult(false, "UNREACHABLE: " + reason);
    }
}
