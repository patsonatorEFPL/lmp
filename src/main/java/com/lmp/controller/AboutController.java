package com.lmp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Locale;

/**
 * Contrôleur pour la page À propos
 *
 * Ce contrôleur gère l'affichage des informations sur l'entreprise Lmp,
 * sa mission, son équipe et ses valeurs.
 */
@Controller
public class AboutController {

    @Autowired
    private MessageSource messageSource;

    /**
     * Affiche la page À propos
     *
     * @param model L'objet Model pour passer des données à la vue
     * @return Le nom du template à utiliser
     */
    @GetMapping("/about")
    public String about(Model model) {
        Locale locale = LocaleContextHolder.getLocale();
        
        // Informations sur l'entreprise (internationalisées)
        String titleKey = messageSource.getMessage("about.page.header.title", null, locale);
        model.addAttribute("title", titleKey);
        model.addAttribute("subtitle", messageSource.getMessage("about.page.header.subtitle", null, locale));
        
        // Mission de l'entreprise (internationalisée)
        model.addAttribute("mission", messageSource.getMessage("about.page.mission.content", null, locale));
        
        // Vision de l'entreprise (internationalisée)
        model.addAttribute("vision", messageSource.getMessage("about.page.vision.content", null, locale));
        
        model.addAttribute("currentPage", "about");
        
        return "about";
    }
}
