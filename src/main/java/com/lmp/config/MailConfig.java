package com.lmp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configuration centralisée pour l'envoi d'emails avec Mailtrap
 */
@Configuration
public class MailConfig {

    private static final Logger logger = LoggerFactory.getLogger(MailConfig.class);

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private String auth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private String starttls;

    @Value("${spring.mail.properties.mail.smtp.debug:false}")
    private String debug;

    @Bean
    public JavaMailSender mailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        // Configuration de base
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        
        // Configuration des propriétés SMTP
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", auth);
        props.put("mail.smtp.starttls.enable", starttls);
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.enable", "false");
        props.put("mail.debug", debug);
        
        // Timeouts pour éviter les blocages
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        
        // Configuration SSL/TLS pour Mailtrap
        if (host.contains("mailtrap.io")) {
            props.put("mail.smtp.ssl.trust", host);
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            
            // Configuration spécifique pour le sandbox Mailtrap
            if (host.contains("sandbox.smtp.mailtrap.io")) {
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.starttls.required", "false");
            }
        }
        
        logger.info("Configuration JavaMailSender - Host: {}, Port: {}, User: {}", 
                   host, port, maskEmail(username));
        logger.debug("Propriétés SMTP configurées: {}", props);
        
        return mailSender;
    }

    /**
     * Masque l'email pour les logs (affiche seulement les premiers caractères)
     */
    private String maskEmail(String email) {
        if (email == null || email.length() <= 3) {
            return "***";
        }
        return email.substring(0, 3) + "***";
    }
}
