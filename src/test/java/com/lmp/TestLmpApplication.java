package com.lmp;

import org.springframework.boot.SpringApplication;

/**
 * Point d'entrée pour le développement local avec Testcontainers.
 * 
 * Lance l'application LMP avec un PostgreSQL 16 éphémère (Docker).
 * Aucune configuration de base de données nécessaire — tout est automatique.
 * 
 * Pour lancer :
 * - Depuis l'IDE : exécuter cette classe (clic droit → Run)
 * - Depuis Maven : mvn spring-boot:test-run
 * 
 * Prérequis : Docker doit être installé et démarré.
 */
public class TestLmpApplication {

    public static void main(String[] args) {
        SpringApplication.from(LmpApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
