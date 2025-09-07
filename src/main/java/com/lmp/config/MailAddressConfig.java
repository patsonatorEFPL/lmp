package com.lmp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration centralisée pour les adresses email avec routing Cloudflare.
 * 
 * Cette classe gère la stratégie d'adresses email :
 * - noreply@lmp-services.ca : emails transactionnels (avec Reply-To vers support)
 * - support@lmp-services.ca : emails bidirectionnels de support
 * 
 * Les adresses sont routées par Cloudflare vers lmp.assistance@gmail.com
 * mais les clients voient seulement les adresses professionnelles.
 */
@Component
@ConfigurationProperties(prefix = "mail.from")
public class MailAddressConfig {
    
    /**
     * Adresse noreply pour emails transactionnels
     * (confirmations de commande, bienvenue, notifications système)
     */
    private String noreply = "noreply@lmp-services.ca";
    
    /**
     * Adresse support pour emails bidirectionnels
     * (formulaires de contact, communications client-service)
     */
    private String support = "support@lmp-services.ca";
    
    /**
     * Configuration du Reply-To pour les emails noreply (même adresse que l'expéditeur)
     */
    private String replyToSupport = "noreply@lmp-services.ca";
    
    /**
     * Nom affiché pour l'expéditeur
     */
    private String name = "LMP Digital Services";
    
    // === GETTERS ET SETTERS ===
    
    /**
     * Obtient l'adresse noreply pour les emails transactionnels
     * @return noreply@lmp-services.ca
     */
    public String getNoreply() {
        return noreply;
    }
    
    public void setNoreply(String noreply) {
        this.noreply = noreply;
    }
    
    /**
     * Obtient l'adresse support pour les emails bidirectionnels
     * @return support@lmp-services.ca
     */
    public String getSupport() {
        return support;
    }
    
    public void setSupport(String support) {
        this.support = support;
    }
    
    /**
     * Obtient l'adresse Reply-To pour les emails noreply
     * @return noreply@lmp-services.ca
     */
    public String getReplyToSupport() {
        return replyToSupport;
    }
    
    public void setReplyToSupport(String replyToSupport) {
        this.replyToSupport = replyToSupport;
    }
    
    /**
     * Obtient le nom affiché pour l'expéditeur
     * @return LMP Digital Services
     */
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    // === MÉTHODES UTILITAIRES ===
    
    /**
     * Vérifie si une adresse est de type noreply
     * @param address adresse à vérifier
     * @return true si c'est une adresse noreply
     */
    public boolean isNoReplyAddress(String address) {
        return address != null && address.equalsIgnoreCase(noreply);
    }
    
    /**
     * Vérifie si une adresse est de type support
     * @param address adresse à vérifier
     * @return true si c'est une adresse support
     */
    public boolean isSupportAddress(String address) {
        return address != null && address.equalsIgnoreCase(support);
    }
    
    /**
     * Obtient l'adresse appropriée selon le type d'email
     * @param isTransactional true pour email transactionnel, false pour support
     * @return adresse email appropriée
     */
    public String getAppropriateFromAddress(boolean isTransactional) {
        return isTransactional ? noreply : support;
    }
    
    /**
     * Obtient le Reply-To approprié selon le type d'email
     * @param isTransactional true pour email transactionnel, false pour support
     * @return adresse Reply-To appropriée (même que l'expéditeur)
     */
    public String getAppropriateReplyTo(boolean isTransactional) {
        return isTransactional ? noreply : support;
    }
    
    @Override
    public String toString() {
        return "MailAddressConfig{" +
                "noreply='" + noreply + '\'' +
                ", support='" + support + '\'' +
                ", replyToSupport='" + replyToSupport + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
