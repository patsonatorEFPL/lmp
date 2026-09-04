package com.lmp.auth.config;

import com.lmp.auth.domain.User;
import com.lmp.auth.repository.UserRepository;
import com.lmp.auth.dto.PurchaseIntent;
import com.lmp.shared.web.ClientIpResolver;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Gestionnaire de succès d'authentification.
 *
 * Étend {@link SavedRequestAwareAuthenticationSuccessHandler} pour réutiliser
 * la mécanique standard Spring Security :
 *  - Lecture du saved request via {@link RequestCache} (abstraction stable, pas
 *    de lecture brute de l'attribut session {@code SPRING_SECURITY_SAVED_REQUEST}).
 *  - Support natif des replays POST, {@code targetUrlParameter},
 *    {@code alwaysUseDefaultTargetUrl}, {@code useReferer}.
 *  - Délégation à la {@code RedirectStrategy} configurée.
 *
 * Surcharges métier :
 *  - Mise à jour de {@code lastLoginDate} et audit log.
 *  - Court-circuit vers {@code /dashboard?processPurchase=true} si une
 *    intention de paiement valide est en session ; le saved request éventuel
 *    est purgé pour éviter un orphelin dans Redis.
 *
 * Cross-host : le default target URL est préfixé par {@code app.base.url}
 * (form login fire sur {@code auth.*} ; user doit atterrir sur le site
 * principal). Les saved requests stockent déjà l'URL absolue, donc rien à
 * faire de notre côté pour ce cas.
 */
@Component
public class PurchaseIntentAuthenticationSuccessHandler
        extends SavedRequestAwareAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseIntentAuthenticationSuccessHandler.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT." + PurchaseIntentAuthenticationSuccessHandler.class.getName());

    private static final String PURCHASE_INTENT_SESSION_KEY = "pendingPurchaseIntent";

    private final UserRepository userRepository;
    private final RequestCache requestCache = new HttpSessionRequestCache();

    @org.springframework.beans.factory.annotation.Value("${app.base.url:}")
    private String baseUrl;

    public PurchaseIntentAuthenticationSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
        setRequestCache(requestCache);
    }

    @PostConstruct
    void configureDefaultTarget() {
        setDefaultTargetUrl("/dashboard");
    }

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

        updateLastLoginDate(userEmail);

        if (handlePurchaseIntent(request, response, session, userEmail)) {
            return;
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }

    private void updateLastLoginDate(String userEmail) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(userEmail);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setLastLoginDate(LocalDateTime.now());
                userRepository.save(user);
            }
        } catch (Exception e) {
            logger.error("Error updating last login date for {}: {}", userEmail, e.getMessage());
        }
    }

    /**
     * @return {@code true} si une intention de paiement a déclenché une redirect
     *         (la response est alors committed et l'appelant doit return).
     */
    private boolean handlePurchaseIntent(HttpServletRequest request,
                                         HttpServletResponse response,
                                         HttpSession session,
                                         String userEmail) throws IOException {
        try {
            PurchaseIntent intent = (PurchaseIntent) session.getAttribute(PURCHASE_INTENT_SESSION_KEY);
            if (intent == null) {
                return false;
            }
            if (!intent.isValid()) {
                session.removeAttribute(PURCHASE_INTENT_SESSION_KEY);
                logger.info("Expired purchase intent removed for user: {}", userEmail);
                return false;
            }

            auditLogger.info("Purchase intent detected post-authentication - User: {}, Service: {}, Amount: {} {}",
                    userEmail, intent.getServiceName(), intent.getAmount(), intent.getCurrency());

            // Purge le saved request : on bypass volontairement le retour
            // vers une URL pré-login (ex. /oauth2/authorize) au profit du flow
            // achat. Sinon un saved request orphelin reste en session/Redis.
            requestCache.removeRequest(request, response);

            String redirectUrl = "/dashboard?processPurchase=true";
            logger.info("Redirecting user {} to {} for purchase processing", userEmail, redirectUrl);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            return true;

        } catch (Exception e) {
            logger.error("Error processing purchase intent for user {}: {}", userEmail, e.getMessage(), e);
            return false;
        }
    }
}
