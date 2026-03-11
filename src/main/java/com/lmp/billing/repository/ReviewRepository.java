package com.lmp.billing.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.Review;
import com.lmp.auth.domain.User;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByUser(User user);
    List<Review> findByOrder(Order order);
    Boolean existsByUserAndOrder(User user, Order order);
    List<Review> findByFeaturedTrueAndAdminApprovedTrueAndExpiresAtAfter(LocalDateTime dateTime);
    List<Review> findByAdminApprovedFalse();
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
