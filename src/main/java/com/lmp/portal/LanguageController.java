package com.lmp.portal;

import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Contrôleur pour la gestion du changement de langue.
 * 
 * Permet aux utilisateurs de changer la langue de l'interface
 * en utilisant un paramètre URL (ex: ?lang=en).
 */
@Controller
public class LanguageController {

        private final LocaleResolver localeResolver;


    public LanguageController(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    /**
     * Change la langue de l'application et redirige vers la page précédente.
     * 
     * @param lang La langue à définir (ex: 'fr', 'en')
     * @param request La requête HTTP
     * @param response La réponse HTTP
     * @return Redirection vers la page précédente ou l'accueil si non définie
     */
    @GetMapping("/lang")
    public String changeLanguage(@RequestParam("lang") String lang,
                               HttpServletRequest request, 
                               HttpServletResponse response) {
        
        try {
            // Créer la locale à partir du paramètre langue
            Locale locale = Locale.forLanguageTag(lang);
            
            // Valider la langue (seulement fr et en supportées)
            if (!isLanguageSupported(lang)) {
                // Si langue non supportée, utiliser le français par défaut
                locale = Locale.FRENCH;
            }
            
            // Définir la nouvelle locale
            localeResolver.setLocale(request, response, locale);
            
            // Log pour debug
            System.out.println("✓ Langue changée vers: " + locale.getLanguage());
            
        } catch (Exception e) {
            // En cas d'erreur, utiliser le français par défaut
            localeResolver.setLocale(request, response, Locale.FRENCH);
            System.err.println("✗ Erreur lors du changement de langue: " + e.getMessage());
        }
        
        // Récupérer l'URL de référence pour rediriger vers la page précédente
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            // Supprimer le paramètre lang existant de l'URL de référence
            referer = removeLanguageParam(referer);
            return "redirect:" + referer;
        }
        
        // Si pas de référence, rediriger vers l'accueil
        return "redirect:/";
    }

    /**
     * Vérifie si la langue est supportée par l'application.
     * 
     * @param lang Le code de langue à vérifier
     * @return true si la langue est supportée, false sinon
     */
    private boolean isLanguageSupported(String lang) {
        return "fr".equals(lang) || "en".equals(lang);
    }
    
    /**
     * Supprime le paramètre lang de l'URL pour éviter la duplication.
     * 
     * @param url L'URL à nettoyer
     * @return L'URL sans le paramètre lang
     */
    private String removeLanguageParam(String url) {
        if (url == null) return url;
        
        // Supprimer ?lang=xx ou &lang=xx
        url = url.replaceAll("[?&]lang=[^&]*", "");
        
        // Corriger si on a supprimé le ? en début de paramètres
        if (url.contains("&") && !url.contains("?")) {
            url = url.replaceFirst("&", "?");
        }
        
        return url;
    }
}
