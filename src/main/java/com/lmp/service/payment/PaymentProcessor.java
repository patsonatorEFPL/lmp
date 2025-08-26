package com.lmp.service.payment;

import com.lmp.domain.entity.Order;
import com.lmp.domain.entity.PaymentTransaction;
import com.lmp.service.payment.dto.PaymentRequestDto;
import com.lmp.service.payment.dto.PaymentResponseDto;
import com.lmp.service.payment.dto.RefundRequestDto;
import com.lmp.service.payment.dto.RefundResponseDto;
import com.lmp.service.payment.exception.PaymentProcessingException;
import com.lmp.service.payment.exception.PaymentValidationException;

import java.math.BigDecimal;

/**
 * Interface abstraite pour tous les processeurs de paiement.
 * Implémente le pattern Strategy pour permettre l'ajout facile de nouveaux fournisseurs.
 * 
 * @author LMP Team
 * @version 1.0
 */
public interface PaymentProcessor {
    
    /**
     * Nom unique du processeur de paiement (ex: "stripe", "paypal", "square")
     * @return le nom du processeur
     */
    String getProcessorName();
    
    /**
     * Vérifie si ce processeur supporte la devise spécifiée
     * @param currency code de la devise (ex: "CAD", "USD", "EUR")
     * @return true si la devise est supportée
     */
    boolean supportsCurrency(String currency);
    
    /**
     * Traite un paiement pour une commande donnée
     * @param order la commande à payer
     * @param paymentRequest les détails du paiement
     * @return la réponse du traitement de paiement
     * @throws PaymentProcessingException si le traitement échoue
     * @throws PaymentValidationException si les données sont invalides
     */
    PaymentResponseDto processPayment(Order order, PaymentRequestDto paymentRequest) 
            throws PaymentProcessingException, PaymentValidationException;
    
    /**
     * Effectue un remboursement pour une transaction existante
     * @param transaction la transaction originale
     * @param refundRequest les détails du remboursement
     * @return la réponse du remboursement
     * @throws PaymentProcessingException si le remboursement échoue
     */
    RefundResponseDto refundPayment(PaymentTransaction transaction, RefundRequestDto refundRequest) 
            throws PaymentProcessingException;
    
    /**
     * Valide les données de paiement avant traitement
     * @param paymentRequest les données à valider
     * @throws PaymentValidationException si les données sont invalides
     */
    void validatePayment(PaymentRequestDto paymentRequest) throws PaymentValidationException;
    
    /**
     * Vérifie le statut d'une transaction auprès du fournisseur
     * @param transactionId l'ID de la transaction chez le fournisseur
     * @return le statut actuel de la transaction
     * @throws PaymentProcessingException si la vérification échoue
     */
    String checkTransactionStatus(String transactionId) throws PaymentProcessingException;
    
    /**
     * Calcule les frais de traitement pour un montant donné
     * @param amount le montant de base
     * @param currency la devise
     * @return les frais de traitement
     */
    BigDecimal calculateProcessingFees(BigDecimal amount, String currency);
    
    /**
     * Vérifie si ce processeur est actuellement disponible
     * @return true si le processeur est opérationnel
     */
    boolean isAvailable();
    
    /**
     * Retourne la version de l'API utilisée par ce processeur
     * @return la version de l'API
     */
    String getApiVersion();
}