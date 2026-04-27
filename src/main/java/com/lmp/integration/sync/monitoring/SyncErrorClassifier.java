package com.lmp.integration.sync.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Classifie les erreurs de synchronisation en patterns connus vs inconnus.
 * <p>
 * Détecte le "drift" côté ERPNext : une erreur jamais vue signale probablement
 * un changement de schema, une nouvelle validation, ou une régression.
 */
@Service
public class SyncErrorClassifier {

    private static final Logger log = LoggerFactory.getLogger(SyncErrorClassifier.class);

    private final SyncErrorPatternRepository patternRepository;

    // Patterns connus — initialisés en dur pour bootstrap rapide.
    // La base de données devient la source de vérité au fil du temps.
    private static final List<KnownPattern> BOOTSTRAP_PATTERNS = List.of(
            // RECOVERABLE — retryable
            new KnownPattern("TimestampMismatchError", SyncErrorPattern.Category.KNOWN_RECOVERABLE),
            new KnownPattern("modified after you have opened it", SyncErrorPattern.Category.KNOWN_RECOVERABLE),
            new KnownPattern("Connection timeout", SyncErrorPattern.Category.KNOWN_RECOVERABLE),
            new KnownPattern("Read timed out", SyncErrorPattern.Category.KNOWN_RECOVERABLE),
            new KnownPattern("Connect timed out", SyncErrorPattern.Category.KNOWN_RECOVERABLE),
            new KnownPattern("503", SyncErrorPattern.Category.KNOWN_RECOVERABLE),
            new KnownPattern("Service Unavailable", SyncErrorPattern.Category.KNOWN_RECOVERABLE),
            new KnownPattern("Bad Gateway", SyncErrorPattern.Category.KNOWN_RECOVERABLE),
            new KnownPattern("Gateway Timeout", SyncErrorPattern.Category.KNOWN_RECOVERABLE),
            new KnownPattern("EOFException", SyncErrorPattern.Category.KNOWN_RECOVERABLE),
            new KnownPattern("Broken pipe", SyncErrorPattern.Category.KNOWN_RECOVERABLE),

            // PERMANENT — nécessite une intervention humaine
            new KnownPattern("LinkValidationError", SyncErrorPattern.Category.KNOWN_PERMANENT),
            new KnownPattern("DoesNotExistError", SyncErrorPattern.Category.KNOWN_PERMANENT),
            new KnownPattern("PermissionError", SyncErrorPattern.Category.KNOWN_PERMANENT),
            new KnownPattern("ValidationError", SyncErrorPattern.Category.KNOWN_PERMANENT),
            new KnownPattern("MandatoryError", SyncErrorPattern.Category.KNOWN_PERMANENT),
            new KnownPattern("already exists", SyncErrorPattern.Category.KNOWN_PERMANENT),
            new KnownPattern("not found", SyncErrorPattern.Category.KNOWN_PERMANENT),
            new KnownPattern("Cannot map because following condition fails", SyncErrorPattern.Category.KNOWN_PERMANENT),
            new KnownPattern("docstatus=1", SyncErrorPattern.Category.KNOWN_PERMANENT),
            new KnownPattern("TypeError", SyncErrorPattern.Category.KNOWN_PERMANENT),
            new KnownPattern("FrappeTypeError", SyncErrorPattern.Category.KNOWN_PERMANENT)
    );

    public SyncErrorClassifier(SyncErrorPatternRepository patternRepository) {
        this.patternRepository = patternRepository;
    }

