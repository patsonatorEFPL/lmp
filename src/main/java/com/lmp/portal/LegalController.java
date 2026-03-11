package com.lmp.portal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Contrôleur pour les pages légales
 * 
 * Ce contrôleur gère l'affichage des pages de politique de confidentialité
 * et des conditions d'utilisation de LMP Local Map Profil.
 */
@Controller
public class LegalController {

    /**
     * Affiche la page de politique de confidentialité
     * 
     * @param model L'objet Model pour passer des données à la vue
     * @return Le nom du template à utiliser
     */
    @GetMapping("/privacy")
    public String privacy(Model model) {
        model.addAttribute("title", "Politique de Confidentialité - LMP");
        model.addAttribute("currentPage", "privacy");
        return "privacy";
    }

    /**
     * Affiche la page des conditions d'utilisation
     * 
     * @param model L'objet Model pour passer des données à la vue
     * @return Le nom du template à utiliser
     */
    @GetMapping("/terms")
    public String terms(Model model) {
        model.addAttribute("title", "Conditions d'Utilisation - LMP");
        model.addAttribute("currentPage", "terms");
        return "terms";
    }
}