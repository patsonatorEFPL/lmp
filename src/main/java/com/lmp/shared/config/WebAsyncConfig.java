package com.lmp.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Pool dédié au dispatch Spring MVC async ({@code Callable<ResponseEntity>}).
 * <p>
 * Sans cette config, Spring MVC default = {@code SimpleAsyncTaskExecutor} non borné.
 * Sous burst register, des centaines de threads spawnent (vu {@code MvcAsync205}
 * dans logs). Bornage pour bcrypt CPU-bound sur 4 OCPU ARM A1 :
 * <ul>
 *   <li>core 8 / max 16 — bcrypt 10 ≈ 100 ms ARM. Avec 4 vCPU et 16 threads max,
 *       ~4 bcrypts en parallèle pleins (le reste attend), throughput ~40 reg/s/replica.
 *       Bump 2× depuis 4/8 (5/9): bench register-only depuis VM montrait 5 reg/s saturé,
 *       diagnose mvcExecutor pool size — bump permet plus de concurrence pendant que
 *       virtual threads carrier (bcrypt) tournent.</li>
 *   <li>queue 64 — backlog absorbe burst, wait worst-case ≈ 64 × 100 ms / 4 cores
 *       = ~1.6s. Évite pile-up forçant timeout 30s côté client.</li>
 *   <li>AbortPolicy — quand pool + queue saturés, 503 immédiat → client retry/back-off
 *       plus utile qu'une requête bloquée 30s qui timeout.</li>
 * </ul>
 * <p>
 * Bean nommé {@code mvcTaskExecutor} (NE shadow PAS {@code applicationTaskExecutor}
 * de Spring Boot autoconfig, utilisé par {@code @Async}).
 */
@Configuration
public class WebAsyncConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebAsyncConfig.class);

    @Bean(name = "mvcTaskExecutor")
    public ThreadPoolTaskExecutor mvcTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Pool 16/32 + queue 128 absorbe ~160 concurrent register sur 4 OCPU
        // (4 bcrypts en parallèle pleins, reste en attente queue).
        executor.setCorePoolSize(16);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(128);
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setThreadNamePrefix("mvc-async-");
        // CallerRunsPolicy : sous saturation, Tomcat caller thread exécute la
        // tâche directement → back-pressure naturelle (slow client) au lieu de
        // RejectedExecutionException → 503 → healthcheck KO. Évite faux unhealthy
        // sous burst load (observé 5 replicas où liveness via Callable saturé).
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(mvcTaskExecutor());
        // 30s timeout sur Callable / DeferredResult — au-delà = 503 au client.
        // Couvre largement bcrypt 10 (~100 ms) + INSERT user + email send (~500 ms total).
        configurer.setDefaultTimeout(30_000L);
        logger.info("[MVC ASYNC] Bound to mvcTaskExecutor (core=8 max=16 queue=64), timeout=30s");
    }
}
