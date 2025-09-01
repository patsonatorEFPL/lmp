package com.lmp.web.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration Web pour l'application LMP
 * Inclut les intercepteurs SEO pour optimiser l'indexation
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Autowired
    private SeoInterceptor seoInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // SEO Interceptor DÉSACTIVÉ temporairement pour debugging
        // registry.addInterceptor(seoInterceptor)
        //         .addPathPatterns("/**")
        //         .excludePathPatterns("/css/**", "/js/**", "/images/**", "/favicon.ico",
        //                            "/api/**", "/admin/**", "/error/**");
    }
}