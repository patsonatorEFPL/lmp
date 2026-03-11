package com.lmp.catalog.web;

import com.lmp.catalog.service.ServicesDataService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Contrôleur pour la page des services
 *
 * Ce contrôleur gère l'affichage des services proposés par LMP.
 * Il utilise le ServicesDataService pour obtenir les données centralisées.
 */
@Controller
public class ServicesController {
    
        private final ServicesDataService servicesDataService;


    public ServicesController(ServicesDataService servicesDataService) {
        this.servicesDataService = servicesDataService;
    }

    /**
     * Affiche la page des services
     *
     * @param model L'objet Model pour passer des données à la vue
     * @return Le nom du template à utiliser
     */
    @GetMapping("/services")
    public String services(Model model) {
        // Utilisation du service centralisé pour obtenir les données
        model.addAttribute("services", servicesDataService.getAllServices());
        
        // Ajouter les informations d'authentification pour le nouveau flux
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null &&
                                 authentication.isAuthenticated() &&
                                 !"anonymousUser".equals(authentication.getName());
        
        model.addAttribute("isAuthenticated", isAuthenticated);
        if (isAuthenticated) {
            model.addAttribute("currentUser", authentication.getName());
        }
        
        model.addAttribute("title", "Nos Services");
        model.addAttribute("subtitle", "Découvrez nos solutions marketing digital complètes");
        model.addAttribute("currentPage", "services");
        
        return "services";
    }
}