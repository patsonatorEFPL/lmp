package com.lmp.shared.web;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Enregistre {@link OidcHostGuardFilter} avec la priorité maximale pour
 * qu'il s'exécute AVANT Spring Security (sinon Spring Authorization Server
 * répondrait sur le mauvais host avant la garde).
 */
@Configuration
public class OidcHostGuardFilterConfig {

    @Bean
    public FilterRegistrationBean<OidcHostGuardFilter> oidcHostGuardFilter(AuthHostResolver authHostResolver) {
        FilterRegistrationBean<OidcHostGuardFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new OidcHostGuardFilter(authHostResolver));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
