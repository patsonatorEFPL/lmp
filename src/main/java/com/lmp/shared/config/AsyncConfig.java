package com.lmp.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Pool d'exécution borné pour {@code @Async}.
 * <p>
 * Sans configuration explicite, Spring Boot 4 instancie {@code SimpleAsyncTaskExecutor}
 * — non borné, spawn un thread par tâche. Sous load (50 VUs × 1 event par register
 * = 50 events/s) le thread count explose et la mémoire avec.
 * <p>
 * Bornage :
 * <ul>
 *   <li>core 4 — handles burst de la charge nominale (10 register/s × 1s ERP)</li>
 *   <li>max 16 — cap dur sous spike, 16 × 8s timeout ERP = 128s drain max</li>
 *   <li>queue 200 — backlog avant CallerRunsPolicy (back-pressure sur le caller)</li>
 *   <li>CallerRunsPolicy — quand pool + queue saturés, l'event run sync sur le thread Tomcat
 *       (ralentit register de l'ERP timeout au pire ; preferable to dropping events).</li>
 * </ul>
 * <p>
 * Quand virtual threads seront ré-activés (après bcrypt async), supprimer ce bean :
 * Spring Boot fournira un VirtualThreadTaskExecutor qui gère naturellement la borne
 * via le carrier ForkJoinPool.
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setThreadNamePrefix("lmp-async-");
        // Back-pressure : si pool + queue pleins, l'event s'exécute sur le thread caller
        // (Tomcat handler) plutôt que d'être perdu. Coût : latence register +ERP timeout.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) ->
                logger.warn("[ASYNC] {}#{} failed: {}",
                        method.getDeclaringClass().getSimpleName(),
                        method.getName(),
                        ex.getMessage());
    }
}
