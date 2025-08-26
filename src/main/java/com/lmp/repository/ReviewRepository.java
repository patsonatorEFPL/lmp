package com.lmp.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lmp.domain.entity.Order;
import com.lmp.domain.entity.Review;
import com.lmp.domain.entity.User;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByUser(User user);
    List<Review> findByOrder(Order order);
    Boolean existsByUserAndOrder(User user, Order order);
    List<Review> findByFeaturedTrueAndAdminApprovedTrueAndExpiresAtAfter(LocalDateTime dateTime);
    List<Review> findByAdminApprovedFalse();
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
