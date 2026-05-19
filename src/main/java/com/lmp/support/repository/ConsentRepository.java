package com.lmp.support.repository;

import com.lmp.support.domain.ConsentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConsentRepository extends JpaRepository<ConsentRecord, UUID> {

    List<ConsentRecord> findBySessionIdOrderByOccurredAtAsc(UUID sessionId);
}
