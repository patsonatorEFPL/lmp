package com.lmp.notification.mail.dispatch;

import com.lmp.shared.config.site.SiteConfigEntry;
import com.lmp.shared.config.site.SiteConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Couvre le fail-soft de {@link EmailDispatcherConfigService#init()} : une DB
 * pas encore prête au boot ne doit PAS faire échouer le démarrage — on retombe
 * sur la stratégie par défaut de l'env.
 */
@ExtendWith(MockitoExtension.class)
class EmailDispatcherConfigServiceTest {

    private EmailDispatcherConfigService serviceWith(SiteConfigRepository repo, String envDefault) {
        EmailDispatcherConfigService svc = new EmailDispatcherConfigService(
                repo, mock(StringRedisTemplate.class), mock(RedisMessageListenerContainer.class));
        ReflectionTestUtils.setField(svc, "envDefaultStrategy", envDefault);
        return svc;
    }

    @Test
    void init_dbThrows_doesNotPropagate_andFallsBackToEnvDefault() {
        SiteConfigRepository repo = mock(SiteConfigRepository.class);
        when(repo.findByKey(anyString())).thenThrow(new RuntimeException("Connection to localhost:1 refused"));

        EmailDispatcherConfigService svc = serviceWith(repo, EmailDispatcherConfigService.STRATEGY_ERPNEXT);

        assertDoesNotThrow(svc::init);
        // DB en panne → on prend l'env default, pas de crash.
        assertEquals(EmailDispatcherConfigService.STRATEGY_ERPNEXT, svc.getActiveStrategy());
    }

    @Test
    void init_dbValueWins_overEnvDefault_whenHealthy() {
        SiteConfigRepository repo = mock(SiteConfigRepository.class);
        when(repo.findByKey(EmailDispatcherConfigService.DB_KEY))
                .thenReturn(Optional.of(new SiteConfigEntry(
                        EmailDispatcherConfigService.DB_KEY, EmailDispatcherConfigService.STRATEGY_ERPNEXT, null)));

        EmailDispatcherConfigService svc = serviceWith(repo, EmailDispatcherConfigService.STRATEGY_SMTP);

        svc.init();
        assertEquals(EmailDispatcherConfigService.STRATEGY_ERPNEXT, svc.getActiveStrategy());
    }
}
