package com.lmp.service.auth;

import com.lmp.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour le service DisposableEmailBlocklist.
 * Vérifie le chargement de la blocklist et la détection des emails jetables.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class DisposableEmailBlocklistTest {

    @Autowired
    private DisposableEmailBlocklist blocklist;

    @Test
    void testBlocklistIsLoaded() {
        // La blocklist locale doit avoir chargé des domaines
        assertTrue(blocklist.size() > 0, "La blocklist doit contenir des domaines");
    }

    @Test
    void testDisposableEmailDetected() {
        // mailinator.com est un domaine jetable très connu
        assertTrue(blocklist.isDisposable("test@mailinator.com"),
                "mailinator.com doit être détecté comme jetable");
    }

    @Test
    void testGuerrillaMailDetected() {
        assertTrue(blocklist.isDisposable("test@guerrillamail.com"),
                "guerrillamail.com doit être détecté comme jetable");
    }

    @Test
    void testTempMailDetected() {
        // temp-mail.org est dans la blocklist standard (tempmail.com peut varier)
        assertTrue(blocklist.isDisposable("test@yopmail.com"),
                "yopmail.com doit être détecté comme jetable");
    }

    @Test
    void testLegitimateEmailNotBlocked() {
        assertFalse(blocklist.isDisposable("user@gmail.com"),
                "gmail.com ne doit PAS être détecté comme jetable");
        assertFalse(blocklist.isDisposable("user@outlook.com"),
                "outlook.com ne doit PAS être détecté comme jetable");
        assertFalse(blocklist.isDisposable("user@yahoo.fr"),
                "yahoo.fr ne doit PAS être détecté comme jetable");
    }

    @Test
    void testNullAndInvalidEmails() {
        assertFalse(blocklist.isDisposable(null), "null ne doit pas être jetable");
        assertFalse(blocklist.isDisposable(""), "vide ne doit pas être jetable");
        assertFalse(blocklist.isDisposable("invalidemail"), "email sans @ ne doit pas être jetable");
    }

    @Test
    void testCaseInsensitive() {
        // Le domaine doit être vérifié en minuscules
        assertTrue(blocklist.isDisposable("test@MAILINATOR.COM"),
                "La détection doit être insensible à la casse");
        assertTrue(blocklist.isDisposable("test@Mailinator.Com"),
                "La détection doit être insensible à la casse (mixte)");
    }
}