    /**
     * Classifie une erreur de synchronisation.
     *
     * @param errorMessage le message d'erreur brut (peut être null)
     * @param syncEventId  l'ID de l'event pour traçabilité
     * @return la catégorie identifiée
     */
    @Transactional
    public SyncErrorPattern.Category classify(String errorMessage, UUID syncEventId) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return SyncErrorPattern.Category.UNKNOWN;
        }

        String normalized = normalize(errorMessage);

        // 1. Chercher un pattern existant en base
        Optional<SyncErrorPattern> existing = findMatchingPattern(normalized);
        if (existing.isPresent()) {
            SyncErrorPattern pattern = existing.get();
            pattern.setOccurrences(pattern.getOccurrences() + 1);
            pattern.setLastSeenAt(LocalDateTime.now());
            pattern.setLastSyncEventId(syncEventId);
            patternRepository.save(pattern);
            log.debug("🔍 [SYNC CLASSIFIER] Matched existing pattern '{}' → {}",
                    pattern.getPattern(), pattern.getCategory());
            return pattern.getCategory();
        }

        // 2. Chercher dans les patterns bootstrap (si la base est vide / fraîche)
        Optional<KnownPattern> bootstrapMatch = BOOTSTRAP_PATTERNS.stream()
                .filter(kp -> normalized.contains(kp.text()))
                .findFirst();

        SyncErrorPattern.Category category = bootstrapMatch
                .map(KnownPattern::category)
                .orElse(SyncErrorPattern.Category.UNKNOWN);

        // 3. Persister le nouveau pattern (vérifier l'unicité par phrase exacte)
        String keyPhrase = extractKeyPhrase(normalized);
        Optional<SyncErrorPattern> existingByPhrase = patternRepository.findByPatternIgnoreCase(keyPhrase);
        if (existingByPhrase.isPresent()) {
            SyncErrorPattern pattern = existingByPhrase.get();
            pattern.setOccurrences(pattern.getOccurrences() + 1);
            pattern.setLastSeenAt(LocalDateTime.now());
            pattern.setLastSyncEventId(syncEventId);
            patternRepository.save(pattern);
            log.debug("🔍 [SYNC CLASSIFIER] Matched existing pattern by exact phrase '{}' → {}",
                    pattern.getPattern(), pattern.getCategory());
            return pattern.getCategory();
        }

        SyncErrorPattern newPattern = new SyncErrorPattern();
        newPattern.setPattern(keyPhrase);
        newPattern.setCategory(category);
        newPattern.setLastSyncEventId(syncEventId);
        newPattern.setAlerted(false);
        patternRepository.save(newPattern);

        if (category == SyncErrorPattern.Category.UNKNOWN) {
            log.warn("🚨 [SYNC CLASSIFIER] NEW UNKNOWN pattern detected: '{}' — potential ERP drift!",
                    truncate(newPattern.getPattern(), 200));
        } else {
            log.info("📋 [SYNC CLASSIFIER] New pattern persisted: '{}' → {}",
                    newPattern.getPattern(), category);
        }

        return category;
    }

    /**
     * Vérifie s'il existe des patterns UNKNOWN non encore alertés.
     */
    @Transactional(readOnly = true)
    public List<SyncErrorPattern> findNewUnknownPatterns() {
        return patternRepository.findUnalertedUnknownPatterns();
    }

    /**
     * Marque un pattern comme ayant déclenché une alerte.
     */
    @Transactional
    public void markAlerted(UUID patternId) {
        patternRepository.findById(patternId).ifPresent(p -> {
            p.setAlerted(true);
            patternRepository.save(p);
        });
    }

    // --- Helpers ---

    private Optional<SyncErrorPattern> findMatchingPattern(String normalized) {
        List<SyncErrorPattern> all = patternRepository.findAll();
        return all.stream()
                .filter(p -> normalized.contains(p.getPattern().toLowerCase()))
                .findFirst();
    }

    private String normalize(String msg) {
        return msg.toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String extractKeyPhrase(String normalized) {
        // Extrait la phrase clé : première ligne ou premier segment de 120 car
        String firstLine = normalized.split("\\n")[0];
        if (firstLine.length() > 120) {
            return firstLine.substring(0, 120) + "…";
        }
        return firstLine;
    }

    private String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "…";
    }

    private record KnownPattern(String text, SyncErrorPattern.Category category) {}
}
