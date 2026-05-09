package com.lmp.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

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
@EnableRedisHttpSession
public class SessionConfig {

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(SecurityJackson2Modules.getModules(getClass().getClassLoader()));
        return new GenericJackson2JsonRedisSerializer(mapper);
    }
}
