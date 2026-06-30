package com.lmp.integration.sync.verification;

import com.lmp.integration.sync.SyncEntityType;
import com.lmp.integration.sync.domain.SyncEvent;
import com.lmp.integration.sync.repository.SyncEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Orchestre la vérification post-sync pour les événements SUCCESS.
 * <p>
 * Appelé inline dans {@code SyncOutboundService.handleResponse()} après un sync réussi.
 * Pas de scheduler séparé — la vérification est immédiate et non-bloquante.
 * <p>
 * Si la vérification échoue, l'événement reste SUCCESS (le document a été créé côté ERP)
 * mais {@code verified_at} reste null et {@code verification_error} est renseigné.
 * Le {@code SyncReconciliationService} signale ensuite les cas non-vérifiés > 1h.
 */
@Service
public class SyncVerificationService {

    private static final Logger log = LoggerFactory.getLogger(SyncVerificationService.class);

    private final List<SyncVerifier> verifiers;
    private final SyncEventRepository syncEventRepository;

    public SyncVerificationService(List<SyncVerifier> verifiers,
                                    SyncEventRepository syncEventRepository) {
        this.verifiers = verifiers;
        this.syncEventRepository = syncEventRepository;
    }

    /**
     * Vérifie un événement SUCCESS côté système externe.
     * Met à jour {@code verified_at} ou {@code verification_error} sur l'événement.
     * <p>
     * Ne lance pas d'exception — la vérification est best-effort et ne doit pas
     * bloquer le callback chain (SO → SINV → PE).
     *
     * @param syncEvent l'événement à vérifier (doit être en statut SUCCESS)
     */
    public void verifyAfterSync(SyncEvent syncEvent) {
        SyncEntityType entityType = syncEvent.getEntityType();
        String externalId = syncEvent.getExternalEntityId();

        // Ne vérifier que les CREATIONs — les updates/deletes n'ont pas besoin de verify
        if (!"CREATED".equals(syncEvent.getEventType())) {
            syncEvent.setVerifiedAt(LocalDateTime.now());
            return;
        }

        SyncVerifier verifier = findVerifier(entityType);
        if (verifier == null) {
            // Pas de verifier pour ce type → marquer comme vérifié par défaut
            syncEvent.setVerifiedAt(LocalDateTime.now());
            log.debug("[VERIFY] No verifier for {} — auto-verified", entityType);
            return;
        }

        try {
            VerificationResult result = verifier.verify(entityType, externalId);

            if (result.verified()) {
                syncEvent.setVerifiedAt(LocalDateTime.now());
                syncEvent.setVerificationError(null);
                log.info("[VERIFY] {} {} verified successfully", entityType, externalId);
            } else {
                syncEvent.setVerificationError(result.errorMessage());
                log.warn("[VERIFY] {} {} verification failed: {}",
                        entityType, externalId, result.errorMessage());
            }
        } catch (Exception e) {
            syncEvent.setVerificationError("Exception: " + e.getMessage());
            log.warn("[VERIFY] {} {} verification exception: {}",
                    entityType, externalId, e.getMessage());
        }
    }

    /**
     * Tente de re-vérifier les événements SUCCESS qui n'ont pas encore de verified_at.
     * Appelé par le {@code SyncReconciliationService} lors de ses cycles.
     *
     * @return nombre d'événements nouvellement vérifiés
     */
    public int retryUnverifiedEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
        List<SyncEvent> unverified = syncEventRepository.findUnverifiedSuccessEvents(cutoff, 20);

        if (unverified.isEmpty()) return 0;

        log.info("[VERIFY] Retrying verification for {} unverified events", unverified.size());
        int verified = 0;

        for (SyncEvent event : unverified) {
            SyncVerifier verifier = findVerifier(event.getEntityType());
            if (verifier == null) {
                event.setVerifiedAt(LocalDateTime.now());
                syncEventRepository.save(event);
                verified++;
                continue;
            }

            try {
                VerificationResult result = verifier.verify(event.getEntityType(), event.getExternalEntityId());
                if (result.verified()) {
                    event.setVerifiedAt(LocalDateTime.now());
                    event.setVerificationError(null);
                    verified++;
                    log.info("[VERIFY RETRY] {} {} now verified",
                            event.getEntityType(), event.getExternalEntityId());
                } else {
                    event.setVerificationError(result.errorMessage());
                    log.warn("[VERIFY RETRY] {} {} still failing: {}",
                            event.getEntityType(), event.getExternalEntityId(), result.errorMessage());
                }
            } catch (Exception e) {
                event.setVerificationError("Retry exception: " + e.getMessage());
            }
            syncEventRepository.save(event);
        }

        return verified;
    }

    /**
     * Compte les événements SUCCESS non vérifiés plus anciens qu'un seuil (alerting).
     */
    public long countStaleUnverifiedEvents(LocalDateTime olderThan) {
        return syncEventRepository.countUnverifiedSuccessOlderThan(olderThan);
    }

    private SyncVerifier findVerifier(SyncEntityType entityType) {
        return verifiers.stream()
                .filter(v -> v.supports(entityType))
                .findFirst()
                .orElse(null);
    }
}
