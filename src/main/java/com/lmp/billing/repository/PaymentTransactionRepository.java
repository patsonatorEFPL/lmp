package com.lmp.billing.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.PaymentTransaction;
import com.lmp.billing.domain.PaymentStatus;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    List<PaymentTransaction> findByOrder(Order order);
    List<PaymentTransaction> findByOrderAndStatus(Order order, PaymentStatus status);
    Optional<PaymentTransaction> findByTransactionId(String transactionId);
    List<PaymentTransaction> findByStatus(PaymentStatus status);
    List<PaymentTransaction> findByPaymentProvider(String paymentProvider);
}
