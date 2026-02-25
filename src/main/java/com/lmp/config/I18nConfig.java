package com.lmp.config;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * Configuration pour l'internationalisation (i18n) de l'application LMP.
 * 
 * Cette configuration permet :
 * - La gestion des messages multilingues (français/anglais)
 * - La résolution de la locale via cookies
 * - Le changement de langue via paramètre URL
 */
@Configuration
public class I18nConfig implements WebMvcConfigurer {

    /**
     * Configuration du MessageSource pour charger les fichiers de traduction.
     * 
     * @return MessageSource configuré pour les fichiers i18n/messages_*.properties
     */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        
        // Définir le nom de base des fichiers de messages (i18n/messages_*.properties)
        messageSource.setBasenames("i18n/messages", "messages");
        
        // Encodage UTF-8 pour supporter les caractères spéciaux
        messageSource.setDefaultEncoding("UTF-8");
        
        // Ne pas utiliser la locale système comme fallback
        messageSource.setFallbackToSystemLocale(false);
        
        // Utiliser le français comme langue par défaut
        messageSource.setDefaultLocale(Locale.FRENCH);
        
        // Cache des messages pour de meilleures performances
        messageSource.setCacheSeconds(3600);
        
        return messageSource;
    }

    /**
     * Résolveur de locale basé sur les cookies.
     * Permet de persister la préférence linguistique de l'utilisateur.
     * 
     * @return LocaleResolver configuré pour utiliser les cookies
     */
    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver();
        
        // Locale par défaut (français pour LMP - entreprise québécoise)
        resolver.setDefaultLocale(Locale.FRENCH);
        
        // Nom du cookie pour stocker la langue
        resolver.setCookieName("lmp_locale");
        
        // Durée de vie du cookie (90 jours)
        resolver.setCookieMaxAge(90 * 24 * 60 * 60);
        
        // Cookie accessible uniquement via HTTP (sécurité)
        resolver.setCookieHttpOnly(true);
        
        // Chemin du cookie (toute l'application)
        resolver.setCookiePath("/");
        
        return resolver;
    }

    /**
     * Intercepteur pour détecter les changements de langue via paramètre URL.
     * 
     * @return LocaleChangeInterceptor configuré pour le paramètre 'lang'
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        
        // Nom du paramètre URL pour changer la langue (ex: ?lang=en)
        interceptor.setParamName("lang");
        
        // Ignorer les valeurs de langue invalides
        interceptor.setIgnoreInvalidLocale(true);
        
        return interceptor;
    }

    /**
     * Enregistrement de l'intercepteur de changement de locale.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
