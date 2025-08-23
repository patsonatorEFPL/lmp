package com.lmp.config;

import com.lmp.web.dto.PurchaseIntent;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Gestionnaire personnalisé de succès d'authentification qui vérifie
 * s'il y a une intention de paiement en session et redirige en conséquence.
 * 
 * Flux :
 * 1. Utilisateur anonyme sélectionne un service → intention stockée en session
 * 2. Utilisateur se connecte ou s'inscrit → ce handler est appelé
 * 3. Si intention trouvée → redirection vers dashboard avec paramètre
 * 4. Dashboard détecte le paramètre → redirection automatique vers Stripe
 */
@Component
public class PurchaseIntentAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(PurchaseIntentAuthenticationSuccessHandler.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT." + PurchaseIntentAuthenticationSuccessHandler.class.getName());
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                      HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        
        String userEmail = authentication.getName();
        HttpSession session = request.getSession();
        
        logger.info("Authentication success for user: {}", userEmail);
        auditLogger.info("User authenticated successfully - Email: {}, IP: {}", 
                        userEmail, getClientIpAddress(request));
        
        try {
            // Vérifier s'il y a une intention de paiement en session
            PurchaseIntent purchaseIntent = (PurchaseIntent) session.getAttribute("pendingPurchaseIntent");
            
            if (purchaseIntent != null && purchaseIntent.isValid()) {
                
                logger.info("Purchase intent found for user {}: {} - Amount: {} {}", 
                           userEmail, purchaseIntent.getServiceName(), 
                           purchaseIntent.getAmount(), purchaseIntent.getCurrency());
                
                auditLogger.info("Purchase intent detected post-authentication - User: {}, Service: {}, Amount: {} {}", 
                               userEmail, purchaseIntent.getServiceName(), 
                               purchaseIntent.getAmount(), purchaseIntent.getCurrency());
                
                // Rediriger vers le dashboard avec un paramètre indiquant qu'il faut traiter le paiement
                String redirectUrl = "/dashboard?processPurchase=true";
                
                logger.info("Redirecting user {} to {} for purchase processing", userEmail, redirectUrl);
                response.sendRedirect(redirectUrl);
                return;
                
            } else {
                if (purchaseIntent != null && !purchaseIntent.isValid()) {
                    // Nettoyer l'intention expirée
                    session.removeAttribute("pendingPurchaseIntent");
                    logger.info("Expired purchase intent removed for user: {}", userEmail);
                }
                
                logger.info("No valid purchase intent found for user: {}", userEmail);
            }
            
        } catch (Exception e) {
            logger.error("Error processing purchase intent for user {}: {}", userEmail, e.getMessage(), e);
            // Continue avec la redirection normale même en cas d'erreur
        }
        
        // Redirection normale vers le dashboard
        String defaultRedirectUrl = "/dashboard";
        logger.info("Standard authentication redirect for user {} to {}", userEmail, defaultRedirectUrl);
        response.sendRedirect(defaultRedirectUrl);
    }
    
    /**
     * Utilitaire pour récupérer l'adresse IP du client
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}