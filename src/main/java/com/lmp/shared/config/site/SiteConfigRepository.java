package com.lmp.shared.config.site;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteConfigRepository extends JpaRepository<SiteConfigEntry, String> {

    Optional<SiteConfigEntry> findByKey(String key);

    List<SiteConfigEntry> findAllByOrderByKeyAsc();
}
