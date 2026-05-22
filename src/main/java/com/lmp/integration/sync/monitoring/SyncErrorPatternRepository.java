package com.lmp.integration.sync.monitoring;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SyncErrorPatternRepository extends JpaRepository<SyncErrorPattern, UUID> {

    Optional<SyncErrorPattern> findByPatternIgnoreCase(String pattern);

    @Query("SELECT p FROM SyncErrorPattern p WHERE p.category = 'UNKNOWN' AND p.alerted = false")
    List<SyncErrorPattern> findUnalertedUnknownPatterns();

    long countByCategoryAndAlerted(SyncErrorPattern.Category category, boolean alerted);

    @Query(value = """
            SELECT * FROM sync_error_patterns
            WHERE alerted = false
            AND category = 'UNKNOWN'
            ORDER BY last_seen_at DESC
            """, nativeQuery = true)
    List<SyncErrorPattern> findRecentUnknownPatterns(@Param("limit") int limit);

    @Modifying
    @Query("DELETE FROM SyncErrorPattern p WHERE p.occurrences = 0 AND p.lastSeenAt < :before")
    int deleteStaleWithZeroOccurrences(@Param("before") Instant before);
}
