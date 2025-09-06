package com.lmp.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.lmp.service.admin.NotificationService;

/**
 * Contrôleur temporaire pour tester l'email de bienvenue
 * À SUPPRIMER après les tests
 */
@Controller
public class TestWelcomeEmailController {

    private static final Logger logger = LoggerFactory.getLogger(TestWelcomeEmailController.class);

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/test-welcome-email")
    @ResponseBody
    public String diagnostic() {
        return "<html><body style='font-family: Arial; padding: 20px;'>" +
               "<h2>✅ Endpoint accessible !</h2>" +
               "<p>NotificationService injecté: " + (notificationService != null ? "OUI" : "NON") + "</p>" +
               "<p><a href='/test-welcome-email-send'>Envoyer l'email maintenant</a></p>" +
               "</body></html>";
    }
    
    @GetMapping("/test-welcome-email-send")
    @ResponseBody
    public String testWelcomeEmail() {
        String testEmail = "patsonator32@gmail.com";
        
        logger.info("=== CONTRÔLEUR DE TEST EMAIL ACCESSIBLE ===");
        
        // Test de base d'abord
        if (notificationService == null) {
            return "❌ NotificationService non injecté";
        }
        
        try {
            logger.info("=== TEST EMAIL DE BIENVENUE DIRECT ===");
            logger.info("Tentative d'envoi à: {}", testEmail);
            
            notificationService.sendTestWelcomeEmail(testEmail);
            
            logger.info("✅ Email de bienvenue envoyé avec succès !");
            
            String response = "✅ <b>Email de bienvenue envoyé avec succès !</b><br><br>" +
                   "<b>Destinataire:</b> " + testEmail + "<br>" +
                   "<b>Template utilisé:</b> emails/welcome-new-account.html<br><br>" +
                   "Vérifiez votre boîte email (et les spams si nécessaire).<br><br>" +
                   "<a href='/'>Retour à l'accueil</a> | " +
                   "<a href='/test-welcome-email'>Renvoyer l'email</a>";
            
            return "<html><body style='font-family: Arial; padding: 20px;'>" + response + "</body></html>";
            
        } catch (Exception e) {
            logger.error("❌ Erreur lors de l'envoi: {}", e.getMessage(), e);
            
            String errorResponse = "❌ <b>Erreur lors de l'envoi de l'email de bienvenue</b><br><br>" +
                   "<b>Erreur:</b> " + e.getMessage() + "<br>" +
                   "<b>Type:</b> " + e.getClass().getSimpleName() + "<br><br>" +
                   "Détails complets dans les logs de l'application.<br><br>" +
                   "<a href='/'>Retour à l'accueil</a> | " +
                   "<a href='/test-welcome-email'>Réessayer</a>";
            
            return "<html><body style='font-family: Arial; padding: 20px;'>" + errorResponse + "</body></html>";
        }
    }
}
