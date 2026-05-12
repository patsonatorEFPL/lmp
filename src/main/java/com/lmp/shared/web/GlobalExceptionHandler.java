package com.lmp.shared.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Gestionnaire global des exceptions pour l'application LMP.
 * 
 * Centralise la gestion des erreurs et fournit des réponses appropriées
 * selon le type d'exception rencontré.
 */
@ControllerAdvice
@org.springframework.core.annotation.Order(10)
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Value("${app.frontend.url:${app.base.url:http://localhost:4200}}")
    private String frontendUrl;

    /** URL de base de l'host auth — utilisée pour rediriger vers /login (canonique). */
    @Value("${app.oauth2.issuer-uri:${app.base.url:http://localhost:8080}}")
    private String authBaseUrl;

    @Value("${company.email:support@localhost}")
    private String companyEmail;

    @org.springframework.web.bind.annotation.ModelAttribute("companyEmail")
    public String globalCompanyEmail() {
        return companyEmail;
    }

    /**
     * Gère les erreurs d'accès refusé (403 Forbidden).
     * 
     * @param ex L'exception d'accès refusé
     * @param model Le modèle pour la vue
     * @return La vue d'erreur 403
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        model.addAttribute("errorTitle", "Accès refusé");
        model.addAttribute("errorMessage", "Vous n'avez pas les permissions nécessaires pour accéder à cette page.");
        model.addAttribute("errorCode", "403");
        model.addAttribute("returnUrl", frontendUrl);
        
        return "error/403";
    }

    /**
     * Laisse passer les ResponseStatusException levées intentionnellement
     * (ex. FrontendRedirectController : 404 sur l'host auth pour pages marketing,
     * ou préfixes backend sans handler) au resolver Spring built-in, qui respecte
     * le statut HTTP demandé. Sans cela, le handler RuntimeException.class
     * ci-dessous les avalerait et renverrait une vue d'erreur 500.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public void handleResponseStatus(ResponseStatusException ex) {
        throw ex;
    }

    /**
     * Gère les erreurs de ressource non trouvée.
     *
     * @param ex L'exception de ressource non trouvée
     * @param model Le modèle pour la vue
     * @return La vue d'erreur 404
     */
    @ExceptionHandler(RuntimeException.class)
    public String handleResourceNotFound(RuntimeException ex, Model model) {
        if (ex.getMessage() != null && ex.getMessage().contains("non trouvé")) {
            model.addAttribute("errorTitle", "Ressource non trouvée");
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("errorCode", "404");
            model.addAttribute("returnUrl", frontendUrl);

            return "error/404";
        }

        // Pour les autres RuntimeException, rediriger vers l'erreur 500
        return handleGeneralException(ex, model);
    }

    /**
     * Gère les erreurs d'authentification.
     * 
     * @param ex L'exception d'authentification
     * @param redirectAttributes Les attributs de redirection
     * @return Redirection vers la page de connexion avec message d'erreur
     */
    @ExceptionHandler({
        BadCredentialsException.class,
        UsernameNotFoundException.class
    })
    public String handleAuthenticationException(Exception ex, RedirectAttributes redirectAttributes) {
        String errorMessage = "Email ou mot de passe incorrect.";
        
        if (ex instanceof UsernameNotFoundException) {
            errorMessage = "Utilisateur non trouvé.";
        }
        
        redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
        return "redirect:" + authBaseUrl + "/login?error=true";
    }

    /**
     * Gère les erreurs de compte désactivé.
     *
     * @param ex L'exception de compte désactivé
     * @param redirectAttributes Les attributs de redirection
     * @return Redirection vers la page de connexion
     */
    @ExceptionHandler(DisabledException.class)
    public String handleDisabledException(DisabledException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", 
            "Votre compte est désactivé. Contactez l'administrateur.");
        return "redirect:" + authBaseUrl + "/login?error=true";
    }

    /**
     * Gère les erreurs de compte verrouillé.
     *
     * @param ex L'exception de compte verrouillé
     * @param redirectAttributes Les attributs de redirection
     * @return Redirection vers la page de connexion
     */
    @ExceptionHandler(LockedException.class)
    public String handleLockedException(LockedException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", 
            "Votre compte est temporairement verrouillé. Contactez l'administrateur.");
        return "redirect:" + authBaseUrl + "/login?error=true";
    }

    /**
     * Gère les erreurs d'argument illégal (validation).
     * 
     * @param ex L'exception d'argument illégal
     * @param model Le modèle pour la vue
     * @return La vue d'erreur 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model) {
        model.addAttribute("errorTitle", "Données invalides");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorCode", "400");
        model.addAttribute("returnUrl", frontendUrl);
        
        return "error/400";
    }

    /**
     * Gère les erreurs d'état illégal (opération non autorisée).
     * 
     * @param ex L'exception d'état illégal
     * @param model Le modèle pour la vue
     * @return La vue d'erreur 400
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalState(IllegalStateException ex, Model model) {
        model.addAttribute("errorTitle", "Opération non autorisée");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorCode", "400");
        model.addAttribute("returnUrl", frontendUrl);
        
        return "error/400";
    }

    /**
     * Static resource 404 (e.g. {@code /chunk-PKHOQFAK.js} demandé par un browser
     * sur un build précédent). Retourne 404 silencieux — pas de stack trace.
     * Volume élevé sous deploy rolling : chaque user avec onglet ouvert avant
     * deploy tape l'ancien chunk path.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleStaticResourceNotFound(NoResourceFoundException ex) {
        logger.debug("Static resource missing: {}", ex.getResourcePath());
    }

    /**
     * Gère toutes les autres exceptions non spécifiques.
     *
     * @param ex L'exception générale
     * @param model Le modèle pour la vue
     * @return La vue d'erreur 500
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneralException(Exception ex, Model model) {
        model.addAttribute("errorTitle", "Erreur interne du serveur");
        model.addAttribute("errorMessage", "Une erreur inattendue s'est produite. Veuillez réessayer plus tard.");
        model.addAttribute("errorCode", "500");
        model.addAttribute("returnUrl", frontendUrl);
        model.addAttribute("technicalDetails", ex.getMessage());

        logger.error("Erreur non gérée: {}", ex.getMessage(), ex);

        return "error/500";
    }

    /**
     * Gère les erreurs de null pointer (développement).
     * 
     * @param ex L'exception de pointeur null
     * @param model Le modèle pour la vue
     * @return La vue d'erreur 500
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleNullPointer(NullPointerException ex, Model model) {
        model.addAttribute("errorTitle", "Erreur de configuration");
        model.addAttribute("errorMessage", "Une erreur de configuration s'est produite. Contactez l'administrateur.");
        model.addAttribute("errorCode", "500");
        model.addAttribute("returnUrl", frontendUrl);
        model.addAttribute("technicalDetails", "NullPointerException: " + ex.getMessage());
        
        // Log l'erreur pour le debugging
        System.err.println("NullPointerException: " + ex.getMessage());
        ex.printStackTrace();
        
        return "error/500";
    }
}