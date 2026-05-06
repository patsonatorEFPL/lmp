package com.lmp.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration centralisée pour les adresses email avec routing Cloudflare.
 * 
 * Cette classe gère la stratégie d'adresses email :
 * - noreply@ : emails transactionnels (avec Reply-To cohérent)
 * - support@ : emails bidirectionnels de support
 * 
 * Les adresses sont configurées via les propriétés mail.from.* 
 * (variables d'environnement MAIL_FROM_NOREPLY, MAIL_FROM_SUPPORT, etc.)
 */
@Component
@ConfigurationProperties(prefix = "mail.from")
public class MailAddressConfig {
    
    /**
     * Adresse noreply pour emails transactionnels
     * (confirmations de commande, bienvenue, notifications système)
     */
    private String noreply = "noreply@localhost";
    
    /**
     * Adresse support pour emails bidirectionnels (support client)
     */
    private String support = "support@localhost";

    /**
     * Adresse contact pour les formulaires publics du site
     */
    private String contact = "info@localhost";
    
    /**
     * Configuration du Reply-To pour les emails noreply (même adresse que l'expéditeur)
     */
    private String replyToSupport = "noreply@localhost";
    
    /**
     * Nom affiché pour l'expéditeur
     */
    private String name = "LMP Digital Services";
    
    // === GETTERS ET SETTERS ===
    
    /**
     * Obtient l'adresse noreply pour les emails transactionnels
     */
    public String getNoreply() {
        return noreply;
    }
    
    public void setNoreply(String noreply) {
        this.noreply = noreply;
    }
    
    /**
     * Obtient l'adresse support pour les emails bidirectionnels
     */
    public String getSupport() {
        return support;
    }
    
    public void setSupport(String support) {
        this.support = support;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }
    
    /**
     * Obtient l'adresse Reply-To pour les emails noreply
     */
    public String getReplyToSupport() {
        return replyToSupport;
    }
    
    public void setReplyToSupport(String replyToSupport) {
        this.replyToSupport = replyToSupport;
    }
    
    /**
     * Obtient le nom affiché pour l'expéditeur
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
