package com.lmp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Arrays;
import java.util.List;

/**
 * Contrôleur pour la carte interactive
 * 
 * Ce contrôleur gère l'affichage de la carte interactive avec les localisations
 * et profils locaux. Il fournit les données nécessaires pour l'affichage des marqueurs.
 */
@Controller
public class MapController {

    /**
     * Affiche la page de la carte interactive
     * 
     * @param model L'objet Model pour passer des données à la vue
     * @return Le nom du template à utiliser
     */
    @GetMapping("/map")
    public String map(Model model) {
        // Localisation principale LMP uniquement
        List<Location> locations = Arrays.asList(
            new Location("LMP - Siège Social", "Rue Gatti De Gamond 97, 1180 Uccle", 50.8012, 4.3447, "university")
        );
        
        model.addAttribute("title", "Carte Interactive");
        model.addAttribute("subtitle", "Découvrez les solutions locales sur notre carte interactive");
        model.addAttribute("locations", locations);
        model.addAttribute("currentPage", "map");
        
        return "map";
    }
    
    /**
     * Classe interne pour représenter une localisation
     * Cette classe contient les informations d'un point d'intérêt sur la carte
     */
    public static class Location {
        private String name;
        private String description;
        private double latitude;
        private double longitude;
        private String category;
        
        public Location(String name, String description, double latitude, double longitude, String category) {
            this.name = name;
            this.description = description;
            this.latitude = latitude;
            this.longitude = longitude;
            this.category = category;
        }
        
        // Getters et Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }
} 
