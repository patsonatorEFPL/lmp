package com.lmp.shared.web.debug;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

@RestController
public class DatabaseTestController {

        private final DataSource dataSource;


    public DatabaseTestController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/debug/database-test")
    public ResponseEntity<Map<String, Object>> testDatabase() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            System.out.println("=== DATABASE CONNECTION TEST START ===");
            
            // Test 1: Obtenir une connexion
            try (Connection connection = dataSource.getConnection()) {
                result.put("connection_status", "SUCCESS");
                result.put("connection_url", connection.getMetaData().getURL());
                result.put("database_product", connection.getMetaData().getDatabaseProductName());
                result.put("database_version", connection.getMetaData().getDatabaseProductVersion());
                result.put("username", connection.getMetaData().getUserName());
                
                System.out.println("✅ Connexion établie avec succès");
                System.out.println("📍 URL: " + connection.getMetaData().getURL());
                System.out.println("🏷️ Produit: " + connection.getMetaData().getDatabaseProductName());
                System.out.println("📦 Version: " + connection.getMetaData().getDatabaseProductVersion());
                System.out.println("👤 Utilisateur: " + connection.getMetaData().getUserName());
                
                // Test 2: Lister les bases de données disponibles
                try (Statement stmt = connection.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SHOW DATABASES");
                    StringBuilder databases = new StringBuilder();
                    while (rs.next()) {
                        databases.append(rs.getString(1)).append(", ");
                    }
                    result.put("available_databases", databases.toString());
                    System.out.println("📚 Bases de données disponibles: " + databases.toString());
                } catch (Exception e) {
                    result.put("databases_error", e.getMessage());
                    System.err.println("❌ Erreur lors de la liste des BD: " + e.getMessage());
                }
                
                // Test 3: Tenter de créer la base de données 'lmp' si elle n'existe pas
                try (Statement stmt = connection.createStatement()) {
                    stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS lmp");
                    result.put("database_creation", "SUCCESS - Database 'lmp' created or already exists");
                    System.out.println("✅ Base de données 'lmp' créée ou existe déjà");
                } catch (Exception e) {
                    result.put("database_creation_error", e.getMessage());
                    System.err.println("❌ Erreur création BD 'lmp': " + e.getMessage());
                }
                
                // Test 4: Se connecter spécifiquement à la base 'lmp'
                try (Statement stmt = connection.createStatement()) {
                    stmt.executeUpdate("USE lmp");
                    result.put("use_lmp_database", "SUCCESS");
                    System.out.println("✅ Connexion à la base 'lmp' réussie");
                } catch (Exception e) {
                    result.put("use_lmp_error", e.getMessage());
                    System.err.println("❌ Erreur USE lmp: " + e.getMessage());
                }
                
            } catch (Exception e) {
                result.put("connection_status", "FAILED");
                result.put("connection_error", e.getMessage());
                System.err.println("❌ Échec de connexion: " + e.getMessage());
                e.printStackTrace();
            }
            
        } catch (Exception e) {
            result.put("fatal_error", e.getMessage());
            System.err.println("💥 Erreur fatale: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("=== DATABASE CONNECTION TEST END ===");
        return ResponseEntity.ok(result);
    }
}