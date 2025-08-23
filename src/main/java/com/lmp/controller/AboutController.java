package com.lmp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Contrôleur pour la page À propos
 * 
 * Ce contrôleur gère l'affichage des informations sur l'entreprise Lmp,
 * sa mission, son équipe et ses valeurs.
 */
@Controller
public class AboutController {

    /**
     * Affiche la page À propos
     * 
     * @param model L'objet Model pour passer des données à la vue
     * @return Le nom du template à utiliser
     */
    @GetMapping("/about")
    public String about(Model model) {
        // Informations sur l'entreprise
        model.addAttribute("title", "À Propos de <span class=\"text-primary\">LMP</span>");
        model.addAttribute("subtitle", "Découvrez notre mission et notre équipe");
        
        // Mission de l'entreprise
        model.addAttribute("mission", "<span class=\"text-primary font-semibold\">LMP</span> (Local Map Profil) est une plateforme innovante " +
                "qui connecte les utilisateurs aux meilleures solutions locales. Notre mission " +
                "est de faciliter la découverte et l'accès aux services, entreprises et opportunités " +
                "qui vous entourent.");
        
        // Vision de l'entreprise
        model.addAttribute("vision", "Nous imaginons un monde où chaque personne peut facilement " +
                "trouver et accéder aux ressources locales qui répondent à ses besoins, " +
                "créant ainsi des communautés plus connectées et prospères.");
        
        // Valeurs de l'entreprise
        model.addAttribute("values", new String[]{
            "Innovation - Nous repoussons constamment les limites de la technologie",
            "Communauté - Nous croyons au pouvoir des connexions locales",
            "Transparence - Nous fournissons des informations claires et fiables",
            "Accessibilité - Nos solutions sont conçues pour tous les utilisateurs",
            "Durabilité - Nous soutenons les pratiques durables et responsables"
        });
        
        // Statistiques fictives
        model.addAttribute("stats", new Object[][]{
            {"1000+", "Entreprises partenaires"},
            {"50+", "Villes couvertes"},
            {"10,000+", "Utilisateurs satisfaits"},
            {"24/7", "Support disponible"}
        });
        
        model.addAttribute("currentPage", "about");
        
        return "about";
    }
} 