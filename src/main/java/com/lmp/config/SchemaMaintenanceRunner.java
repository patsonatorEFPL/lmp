package com.lmp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
 * legacy laissées par d'anciennes versions des entités.
 *
 * Stratégie dynamique : lit INFORMATION_SCHEMA pour trouver TOUTES les
 * colonnes NOT NULL sans default sur les tables cibles, et les rend
 * nullable si elles ne font pas partie des colonnes connues de Hibernate.
 *
 * Entièrement idempotent et silencieux sur H2 / fresh DB.
 */
@Component
@Order(0)
public class SchemaMaintenanceRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SchemaMaintenanceRunner.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Colonnes mappées par Hibernate pour chaque table — ne jamais les toucher.
    private static final Set<String> KNOWN_SERVICES_COLUMNS = new HashSet<>(Arrays.asList(
        "id", "category_id", "title", "slug", "description",
        "icon", "display_order", "featured", "active", "created_at", "updated_at"
    ));

    private static final Set<String> KNOWN_SERVICE_OFFERS_COLUMNS = new HashSet<>(Arrays.asList(
        "id", "service_id", "name", "price", "original_price",
        "duration_type", "valid_from", "valid_to", "is_default", "active"
    ));

    @Override
    public void run(String... args) throws Exception {
        logger.info("🔧 SchemaMaintenanceRunner — vérification du schéma...");

        try {
            String dbName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            if (dbName == null) {
                logger.warn("⚠️  Impossible de déterminer le nom de la base (H2 / non-MySQL) — correctifs ignorés.");
                return;
            }
            fixLegacyColumnsOnTable(dbName, "services",       KNOWN_SERVICES_COLUMNS);
            fixLegacyColumnsOnTable(dbName, "service_offers", KNOWN_SERVICE_OFFERS_COLUMNS);
        } catch (Exception e) {
            // Sur H2 ou toute autre base non-MySQL, SELECT DATABASE() échoue → on ignore.
            logger.debug("SchemaMaintenanceRunner ignoré (non-MySQL ?) : {}", e.getMessage());
        }

        logger.info("✅ SchemaMaintenanceRunner — terminé.");
    }

    /**
     * Pour la table donnée, trouve toutes les colonnes NOT NULL sans valeur
     * par défaut qui NE SONT PAS dans les colonnes connues de Hibernate,
     * et les rend nullable (ALTER TABLE … MODIFY COLUMN … NULL DEFAULT NULL).
     *
     * Utilise COLUMN_TYPE tel que retourné par INFORMATION_SCHEMA pour
     * conserver le type exact de la colonne (VARCHAR, DECIMAL, etc.).
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
                    // Colonne mappée par Hibernate — pas de modification.
                    continue;
                }

                try {
                    jdbcTemplate.execute(
                        "ALTER TABLE `" + tableName + "` " +
                        "MODIFY COLUMN `" + columnName + "` " + columnType + " NULL DEFAULT NULL"
                    );
                    logger.info("✅ Schema fix: {}.{} ({}) rendue nullable.", tableName, columnName, columnType);
                } catch (Exception alterEx) {
                    logger.warn("⚠️  Impossible de modifier {}.{} : {}", tableName, columnName, alterEx.getMessage());
                }
            }

        } catch (Exception e) {
            logger.debug("fixLegacyColumnsOnTable({}) ignoré : {}", tableName, e.getMessage());
        }
    }
}
