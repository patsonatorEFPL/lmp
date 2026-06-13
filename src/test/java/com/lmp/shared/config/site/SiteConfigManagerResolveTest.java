package com.lmp.shared.config.site;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Couvre le fail-soft du tier base de données dans {@link SiteConfigManager#init()}
 * / resolve : une DB pas encore prête au boot ne doit PAS faire échouer le
 * démarrage — on retombe sur le tier dérivé de {@code lmp.site.url}.
 */
@ExtendWith(MockitoExtension.class)
class SiteConfigManagerResolveTest {

    private SiteConfigManager managerWith(SiteConfigRepository repo, Environment env) {
        SiteConfigManager mgr = new SiteConfigManager(repo, mock(ApplicationEventPublisher.class), env);
        ReflectionTestUtils.setField(mgr, "siteUrl", "https://lmp-services.ca");
        return mgr;
    }

    @Test
    void init_dbThrows_doesNotPropagate_andResolvesFromDerived() {
        SiteConfigRepository repo = mock(SiteConfigRepository.class);
        when(repo.findByKey(anyString())).thenThrow(new RuntimeException("Connection to localhost:1 refused"));
        Environment env = mock(Environment.class);
        when(env.getProperty(anyString())).thenReturn(null);

        SiteConfigManager mgr = managerWith(repo, env);

        // warmCache() touche le tier DB pour 10 clés ; init() ne doit pas throw.
        assertDoesNotThrow(mgr::init);

        // Le tier DB a bien été tenté (et gardé), pas court-circuité.
        verify(repo, atLeastOnce()).findByKey(anyString());

        // Clé dérivée de lmp.site.url → résolue malgré la DB en panne.
        assertEquals("https://lmp-services.ca", mgr.getString("app.base.url"));
        // Clé connue seulement en DB → null (fall-through propre), pas d'exception.
        assertNull(mgr.getString("clé.inconnue.uniquement.db"));
    }

    @Test
    void resolve_envWins_overDbAndDerived() {
        SiteConfigRepository repo = mock(SiteConfigRepository.class);
        Environment env = mock(Environment.class);
        when(env.getProperty("app.base.url")).thenReturn("https://override.example");

        SiteConfigManager mgr = managerWith(repo, env);
        mgr.init();

        assertEquals("https://override.example", mgr.getString("app.base.url"));
        // Tier env gagnant → la DB n'est jamais interrogée pour cette clé.
        verify(repo, never()).findByKey("app.base.url");
    }

    @Test
    void resolve_dbValueWins_overDerived_whenHealthy() {
        SiteConfigRepository repo = mock(SiteConfigRepository.class);
        Environment env = mock(Environment.class);
        when(env.getProperty(anyString())).thenReturn(null);
        when(repo.findByKey("app.base.url"))
                .thenReturn(Optional.of(new SiteConfigEntry("app.base.url", "https://from-db.example", null)));

        SiteConfigManager mgr = managerWith(repo, env);
        mgr.init();

        assertEquals("https://from-db.example", mgr.getString("app.base.url"));
    }
}
