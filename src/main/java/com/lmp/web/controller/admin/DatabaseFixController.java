package com.lmp.web.controller.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Contrôleur pour appliquer des correctifs de base de données
 */
@RestController
@RequestMapping("/api/admin/database")
@PreAuthorize("hasRole('ADMIN')")
public class DatabaseFixController {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseFixController.class);

    @Autowired
    private DataSource dataSource;

    /**
     * Applique les correctifs pour permettre le hard delete
     */
    @PostMapping("/fix-hard-delete")
    public ResponseEntity<?> fixHardDeleteConstraints() {
        try {
            logger.warn("🔧 [DB-FIX] Début application des correctifs hard delete");
            
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                
                // Vérifier et modifier orders.user_id
                logger.info("🔄 [DB-FIX] Modification de orders.user_id");
                statement.executeUpdate("ALTER TABLE orders MODIFY COLUMN user_id BIGINT NULL");
                logger.info("✅ [DB-FIX] orders.user_id modifié avec succès");
                
                // Vérifier et modifier reviews.user_id
                logger.info("🔄 [DB-FIX] Modification de reviews.user_id");
                statement.executeUpdate("ALTER TABLE reviews MODIFY COLUMN user_id BIGINT NULL");
                logger.info("✅ [DB-FIX] reviews.user_id modifié avec succès");
                
                // Vérifier les changements
                var resultSet = statement.executeQuery(
                    "SELECT TABLE_NAME, COLUMN_NAME, IS_NULLABLE " +
                    "FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_NAME IN ('orders', 'reviews') " +
                    "AND COLUMN_NAME = 'user_id' " +
                    "AND TABLE_SCHEMA = DATABASE()"
                );
                
                StringBuilder result = new StringBuilder("Changements appliqués:\\n");
                while (resultSet.next()) {
                    result.append(resultSet.getString("TABLE_NAME"))
                          .append(".user_id IS_NULLABLE = ")
                          .append(resultSet.getString("IS_NULLABLE"))
                          .append("\\n");
                }
                
                logger.warn("✅ [DB-FIX] Tous les correctifs appliqués avec succès");
                
                return ResponseEntity.ok().body("{\"success\": true, \"message\": \"" + 
                    result.toString().replace("\"", "\\\"") + "\"}");
                
            }
        } catch (Exception e) {
            logger.error("❌ [DB-FIX] Erreur lors de l'application des correctifs: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}");
        }
    }
}
