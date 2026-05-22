package com.lmp.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;

/**
 * Active Spring Session Redis pour partager les HttpSession entre replicas via
 * le service Redis dédié {@code lmp-redis-cache}. Latence cible : sub-milliseconde.
 *
 * <p><b>Serializer JSON (vs JDK default)</b> — découvert sous bench : le default
 * {@link org.springframework.data.redis.serializer.JdkSerializationRedisSerializer}
 * exige que tout objet stocké en session implémente {@link java.io.Serializable}.
 * Les entities JPA ({@code com.lmp.catalog.domain.Service} etc.) ne le font pas
 * → {@code NotSerializableException} sur chaque request authentifiée qui touche
 * un service en session. Fix : {@link GenericJackson2JsonRedisSerializer} qui
 * sérialise via Jackson (POJO standard, pas de marker interface requise).
 *
 * <p>Le mapper Jackson inclut {@link SecurityJackson2Modules} pour gérer correctement
 * les types Spring Security (Authentication, GrantedAuthority...) qui ont besoin
 * de @JsonTypeInfo pour round-trip sécurisé.</p>
 *
 * <p>Migration de spring-session-jdbc → spring-session-data-redis. La table
 * {@code SPRING_SESSION} créée par V40 reste en DB mais inutilisée.</p>
 */
@Configuration
@EnableRedisIndexedHttpSession
public class SessionConfig {

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(SecurityJackson2Modules.getModules(getClass().getClassLoader()));
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        // Spring Session 4.0 + Security 7.0 : the AllowlistTypeIdResolver shipped by
        // SecurityJackson2Modules rejects java.lang.Long, which appears legitimately in
        // session payloads (e.g. lastAccessedTime). Activate default polymorphic typing
        // with a permissive validator — safe because our Redis is private (password-auth,
        // not internet-exposed) and we trust the data we write ourselves.
        mapper.activateDefaultTyping(
                com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    /**
     * Spring Session essaie de configurer Redis {@code notify-keyspace-events Egex}
     * pour la session expiration (RedisIndexedSessionRepository). Avec
     * {@code requirepass} actif sur lmp-redis-cache, le client Spring Session
     * connecté sans permission CONFIG voit la commande échouer →
     * {@code NOAUTH Authentication required} ou {@code unknown command}.
     *
     * <p>Désactive l'auto-config via {@link ConfigureRedisAction#NO_OP}. La
     * notification keyspace est configurée côté redis-cache directement
     * (compose command : {@code --notify-keyspace-events Egex}).</p>
     */
    @Bean
    public ConfigureRedisAction configureRedisAction() {
        return ConfigureRedisAction.NO_OP;
    }
}
