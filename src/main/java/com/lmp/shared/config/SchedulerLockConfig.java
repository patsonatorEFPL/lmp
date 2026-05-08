package com.lmp.shared.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * ShedLock — coordonne les {@code @Scheduled} critiques entre N répliques swarm.
 * <p>
 * Sans cette config, en multi-replica chaque scheduled tourne SUR CHAQUE instance
 * → emails dupliqués (reminders, welcome), purge DB exécutée plusieurs fois,
 * external API refresh facturée 2× par run, etc.
 * <p>
 * Avec ShedLock, la première instance qui hit le scheduled gagne le lock dans
 * la table {@code shedlock} pour la durée {@code lockAtMostFor}. Les autres
 * répliques skip silencieusement leur tour.
 * <p>
 * {@code defaultLockAtMostFor = "PT30M"} : si une réplique crash en plein milieu
 * d'un scheduled, le lock est libéré après 30 min max (autres répliques peuvent
 * reprendre). À ajuster individuellement avec {@code @SchedulerLock(lockAtMostFor=...)}
 * pour les jobs courts (ex: purge quotidienne = 5 min).
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class SchedulerLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime() // Utilise NOW() côté PostgreSQL — pas de skew d'horloge entre répliques
                        .build()
        );
    }
}
