package com.lmp;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Test de vérification de l'architecture Spring Modulith.
 * Vérifie que les modules respectent les règles de dépendances.
 */
class ModulithArchitectureTest {

    @Test
    void verifyModularStructure() {
        var modules = ApplicationModules.of(LmpApplication.class);
        modules.forEach(System.out::println);
        modules.verify();
    }
}
