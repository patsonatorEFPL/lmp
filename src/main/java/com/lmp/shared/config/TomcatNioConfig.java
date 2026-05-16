package com.lmp.shared.config;

import org.apache.coyote.http11.Http11NioProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tune Tomcat NIO acceptor / poller thread counts pour 4-core ARM.
 *
 * <p>Bench 6k VU 3min sustained 2026-05-16 : 8-9% fails build-up cumulatif
 * malgré 6k VU 90s burst à 0% fails. Pattern indique saturation NIO event loop
 * sur durée prolongée — defaults Tomcat = {@code pollerThreadCount=2} (capped
 * à #cores/2) + {@code acceptorThreadCount=1}.</p>
 *
 * <p>Sur 4-core ARM avec VT enabled, les request workers sont des VT
 * (cheap), mais les acceptor + poller threads gèrent les socket I/O events
 * (epoll/kqueue) et sont les fils OS critiques. Sous 6k connections, le
 * single acceptor peut devenir bottleneck d'accept rate.</p>
 *
 * <p>Tuning:</p>
 * <ul>
 *   <li>pollerThreadCount = 4 (match cores)</li>
 *   <li>acceptorThreadCount = 2</li>
 *   <li>keepAliveTimeout = 30s (default 60s → libère connections plus vite)</li>
 *   <li>maxKeepAliveRequests = 500 (default 100 → réduit handshake overhead)</li>
 * </ul>
 */
@Configuration
public class TomcatNioConfig {

    private static final Logger logger = LoggerFactory.getLogger(TomcatNioConfig.class);

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatNioCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            if (connector.getProtocolHandler() instanceof Http11NioProtocol protocol) {
                protocol.setKeepAliveTimeout(30000);
                protocol.setMaxKeepAliveRequests(500);
                connector.setProperty("pollerThreadCount", "4");
                connector.setProperty("acceptorThreadCount", "2");
                logger.info("[TomcatNIO] poller=4 acceptor=2 keepAliveTimeout=30s maxKeepAliveRequests=500");
            }
        });
    }
}
