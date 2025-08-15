package com.lmp.controller;

import com.lmp.service.ServicesDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Contrôleur pour la page d'accueil du site LMP
 * 
 * Ce contrôleur gère les requêtes liées à la page principale du site.
 */
@Controller
public class HomeController {
    
    @Autowired
    private ServicesDataService servicesDataService;

    /**
     * Affiche la page d'accueil
     * 
     * @param model L'objet Model permet de passer des données du contrôleur vers la vue
     * @return Le nom du template Thymeleaf à utiliser (index.html)
     */
    @GetMapping("/")
    public String home(Model model) {
        // Ajout de données au modèle qui seront disponibles dans la vue
        model.addAttribute("title", "LMP - Marketing Digital Local");
        model.addAttribute("subtitle", "Dominez votre marché local avec nos solutions digitales");
        model.addAttribute("description", "LMP vous accompagne pour optimiser votre présence digitale locale. " +
                "Référencement, gestion d'avis, marketing digital - nous avons les solutions pour votre croissance.");
        model.addAttribute("currentPage", "home");
        
        // Ajout des services principaux pour la section services de la page d'accueil
        model.addAttribute("featuredServices", servicesDataService.getFeaturedServices());
        model.addAttribute("totalServicesCount", servicesDataService.getAllServices().size());
        
        return "index";
    }
} 