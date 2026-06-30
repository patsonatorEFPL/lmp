package com.lmp.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Correctif de schéma pour la base de données de production.
 *
 * S'exécute avant DataInitializer (@Order(0)) pour corriger les colonnes
 * et contraintes legacy laissées par d'anciennes versions des entités.
 *
 * Stratégie dynamique :
 *   1. Lit INFORMATION_SCHEMA pour trouver TOUTES les colonnes NOT NULL
 *      sans default sur les tables cibles, et les rend nullable si elles
 *      ne font pas partie des colonnes connues de Hibernate.
 *   2. Détecte et supprime les contraintes FK orphelines qui référencent
 *      d'anciennes tables renommées (ex. service_category → service_categories).
 *
 * Entièrement idempotent et silencieux sur H2 / fresh DB.
 */
@Component
@Order(0)
public class SchemaMaintenanceRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SchemaMaintenanceRunner.class);

        private final JdbcTemplate jdbcTemplate;

    // -------------------------------------------------------------------------
    // Colonnes mappées par Hibernate — ne jamais les toucher.
    // -------------------------------------------------------------------------

    private static final Set<String> KNOWN_SERVICES_COLUMNS = new HashSet<>(Arrays.asList(
        "id", "category_id", "title", "slug", "description",
        "icon", "display_order", "featured", "active", "created_at", "updated_at"
    ));

    private static final Set<String> KNOWN_SERVICE_OFFERS_COLUMNS = new HashSet<>(Arrays.asList(
        "id", "service_id", "name", "price", "original_price",
        "duration_type", "duration", "valid_from", "valid_to", "is_default", "active"
    ));

    // -------------------------------------------------------------------------
    // Tables courantes de Hibernate — toute FK qui référence un nom ABSENT
    // de cette liste est considérée orpheline et sera supprimée.
    // -------------------------------------------------------------------------

    private static final Set<String> CURRENT_HIBERNATE_TABLES = new HashSet<>(Arrays.asList(
        "cart", "cart_item", "invoices", "offer_benefits", "orders", "order_items",
        "order_status_history", "payment_transactions", "refunds", "reviews", "roles",
        "services", "service_benefits", "service_categories", "service_offers",
        "users", "appointments", "webhook_event_logs"
    ));


    public SchemaMaintenanceRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("SchemaMaintenanceRunner — vérification du schéma...");

        try {
            String dbName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            if (dbName == null) {
                logger.warn("Impossible de déterminer le nom de la base (H2 / non-MySQL) — correctifs ignorés.");
                return;
            }

            // Phase 1 : corriger les colonnes legacy NOT NULL sans default
            fixLegacyColumnsOnTable(dbName, "services",       KNOWN_SERVICES_COLUMNS);
            fixLegacyColumnsOnTable(dbName, "service_offers", KNOWN_SERVICE_OFFERS_COLUMNS);

            // Phase 2 : supprimer les FK orphelines (pointant vers d'anciennes tables)
            dropOrphanForeignKeys(dbName);

        } catch (Exception e) {
            // Sur H2 ou toute autre base non-MySQL, SELECT DATABASE() échoue → on ignore.
            logger.debug("SchemaMaintenanceRunner ignoré (non-MySQL ?) : {}", e.getMessage());
        }

        logger.info("SchemaMaintenanceRunner — terminé.");
    }

    // =========================================================================
    // Phase 1 : colonnes legacy NOT NULL
    // =========================================================================

    /**
     * Pour la table donnée, trouve toutes les colonnes NOT NULL sans valeur
     * par défaut qui NE SONT PAS dans les colonnes connues de Hibernate,
     * et les rend nullable (ALTER TABLE … MODIFY COLUMN … NULL DEFAULT NULL).
     */
    private void fixLegacyColumnsOnTable(String dbName, String tableName, Set<String> knownColumns) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME, COLUMN_TYPE " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = ? " +
                "  AND TABLE_NAME   = ? " +
                "  AND IS_NULLABLE  = 'NO' " +
                "  AND COLUMN_DEFAULT IS NULL " +
                "  AND COLUMN_KEY   != 'PRI'",
                dbName, tableName
            );

            for (Map<String, Object> row : rows) {
                String columnName = (String) row.get("COLUMN_NAME");
                String columnType = (String) row.get("COLUMN_TYPE");

                if (knownColumns.contains(columnName)) {
                    continue;
                }

                try {
                    jdbcTemplate.execute(
                        "ALTER TABLE `" + tableName + "` " +
                        "MODIFY COLUMN `" + columnName + "` " + columnType + " NULL DEFAULT NULL"
                    );
                    logger.info("Schema fix: {}.{} ({}) rendue nullable.", tableName, columnName, columnType);
                } catch (Exception alterEx) {
                    logger.warn("Impossible de modifier {}.{} : {}", tableName, columnName, alterEx.getMessage());
                }
            }

        } catch (Exception e) {
            logger.debug("fixLegacyColumnsOnTable({}) ignoré : {}", tableName, e.getMessage());
        }
    }

    // =========================================================================
    // Phase 2 : FK orphelines
    // =========================================================================

    /**
     * Recherche toutes les FK de la base dont la table référencée (REFERENCED_TABLE_NAME)
     * n'existe PAS dans la liste des tables Hibernate courantes. Ces FK pointent vers
     * d'anciennes tables renommées (ex. service_category → service_categories) et
     * empêchent les INSERT Hibernate de fonctionner.
     *
     * Supprime chaque contrainte orpheline trouvée.
     */
    private void dropOrphanForeignKeys(String dbName) {
        try {
            List<Map<String, Object>> fks = jdbcTemplate.queryForList(
                "SELECT CONSTRAINT_NAME, TABLE_NAME, REFERENCED_TABLE_NAME " +
                "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                "WHERE TABLE_SCHEMA = ? " +
                "  AND REFERENCED_TABLE_NAME IS NOT NULL",
                dbName
            );

            for (Map<String, Object> fk : fks) {
                String referencedTable = (String) fk.get("REFERENCED_TABLE_NAME");

                if (CURRENT_HIBERNATE_TABLES.contains(referencedTable)) {
                    // FK pointe vers une table connue → légitime, on n'y touche pas.
                    continue;
                }

                String constraintName = (String) fk.get("CONSTRAINT_NAME");
                String tableName      = (String) fk.get("TABLE_NAME");

                try {
                    jdbcTemplate.execute(
                        "ALTER TABLE `" + tableName + "` DROP FOREIGN KEY `" + constraintName + "`"
                    );
                    logger.info("Schema fix: FK orpheline '{}' sur '{}' (-> '{}') supprimée.",
                            constraintName, tableName, referencedTable);
                } catch (Exception dropEx) {
                    logger.warn("Impossible de supprimer FK '{}' sur '{}' : {}",
                            constraintName, tableName, dropEx.getMessage());
                }
            }

        } catch (Exception e) {
            logger.debug("dropOrphanForeignKeys ignoré : {}", e.getMessage());
        }
    }
}
