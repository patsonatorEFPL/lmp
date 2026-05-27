package com.lmp.auth.config;

import com.lmp.auth.domain.User;
import com.lmp.auth.repository.UserRepository;
import com.lmp.auth.dto.PurchaseIntent;
import com.lmp.shared.web.ClientIpResolver;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

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

        private final UserRepository userRepository;

    /** Base URL du site principal — utilisée pour redirects absolus cross-host
     *  (form login fire sur auth.*, mais le user doit atterrir sur le site principal). */
    @org.springframework.beans.factory.annotation.Value("${app.base.url:}")
    private String baseUrl;


    public PurchaseIntentAuthenticationSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Préfixe le path avec baseUrl pour cross-host redirect (form login sur auth.* → site sur baseUrl). */
    private String absoluteUrl(String path) {
        if (baseUrl == null || baseUrl.isBlank()) return path;
        return baseUrl + path;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                      HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        
        String userEmail = authentication.getName();
        HttpSession session = request.getSession();
        
        logger.info("Authentication success for user: {}", userEmail);
        auditLogger.info("User authenticated successfully - Email: {}, IP: {}", 
                        userEmail, ClientIpResolver.resolve(request));

        // Mettre à jour la date de dernière connexion
        try {
            Optional<User> userOpt = userRepository.findByEmail(userEmail);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setLastLoginDate(LocalDateTime.now());
                userRepository.save(user);
                logger.info("Last login date updated for user: {}", userEmail);
            }
        } catch (Exception e) {
            logger.error("Error updating last login date for {}: {}", userEmail, e.getMessage());
        }
        
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
                String redirectUrl = absoluteUrl("/dashboard?processPurchase=true");

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
        
        // Vérifier s'il y a une requête sauvegardée (ex: /oauth2/authorize en flow SSO)
        Object savedRequestObj = request.getSession().getAttribute("SPRING_SECURITY_SAVED_REQUEST");
        if (savedRequestObj instanceof DefaultSavedRequest savedRequest) {
            String targetUrl = savedRequest.getRequestURL();
            if (savedRequest.getQueryString() != null) {
                targetUrl += "?" + savedRequest.getQueryString();
            }
            logger.info("Redirecting user {} to saved request: {}", userEmail, targetUrl);
            response.sendRedirect(targetUrl);
            return;
        }

        // Redirection normale vers le dashboard sur le site principal (cross-host depuis auth.*).
        String defaultRedirectUrl = absoluteUrl("/dashboard");
        logger.info("Standard authentication redirect for user {} to {}", userEmail, defaultRedirectUrl);
        response.sendRedirect(defaultRedirectUrl);
    }
}