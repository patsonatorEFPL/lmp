package com.lmp.service.invoice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service de cache temporaire pour les factures PDF.
 * Les factures générées sont mises en cache pendant 5 minutes
 * pour éviter de surcharger le serveur si l'utilisateur clique plusieurs fois.
 */
@Service
public class InvoiceCacheService {
    
    private static final Logger log = LoggerFactory.getLogger(InvoiceCacheService.class);
    
    // Durée de vie du cache en minutes
    private static final int CACHE_TTL_MINUTES = 5;
    
    // Cache des PDF générés avec leur date d'expiration
    private final Map<String, CachedInvoice> cache = new ConcurrentHashMap<>();
    
    /**
     * Entrée de cache pour une facture
     */
    private static class CachedInvoice {
        final byte[] pdfData;
        final LocalDateTime expiresAt;
        final String invoiceNumber;
        
        CachedInvoice(byte[] pdfData, String invoiceNumber) {
            this.pdfData = pdfData;
            this.invoiceNumber = invoiceNumber;
            this.expiresAt = LocalDateTime.now().plusMinutes(CACHE_TTL_MINUTES);
        }
        
        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }
    
    /**
     * Récupère une facture du cache si elle existe et n'est pas expirée.
     * 
     * @param orderId L'ID de la commande
     * @return Les données PDF ou null si non trouvé/expiré
     */
    public byte[] get(UUID orderId) {
        String key = generateKey(orderId);
        CachedInvoice cached = cache.get(key);
        
        if (cached == null) {
            log.debug("Cache miss pour la facture de la commande #{}", orderId);
            return null;
        }
        
        if (cached.isExpired()) {
            log.debug("Cache expiré pour la facture de la commande #{}", orderId);
            cache.remove(key);
            return null;
        }
        
        log.debug("Cache hit pour la facture de la commande #{}", orderId);
        return cached.pdfData;
    }
    
    /**
     * Récupère le numéro de facture du cache.
     * 
     * @param orderId L'ID de la commande
     * @return Le numéro de facture ou null si non trouvé
     */
    public String getInvoiceNumber(UUID orderId) {
        String key = generateKey(orderId);
        CachedInvoice cached = cache.get(key);
        
        if (cached == null || cached.isExpired()) {
            return null;
        }
        
        return cached.invoiceNumber;
    }
    
    /**
     * Met en cache une facture générée.
     * 
     * @param orderId L'ID de la commande
     * @param pdfData Les données PDF
     * @param invoiceNumber Le numéro de facture
     */
    public void put(UUID orderId, byte[] pdfData, String invoiceNumber) {
        String key = generateKey(orderId);
        cache.put(key, new CachedInvoice(pdfData, invoiceNumber));
        log.debug("Facture mise en cache pour la commande #{}, expire dans {} minutes", 
                 orderId, CACHE_TTL_MINUTES);
    }
    
    /**
     * Invalide le cache pour une commande spécifique.
     * Utile si les données de la commande changent.
     * 
     * @param orderId L'ID de la commande
     */
    public void invalidate(UUID orderId) {
        String key = generateKey(orderId);
        cache.remove(key);
        log.debug("Cache invalidé pour la commande #{}", orderId);
    }
    
    /**
     * Nettoie les entrées expirées du cache.
     * Exécuté toutes les 5 minutes.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupExpiredEntries() {
        int initialSize = cache.size();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int removed = initialSize - cache.size();
        
        if (removed > 0) {
            log.info("Nettoyage du cache des factures: {} entrées supprimées, {} restantes", 
                    removed, cache.size());
        }
    }
    
    /**
     * Retourne la taille actuelle du cache.
     */
    public int getCacheSize() {
        return cache.size();
    }
    
    /**
     * Génère la clé de cache pour une commande.
     */
    private String generateKey(UUID orderId) {
        return "invoice_" + orderId;
    }
}
