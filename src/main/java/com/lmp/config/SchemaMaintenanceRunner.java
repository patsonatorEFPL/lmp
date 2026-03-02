package com.lmp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Correctif de schéma pour la base de données de production.
 *
 * S'exécute avant DataInitializer (@Order(0)) pour corriger les colonnes
 * legacy laissées par d'anciennes versions de l'entité.
 *
 * Toutes les opérations sont encapsulées dans un try-catch :
 * elles sont silencieusement ignorées si la colonne est déjà corrigée
 * ou si elle n'existe pas (environnement H2 dev, fresh DB, etc.).
 */
@Component
@Order(0)
public class SchemaMaintenanceRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SchemaMaintenanceRunner.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        logger.info("🔧 SchemaMaintenanceRunner — vérification du schéma...");
        fixLegacyDurationColumn();
        logger.info("✅ SchemaMaintenanceRunner — terminé.");
    }

    /**
     * La colonne 'duration' existait avant la renomination en 'duration_type'.
     * L'entité ServiceOffer ne la mappe plus, donc Hibernate n'envoie pas de valeur
     * à l'INSERT → MySQL rejette avec "Field 'duration' doesn't have a default value".
     *
     * Fix : rendre la colonne nullable (MySQL ignore alors l'absence de valeur).
     * La commande est idempotente : une seconde exécution ne cause pas d'erreur.
     */
    private void fixLegacyDurationColumn() {
        try {
            jdbcTemplate.execute(
                "ALTER TABLE service_offers MODIFY COLUMN duration VARCHAR(100) NULL DEFAULT NULL"
            );
            logger.info("✅ Schema fix: colonne legacy 'duration' rendue nullable.");
        } catch (Exception e) {
            // Cas normaux : colonne inexistante (H2/fresh DB) ou déjà nullable → ignoré
            logger.debug("Schema fix 'duration' ignoré : {}", e.getMessage());
        }
    }
}
