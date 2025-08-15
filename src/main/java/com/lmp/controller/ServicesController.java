package com.lmp.controller;

import com.lmp.service.ServicesDataService;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    @Autowired
    private ServicesDataService servicesDataService;

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
        
        model.addAttribute("title", "Nos Services");
        model.addAttribute("subtitle", "Découvrez nos solutions marketing digital complètes");
        model.addAttribute("currentPage", "services");
        
        return "services";
    }
} 