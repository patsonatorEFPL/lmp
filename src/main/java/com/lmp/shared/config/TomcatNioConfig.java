package com.lmp.shared.config;

import org.apache.coyote.http11.Http11NioProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tomcat NIO poller threads tuning pour 4-core ARM.
 *
 * <p>Bench 10k VU mix réaliste iter20b : 32% fails, CPU 250%/400% = 62% utilisé.
 * 38% capacity idle pendant que k6 timeout = bottleneck NIO event loop.
 *
 * <p>Defaults Tomcat 4-core ARM :</p>
 * <ul>
 *   <li>{@code pollerThreadCount = Math.min(2, available cores / 2)} = 2</li>
 *   <li>{@code acceptorThreadCount = 1}</li>
 * </ul>
 *
 * <p>Sous 10k connections concurrentes, 2 poller threads dispatch socket
 * events insuffisant. Bump à 4 pour match cores. Acceptor garde 1 (rarement
 * bottleneck si max-connections élevé).</p>
 *
 * <p><b>NE PAS toucher</b> {@code keepAliveTimeout} (iter5 reverted bumped 60→30s
 * → re-handshake overhead worse). Garde defaults.</p>
 */
@Configuration
public class TomcatNioConfig {

    private static final Logger logger = LoggerFactory.getLogger(TomcatNioConfig.class);

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatNioCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            if (connector.getProtocolHandler() instanceof Http11NioProtocol) {
                connector.setProperty("pollerThreadCount", "4");
                logger.info("[TomcatNIO] pollerThreadCount=4 (defaults : acceptor=1, keepAliveTimeout=60s)");
            }
        });
    }
}
