package com.lmp.controller;

import com.lmp.service.ServicesDataService;
import com.lmp.service.user.UserService;
import com.lmp.domain.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;

/**
 * Contrôleur pour la page d'accueil du site LMP
 * 
 * Ce contrôleur gère les requêtes liées à la page principale du site.
 */
@Controller
public class HomeController {
    
    @Autowired
    private ServicesDataService servicesDataService;
    
    @Autowired
    private UserService userService;

    /**
     * Affiche la page d'accueil
     * 
     * @param model L'objet Model permet de passer des données du contrôleur vers la vue
     * @return Le nom du template Thymeleaf à utiliser (index.html)
     */
    @GetMapping("/")
    public String home(Model model) {
        // Récupérer les informations d'authentification
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName());
        
        // Informations d'authentification pour le JavaScript
        model.addAttribute("isAuthenticated", isAuthenticated);
        
        if (isAuthenticated) {
            try {
                Optional<User> userOpt = userService.findByEmail(auth.getName());
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    // Créer un objet simple pour éviter les problèmes de sérialisation
                    java.util.Map<String, Object> userInfo = new java.util.HashMap<>();
                    userInfo.put("firstName", user.getFirstName());
                    userInfo.put("lastName", user.getLastName());
                    userInfo.put("email", user.getEmail());
                    userInfo.put("phone", user.getPhone());
                    model.addAttribute("currentUser", userInfo);
                }
            } catch (Exception e) {
                // En cas d'erreur, on continue sans les données utilisateur
                System.err.println("Erreur lors de la récupération de l'utilisateur: " + e.getMessage());
                model.addAttribute("currentUser", null);
            }
        }
        
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