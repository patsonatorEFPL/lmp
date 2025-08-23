package com.lmp.service.payment;

import com.lmp.domain.entity.Order;
import com.lmp.domain.entity.PaymentTransaction;
import com.lmp.service.payment.dto.PaymentRequestDto;
import com.lmp.service.payment.dto.PaymentResponseDto;
import com.lmp.service.payment.dto.RefundRequestDto;
import com.lmp.service.payment.dto.RefundResponseDto;
import com.lmp.service.payment.dto.WebhookEventDto;
import com.lmp.service.payment.exception.PaymentProcessingException;
import com.lmp.service.payment.exception.PaymentValidationException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interface du service de paiement principal
 * Orchestre les différents processeurs de paiement et gère la persistance
 */
public interface PaymentService {
    
    /**
     * Traite un paiement pour une commande
     * @param orderId l'ID de la commande
     * @param paymentRequest les détails du paiement
     * @return la réponse du traitement
     * @throws PaymentProcessingException si le traitement échoue
     * @throws PaymentValidationException si les données sont invalides
     */
    PaymentResponseDto processPayment(Long orderId, PaymentRequestDto paymentRequest) 
            throws PaymentProcessingException, PaymentValidationException;
    
    /**
     * Effectue un remboursement
     * @param transactionId l'ID de la transaction à rembourser
     * @param refundRequest les détails du remboursement
     * @return la réponse du remboursement
     * @throws PaymentProcessingException si le remboursement échoue
     * @throws PaymentValidationException si les données de remboursement sont invalides
     */
    RefundResponseDto refundPayment(Long transactionId, RefundRequestDto refundRequest)
            throws PaymentProcessingException, PaymentValidationException;
    
    /**
     * Vérifie le statut d'une transaction
     * @param transactionId l'ID de la transaction
     * @return le statut actuel
     * @throws PaymentProcessingException si la vérification échoue
     */
    String checkTransactionStatus(Long transactionId) throws PaymentProcessingException;
    
    /**
     * Récupère une transaction par son ID
     * @param transactionId l'ID de la transaction
     * @return la transaction
     */
    PaymentTransaction getTransaction(Long transactionId);
    
    /**
     * Récupère toutes les transactions d'une commande
     * @param orderId l'ID de la commande
     * @return la liste des transactions
     */
    List<PaymentTransaction> getTransactionsByOrder(Long orderId);
    
    /**
     * Calcule les frais de traitement pour un montant et fournisseur donnés
     * @param amount le montant
     * @param currency la devise
     * @param provider le fournisseur de paiement
     * @return les frais calculés
     */
    BigDecimal calculateProcessingFees(BigDecimal amount, String currency, String provider);
    
    /**
     * Vérifie si un fournisseur de paiement est disponible
     * @param provider le nom du fournisseur
     * @return true si disponible
     */
    boolean isProviderAvailable(String provider);
    
    /**
     * Récupère la liste des fournisseurs de paiement supportés
     * @return la liste des fournisseurs
     */
    List<String> getSupportedProviders();
    
    /**
     * Traite un événement de webhook
     * @param provider le fournisseur de paiement
     * @param payload le contenu du webhook
     * @param signature la signature de sécurité
     * @return l'événement traité
     * @throws PaymentProcessingException si le traitement échoue
     */
    WebhookEventDto processWebhook(String provider, String payload, String signature) 
            throws PaymentProcessingException;
    
    /**
     * Valide les données de paiement avant traitement
     * @param paymentRequest les données à valider
     * @param provider le fournisseur de paiement
     * @throws PaymentValidationException si les données sont invalides
     */
    void validatePaymentRequest(PaymentRequestDto paymentRequest, String provider) 
            throws PaymentValidationException;
    
    /**
     * Récupère les statistiques de paiement
     * @param orderId l'ID de la commande (optionnel)
     * @return les statistiques
     */
    PaymentStatistics getPaymentStatistics(Long orderId);
    
    /**
     * Classe interne pour les statistiques de paiement
     */
    class PaymentStatistics {
        private BigDecimal totalAmount;
        private BigDecimal totalFees;
        private int successfulPayments;
        private int failedPayments;
        private int refundedPayments;
        
        // Constructeurs
        public PaymentStatistics() {}
        
        public PaymentStatistics(BigDecimal totalAmount, BigDecimal totalFees, 
                               int successfulPayments, int failedPayments, int refundedPayments) {
            this.totalAmount = totalAmount;
            this.totalFees = totalFees;
            this.successfulPayments = successfulPayments;
            this.failedPayments = failedPayments;
            this.refundedPayments = refundedPayments;
        }
        
        // Getters et Setters
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        
        public BigDecimal getTotalFees() { return totalFees; }
        public void setTotalFees(BigDecimal totalFees) { this.totalFees = totalFees; }
        
        public int getSuccessfulPayments() { return successfulPayments; }
        public void setSuccessfulPayments(int successfulPayments) { this.successfulPayments = successfulPayments; }
        
        public int getFailedPayments() { return failedPayments; }
        public void setFailedPayments(int failedPayments) { this.failedPayments = failedPayments; }
        
        public int getRefundedPayments() { return refundedPayments; }
        public void setRefundedPayments(int refundedPayments) { this.refundedPayments = refundedPayments; }
        
        public int getTotalPayments() {
            return successfulPayments + failedPayments + refundedPayments;
        }
        
        public double getSuccessRate() {
            int total = getTotalPayments();
            return total > 0 ? (double) successfulPayments / total * 100 : 0.0;
        }
    }
}