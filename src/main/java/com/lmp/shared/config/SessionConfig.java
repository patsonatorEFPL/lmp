package com.lmp.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

/**
 * Active Spring Session JDBC pour partager les HttpSession entre replicas via
 * la base PostgreSQL. Sans ça, chaque container Tomcat garde ses sessions en
 * mémoire et un user qui passe d'un replica à l'autre via le LB Traefik perd
 * sa session — observé en staging multi-replica (auth/me 401 après login OK).
 *
 * <p>Schéma {@code SPRING_SESSION} + {@code SPRING_SESSION_ATTRIBUTES} créé
 * par Flyway (V40__spring_session_schema.sql), pas par Spring Session
 * (initialize-schema=never), pour rester cohérent avec les autres migrations
 * gérées par Flyway.</p>
 */
@Configuration
@EnableJdbcHttpSession
public class SessionConfig {
}
