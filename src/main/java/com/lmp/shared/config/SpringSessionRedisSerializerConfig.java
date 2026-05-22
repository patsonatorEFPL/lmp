package com.lmp.shared.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.jackson2.SecurityJackson2Modules;

/**
 * Spring Session Data Redis 4.0 + Security 7.0 ship a {@code GenericJackson2JsonRedisSerializer}
 * built on {@link SecurityJackson2Modules}, whose AllowlistTypeIdResolver rejects basic types
 * like {@code java.lang.Long} that legitimately appear in serialized session payloads
 * (e.g. {@code lastAccessedTime}). Result : sessions can't be deserialized → users see
 * stack traces in container logs and sessions get dropped.
 *
 * <p>We replace the default serializer with a permissive but still polymorphic-validated
 * Jackson mapper. The data we're (de)serializing comes from our own Redis instance which is
 * private to the prod stack (authenticated, no external access), so default typing is safe.</p>
 *
 * <p>Bean name {@code springSessionDefaultRedisSerializer} is the convention Spring Session
 * looks up via {@code RedisHttpSessionConfiguration#setDefaultRedisSerializer}.</p>
 */
@Configuration
public class SpringSessionRedisSerializerConfig {

    @Bean(name = "springSessionDefaultRedisSerializer")
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(SecurityJackson2Modules.getModules(getClass().getClassLoader()));
        mapper.registerModule(new JavaTimeModule());
        // Trust everything in OUR own Redis. Pool is private, password-protected.
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }
}
