package com.lmp.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Pool d'exécution borné pour {@code @Async} (event bus → ERP sync).
 * <p>
 * Bean nommé {@code eventTaskExecutor} pour ne PAS shadower
 * {@code applicationTaskExecutor} créé par Spring Boot pour Spring MVC Callable
 * (configuré via {@code spring.task.execution.*} dans application-*.properties).
 * <p>
 * Bornage :
 * <ul>
 *   <li>core 4 — burst nominal (10 register/s × ~1 event/s ERP)</li>
 *   <li>max 16 — cap dur sous spike</li>
 *   <li>queue 200 — backlog avant CallerRunsPolicy (back-pressure)</li>
 * </ul>
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "eventTaskExecutor")
    public ThreadPoolTaskExecutor eventTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setThreadNamePrefix("lmp-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Executor virtual-threads pour pipelines auth offload (forgot-password) :
     * setConcurrencyLimit cape l'admission, virtualThreads=true évite tout coût
     * d'OS thread. Sous spike 750 req concurrentes, l'API répond 200 instantané
     * pendant que DB query+save+enqueue runs en arrière-plan.
     */
    @Bean(name = "authBackgroundExecutor")
    public SimpleAsyncTaskExecutor authBackgroundExecutor() {
        SimpleAsyncTaskExecutor exec = new SimpleAsyncTaskExecutor("lmp-auth-bg-");
        exec.setVirtualThreads(true);
        exec.setConcurrencyLimit(500);
        return exec;
    }

    @Override
    public Executor getAsyncExecutor() {
        return eventTaskExecutor();
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
