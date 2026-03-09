package com.lmp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration OpenAPI / Swagger pour l'API LMP.
 * 
 * Génère automatiquement la documentation API accessible via /swagger-ui.html.
 * Le spec OpenAPI JSON est disponible via /v3/api-docs.
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

    @Bean
    public OpenAPI lmpOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LMP Digital Services — API")
                        .version("1.0")
                        .description("""
                                API REST du SaaS LMP Digital Services.
                                
                                ## Authentification
                                L'API utilise des sessions HTTP (cookies HttpOnly).
                                - POST `/api/v1/auth/login` pour obtenir une session
                                - Le cookie `JSESSIONID` est envoyé automatiquement
                                - Header `X-XSRF-TOKEN` requis pour les requêtes mutatives (POST, PUT, DELETE)
                                
                                ## Versioning
                                Tous les endpoints sont préfixés par `/api/v1/`.
                                """)
                        .contact(new Contact()
                                .name("LMP Digital Services")
                                .email("support@lmp-services.be")
                                .url("https://lmp-services.be"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://lmp-services.be/terms")))
                .servers(List.of(
                        new Server().url(baseUrl).description("Current environment")));
    }
}
