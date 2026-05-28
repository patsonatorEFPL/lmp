package com.lmp.shared.config;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * Force {@code autoStartup=false} sur CHAQUE {@link RedisMessageListenerContainer}
 * du contexte — le container applicatif (SSE/dispatcher) ET le
 * {@code springSessionRedisMessageListenerContainer} interne de Spring Session.
 *
 * <p>Sans ça, ces beans {@code SmartLifecycle} ouvrent une souscription Lettuce
 * synchrone pendant {@code finishRefresh}. Si {@code redis-cache} n'est pas encore
 * résolvable dans la fenêtre de boot du container (DNS overlay pas prêt), la
 * connexion échoue et le refresh du contexte est annulé → l'app entière meurt
 * puis Docker la redémarre en boucle. Le pub/sub Redis (realtime SSE, sync
 * dispatcher, expiration de session indexée) n'est PAS critique : il ne doit pas
 * pouvoir tuer auth/billing.</p>
 *
 * <p>{@link RedisListenerStarter} démarre tous ces containers après
 * {@code ApplicationReadyEvent} avec retry borné — comme un client Redis résilient
 * qui réessaie au lieu de crasher. Une indisponibilité Redis transitoire au boot
 * dégrade le realtime au lieu de tuer l'application.</p>
 */
@Component
public class RedisListenerDeferralPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof RedisMessageListenerContainer container) {
            container.setAutoStartup(false);
        }
        return bean;
    }
}
