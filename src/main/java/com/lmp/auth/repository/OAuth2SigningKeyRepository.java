package com.lmp.auth.repository;

import com.lmp.auth.domain.OAuth2SigningKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OAuth2SigningKeyRepository extends JpaRepository<OAuth2SigningKey, UUID> {

    List<OAuth2SigningKey> findByActiveTrueOrderByCreatedAtDesc();
}
