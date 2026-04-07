package com.lmp;

import org.springframework.boot.SpringApplication;

/**
 * Point d'entrée de développement (classpath test).
 * <p>
 * Par défaut : même comportement que {@link LmpApplication} — connexion à PostgreSQL
 * local persistant ({@code docker-compose.dev.yml}).
 * <p>
 * PostgreSQL éphémère (Testcontainers) uniquement si la variable d'environnement
 * {@code LMP_DEV_TESTCONTAINERS=true} (ou propriété système {@code -Dlmp.dev.testcontainers=true}).
 */
public class TestLmpApplication {

    public static void main(String[] args) {
        if (useTestcontainers()) {
            SpringApplication.from(LmpApplication::main)
                    .with(TestcontainersConfiguration.class)
                    .run(args);
        } else {
            LmpApplication.main(args);
        }
    }

    private static boolean useTestcontainers() {
        String env = System.getenv("LMP_DEV_TESTCONTAINERS");
        if (env != null && !env.isBlank()) {
            return Boolean.parseBoolean(env);
        }
        return Boolean.parseBoolean(System.getProperty("lmp.dev.testcontainers", "false"));
    }
}
