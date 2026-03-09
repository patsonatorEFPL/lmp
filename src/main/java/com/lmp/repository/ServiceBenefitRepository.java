package com.lmp.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lmp.domain.entity.Service;
import com.lmp.domain.entity.ServiceBenefit;

@Repository
public interface ServiceBenefitRepository extends JpaRepository<ServiceBenefit, UUID> {
    List<ServiceBenefit> findByService(Service service);
}
