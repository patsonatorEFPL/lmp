package com.lmp.shared.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * Force Caffeine local cache pour @Cacheable annotations.
 *
 * <p>Sans ce bean, Spring Boot 4 autoconfig détecte spring-boot-starter-data-redis
 * + @EnableCaching et instancie un {@link org.springframework.data.redis.cache.RedisCacheManager}
 * avec serializer Jdk (default). Les caches retournent des entities JPA non-Serializable
 * ({@code com.lmp.catalog.domain.Service}, etc.) → {@code NotSerializableException}
 * sur chaque @Cacheable miss.</p>
 *
 * <p>Caffeine local est plus rapide (sub-microsecond vs Redis 500μs) et ne demande
 * pas Serializable. Pour LMP volume actuel les caches sont read-heavy idempotent
 * (services catalog, FX rates, geo IP) → staleness inter-replica acceptable
 * (TTL court). Si besoin coherence cross-replica → Spring Cache + Redis namespace
 * + JSON serializer (pas le default JDK).</p>
 *
 * <p>{@link Primary} pour gagner contre l'autoconfig RedisCacheManager.</p>
 */
@Configuration
public class CacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager();
        mgr.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats());
        return mgr;
    }
}
