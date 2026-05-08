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
 * dans logs). Bornage strict pour bcrypt CPU-bound :
 * <ul>
 *   <li>core 4 / max 8 — bcrypt 10 (~150 ms ARM × 8 threads / 2 vCPU = ~25 reg/s plafond CPU.
 *       Plus de threads = thrashing, pas de gain throughput sur CPU pur.</li>
 *   <li>queue 256 — backlog absorbe burst de register</li>
 *   <li>CallerRunsPolicy — quand pool + queue saturés, exécute sync sur thread caller
 *       (Tomcat handler) plutôt que rejeter. Back-pressure naturelle.</li>
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
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(256);
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setThreadNamePrefix("mvc-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(mvcTaskExecutor());
        // 30s timeout sur Callable / DeferredResult — au-delà = 503 au client.
        // Couvre largement bcrypt 10 (~150 ms) + INSERT user + email send (~500 ms total).
        configurer.setDefaultTimeout(30_000L);
        logger.info("[MVC ASYNC] Bound to mvcTaskExecutor (core=4 max=8 queue=256), timeout=30s");
    }
}
