package com.lmp.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Active Spring Session Redis pour partager les HttpSession entre replicas via
 * le service Redis dédié {@code lmp-redis}. Latence cible : sub-milliseconde
 * (vs ~5-10 ms via Spring Session JDBC sur Postgres staging).
 *
 * <p>Migration de spring-session-jdbc → spring-session-data-redis : Redis sert
 * aussi de cache + pub/sub + queue (style stack external CRM), donc l'avoir comme
 * session store élimine 1 round-trip Postgres par requête authentifiée.</p>
 *
 * <p>La table {@code SPRING_SESSION} créée par la migration V40 reste en DB
 * mais inutilisée — laissée en place pour faciliter rollback si Redis devait
 * tomber durablement. À supprimer par migration future si confiance Redis OK.</p>
 */
@Configuration
@EnableRedisHttpSession
public class SessionConfig {
}
