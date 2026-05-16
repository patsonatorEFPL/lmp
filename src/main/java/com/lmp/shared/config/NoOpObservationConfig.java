package com.lmp.shared.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Override Spring Boot {@code ObservationAutoConfiguration} avec {@code NOOP}.
 *
 * <p>async-profiler v4.4 5k VU 3min bench 2026-05-16 : Micrometer/Observation
 * = 55.5% CPU samples (11616/20936) même avec {@code MANAGEMENT_OBSERVATIONS_ENABLE_ALL=false}.
 * L'env var supprime la création de meters mais
 * {@code ObservationFilterChainDecorator} et les autres wraps Tomcat/MVC
 * continuent d'appeler {@code Observation.start()/stop()} → instances NoOp
 * créées + dispatch through interface → overhead persistant.</p>
 *
 * <p>En forçant {@code ObservationRegistry.NOOP} comme bean {@code @Primary},
 * tous les sites Spring qui injectent {@code ObservationRegistry} (Spring Security
 * filter chain, Spring MVC, RestClient, RestTemplate, JDBC, Reactor) reçoivent
 * cette implémentation singleton qui retourne {@code NoopObservation} pour tous
 * les chemins. {@code NoopObservation.start()} est literally {@code return this}
 * → near-zero overhead.</p>
 *
 * <p><b>Trade-off:</b> perte TOTALE d'observability runtime — pas de spans
 * tracing, pas de metrics derived from observations, pas de @Observed annotations.
 * Acceptable car LMP n'utilise pas tracing distributé et les metrics critiques
 * (Tomcat, JVM, JDBC, Redis) sont collectées via Micrometer Counters/Gauges
 * indépendants du système d'observation.</p>
 */
@Configuration
public class NoOpObservationConfig {

    @Bean
    @Primary
    public ObservationRegistry observationRegistry() {
        return ObservationRegistry.NOOP;
    }
}
