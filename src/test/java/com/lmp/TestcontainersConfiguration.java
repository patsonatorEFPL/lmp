package com.lmp;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Configuration Testcontainers pour le développement et les tests.
 * 
 * Fournit un conteneur PostgreSQL 16 éphémère qui est automatiquement :
 * - Démarré au lancement de l'application (2-3 secondes)
 * - Configuré comme datasource via @ServiceConnection
 * - Détruit à l'arrêt de l'application
 * 
 * Utilisation :
 * - Dev : exécuter TestLmpApplication.main() depuis l'IDE ou mvn spring-boot:test-run
 * - Tests : @Import(TestcontainersConfiguration.class) ou @SpringBootTest + @Import
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("lmp_db")
                .withUsername("lmp_dev")
                .withPassword("***DB_PASSWORD_REMOVED***");
    }
}
