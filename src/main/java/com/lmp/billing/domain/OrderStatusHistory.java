package com.lmp.billing.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import com.lmp.auth.domain.User;
import com.lmp.billing.domain.OrderStatus;

import jakarta.persistence.*;

@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 50)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 50)
    private OrderStatus toStatus;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "changed_by", length = 100)
    private String changedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    public OrderStatusHistory() {}

    public OrderStatusHistory(Order order, OrderStatus fromStatus, OrderStatus toStatus,
                            LocalDateTime changedAt, String changedBy, String note) {
        this.order = order;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedAt = changedAt;
        this.changedBy = changedBy;
        this.note = note;
    }
    
    public OrderStatusHistory(Order order, OrderStatus fromStatus, OrderStatus toStatus,
                            LocalDateTime changedAt, User createdBy, String note) {
        this.order = order;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedAt = changedAt;
        this.createdBy = createdBy;
        this.changedBy = createdBy != null ? createdBy.getEmail() : null;
        this.note = note;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public OrderStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(OrderStatus fromStatus) { this.fromStatus = fromStatus; }
    public OrderStatus getToStatus() { return toStatus; }
    public void setToStatus(OrderStatus toStatus) { this.toStatus = toStatus; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
        this.changedBy = createdBy != null ? createdBy.getEmail() : null;
    }
}
