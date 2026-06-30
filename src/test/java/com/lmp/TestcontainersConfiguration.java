package com.lmp;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Configuration Testcontainers pour le développement et les tests.
 * 
 * Fournit un conteneur PostgreSQL 18 éphémère qui est automatiquement :
 * - Démarré au lancement de l'application (2-3 secondes)
 * - Configuré comme datasource via @ServiceConnection
 * - Détruit à l'arrêt de l'application
 * 
 * Utilisation :
 * - Dev optionnel : {@code LMP_DEV_TESTCONTAINERS=true} + {@link com.lmp.TestLmpApplication}
 * - Tests : {@code @Import(TestcontainersConfiguration.class)}
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final String EPHEMERAL_PG_PASSWORD = "lmp_testcontainers_dev";

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:18-alpine")
                .withDatabaseName("lmp_db")
                .withUsername("lmp_dev")
                .withPassword(EPHEMERAL_PG_PASSWORD);
    }

    /**
     * Redis éphémère — sessions Spring Session, caches et pub/sub email.
     * Sans lui, les tests exigeaient un Redis local sur localhost:6379.
     */
    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    }
}
