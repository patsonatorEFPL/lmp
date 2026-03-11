package com.lmp.catalog.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lmp.catalog.domain.Service;
import com.lmp.catalog.domain.ServiceBenefit;

@Repository
public interface ServiceBenefitRepository extends JpaRepository<ServiceBenefit, UUID> {
    List<ServiceBenefit> findByService(Service service);

    @Modifying
    @Query("DELETE FROM ServiceBenefit sb WHERE sb.service.id = :serviceId")
    void deleteAllByServiceId(@Param("serviceId") UUID serviceId);
}
