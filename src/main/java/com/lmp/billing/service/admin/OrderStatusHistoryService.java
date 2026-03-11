package com.lmp.billing.service.admin;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderStatusHistory;
import com.lmp.billing.domain.OrderStatus;
import com.lmp.billing.repository.OrderStatusHistoryRepository;
import com.lmp.billing.dto.admin.OrderStatusHistoryDto;

/**
 * Service pour la gestion de l'historique des changements de statut des commandes.
 * Permet l'audit complet et la traçabilité des modifications.
 */
@Service
@Transactional
public class OrderStatusHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(OrderStatusHistoryService.class);

        private final OrderStatusHistoryRepository historyRepository;


    public OrderStatusHistoryService(OrderStatusHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    /**
     * Enregistre un changement de statut dans l'historique
     */
    public OrderStatusHistory recordStatusChange(Order order, OrderStatus fromStatus, 
                                               OrderStatus toStatus, String note) {
        logger.info("Enregistrement changement statut commande {}: {} -> {}", 
            order.getId(), fromStatus, toStatus);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setChangedAt(LocalDateTime.now());
        history.setNote(note);
        history.setChangedBy("ADMIN"); // TODO: Récupérer l'utilisateur connecté

        return historyRepository.save(history);
    }

    /**
     * Récupère l'historique complet d'une commande
     */
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryDto> getOrderHistory(java.util.UUID orderId) {
        List<OrderStatusHistory> history = historyRepository.findByOrderIdOrderByChangedAtDesc(orderId);
        
        return history.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    /**
     * Récupère l'historique avec pagination
     */
    @Transactional(readOnly = true)
    public Page<OrderStatusHistoryDto> getOrderHistoryPaged(java.util.UUID orderId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "changedAt"));
        Page<OrderStatusHistory> history = historyRepository.findByOrderId(orderId, pageable);
        
        return history.map(this::convertToDto);
    }

    /**
     * Récupère l'historique global avec filtres
     */
    @Transactional(readOnly = true)
    public Page<OrderStatusHistoryDto> getGlobalHistory(LocalDateTime startDate, LocalDateTime endDate, 
                                                       OrderStatus fromStatus, OrderStatus toStatus,
                                                       int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "changedAt"));
        Page<OrderStatusHistory> history = historyRepository.findHistoryWithFilters(
            startDate, endDate, fromStatus, toStatus, pageable);
        
        return history.map(this::convertToDto);
    }

    /**
     * Statistiques sur les changements de statut
     */
    @Transactional(readOnly = true)
    public List<Object[]> getStatusChangeStats(LocalDateTime startDate, LocalDateTime endDate) {
        return historyRepository.getStatusChangeStats(startDate, endDate);
    }

    /**
     * Temps moyen par transition de statut
     */
    @Transactional(readOnly = true)
    public List<Object[]> getAverageTransitionTimes(LocalDateTime startDate, LocalDateTime endDate) {
        return historyRepository.getAverageTransitionTimes(startDate, endDate);
    }

    /**
     * Conversion vers DTO
     */
    private OrderStatusHistoryDto convertToDto(OrderStatusHistory history) {
        OrderStatusHistoryDto dto = new OrderStatusHistoryDto();
        
        dto.setId(history.getId());
        dto.setOrderId(history.getOrder().getId());
        dto.setFromStatus(history.getFromStatus());
        dto.setToStatus(history.getToStatus());
        dto.setChangedAt(history.getChangedAt());
        dto.setNote(history.getNote());
        dto.setChangedBy(history.getChangedBy());
        
        // Informations additionnelles sur la commande
        if (history.getOrder() != null) {
            dto.setOrderServiceName(history.getOrder().getServiceName());
            if (history.getOrder().getUser() != null) {
                dto.setCustomerEmail(history.getOrder().getUser().getEmail());
            }
        }
        
        return dto;
    }
}