package com.lmp.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.lmp.auth.domain.User;
import com.lmp.auth.service.CustomUserDetailsService;

import io.sentry.Sentry;

import java.util.List;

/**
 * Service pour la gestion sécurisée des sessions utilisateur.
 * 
 * Responsable de l'invalidation des sessions actives lors d'actions administratives
 * critiques comme la suppression ou le blocage d'utilisateurs.
 */
@Service
public class SessionSecurityService {

    private static final Logger logger = LoggerFactory.getLogger(SessionSecurityService.class);

        private final SessionRegistry sessionRegistry;

        private final CustomUserDetailsService userDetailsService;


    public SessionSecurityService(SessionRegistry sessionRegistry,
                           CustomUserDetailsService userDetailsService) {
        this.sessionRegistry = sessionRegistry;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Invalide toutes les sessions actives d'un utilisateur spécifique.
     * 
     * @param user L'utilisateur dont les sessions doivent être invalidées
     * @return Le nombre de sessions invalidées
     */
    public int invalidateAllUserSessions(User user) {
        logger.warn("SESSION SECURITY - Tentative d'invalidation de toutes les sessions pour l'utilisateur: {}", user.getEmail());
        
        try {
            // Charger les détails de l'utilisateur pour la recherche de session
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            
            // Récupérer toutes les sessions actives pour cet utilisateur
            List<SessionInformation> activeSessions = sessionRegistry.getAllSessions(userDetails, false);
            
            logger.info("SESSION SECURITY - Trouvé {} session(s) active(s) pour l'utilisateur: {}", 
                       activeSessions.size(), user.getEmail());
            
            int invalidatedCount = 0;
            
            // Invalider chaque session active
            for (SessionInformation sessionInfo : activeSessions) {
                if (!sessionInfo.isExpired()) {
                    logger.warn("SESSION SECURITY - Invalidation de la session {} pour l'utilisateur: {}", 
                               sessionInfo.getSessionId(), user.getEmail());
                    
                    sessionInfo.expireNow();
                    invalidatedCount++;
                    
                    logger.info("SESSION SECURITY - Session {} invalidée avec succès", sessionInfo.getSessionId());
                } else {
                    logger.debug("SESSION SECURITY - Session {} déjà expirée", sessionInfo.getSessionId());
                }
            }
            
            logger.warn("SESSION SECURITY - RÉSULTAT: {} session(s) invalidée(s) pour l'utilisateur: {}", 
                       invalidatedCount, user.getEmail());
            
            return invalidatedCount;
            
        } catch (UsernameNotFoundException e) {
            // SECURITY : utilisateur disparu pendant la transaction (hard-delete race,
            // ou compte qui n'existe simplement plus). Pas d'erreur Sentry-worthy.
            logger.warn("SESSION SECURITY - Utilisateur {} introuvable pour invalidation sessions: {}",
                    user.getEmail(), e.getMessage());
            return 0;
        } catch (Exception e) {
            // SECURITY : exception INATTENDUE pendant lookup ou expireNow.
            // Avant ce fix le swallow silencieux faisait passer un échec d'invalidation
            // de sessions critique pour un cas "0 session trouvée" → admin pensait
            // que la sécurité avait pris effet alors qu'aucune session n'avait été
            // touchée. On log error + capture Sentry pour alerter ; le caller voit
            // toujours 0 mais l'évènement est visible côté ops.
            logger.error("SESSION SECURITY - ERREUR INATTENDUE invalidation sessions pour {} : {}",
                        user.getEmail(), e.getMessage(), e);
            if (Sentry.isEnabled()) Sentry.captureException(e);
            return 0;
        }
    }

    /**
     * Invalide toutes les sessions actives d'un utilisateur par email.
     * 
     * @param userEmail L'email de l'utilisateur
     * @return Le nombre de sessions invalidées
     */
    public int invalidateAllUserSessionsByEmail(String userEmail) {
        logger.warn("SESSION SECURITY - Tentative d'invalidation par email: {}", userEmail);
        
        try {
            // Charger les détails de l'utilisateur
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            
            // Récupérer toutes les sessions actives
            List<SessionInformation> activeSessions = sessionRegistry.getAllSessions(userDetails, false);
            
            logger.info("SESSION SECURITY - Trouvé {} session(s) active(s) pour l'email: {}", 
                       activeSessions.size(), userEmail);
            
            int invalidatedCount = 0;
            
            // Invalider chaque session active
            for (SessionInformation sessionInfo : activeSessions) {
                if (!sessionInfo.isExpired()) {
                    logger.warn("SESSION SECURITY - Invalidation de la session {} pour l'email: {}", 
                               sessionInfo.getSessionId(), userEmail);
                    
                    sessionInfo.expireNow();
                    invalidatedCount++;
                } else {
                    logger.debug("SESSION SECURITY - Session {} déjà expirée", sessionInfo.getSessionId());
                }
            }
            
            logger.warn("SESSION SECURITY - RÉSULTAT: {} session(s) invalidée(s) pour l'email: {}", 
                       invalidatedCount, userEmail);
            
            return invalidatedCount;
            
        } catch (UsernameNotFoundException e) {
            logger.warn("SESSION SECURITY - Email {} introuvable pour invalidation sessions",
                    userEmail);
            return 0;
        } catch (Exception e) {
            // Voir invalidateAllUserSessions(User) : swallow silencieux historique
            // → maintenant log error + Sentry pour alerte ops.
            logger.error("SESSION SECURITY - ERREUR INATTENDUE invalidation sessions pour email {} : {}",
                        userEmail, e.getMessage(), e);
            if (Sentry.isEnabled()) Sentry.captureException(e);
            return 0;
        }
    }

    /**
     * Obtient le nombre de sessions actives pour un utilisateur.
     * 
     * @param userEmail L'email de l'utilisateur
     * @return Le nombre de sessions actives
     */
    public int getActiveSessionCount(String userEmail) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            List<SessionInformation> activeSessions = sessionRegistry.getAllSessions(userDetails, false);
            
            long activeCount = activeSessions.stream()
                    .filter(session -> !session.isExpired())
                    .count();
            
            logger.debug("SESSION SECURITY - Utilisateur {} a {} session(s) active(s)", 
                        userEmail, activeCount);
            
            return (int) activeCount;
            
        } catch (UsernameNotFoundException e) {
            logger.debug("SESSION SECURITY - Email {} introuvable pour count sessions", userEmail);
            return 0;
        } catch (Exception e) {
            logger.error("SESSION SECURITY - ERREUR INATTENDUE count sessions pour {} : {}",
                        userEmail, e.getMessage(), e);
            if (Sentry.isEnabled()) Sentry.captureException(e);
            return 0;
        }
    }

    /**
     * Vérifie si un utilisateur a des sessions actives.
     * 
     * @param userEmail L'email de l'utilisateur
     * @return true si l'utilisateur a des sessions actives
     */
    public boolean hasActiveSessions(String userEmail) {
        return getActiveSessionCount(userEmail) > 0;
    }
}