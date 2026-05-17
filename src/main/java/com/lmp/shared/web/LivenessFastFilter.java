package com.lmp.shared.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.nio.charset.StandardCharsets;

/**
 * Sert {@code /actuator/health/liveness} via raw Servlet Filter HIGHEST_PRECEDENCE.
 *
 * <p>Bench isolated 10k VU 60s 2026-05-17 : liveness 15.84% fails même avec
 * Order(0) bypass SecurityFilterChain. Cause = pipeline Actuator complète
 * (WebEndpointDiscoverer + HealthEndpointWebExtension + Argument Resolver +
 * MessageConverter) wrap chaque request même pour livenessState in-memory.</p>
 *
 * <p>Pattern raw filter prouvé sur SPA shell ({@link SpaShellPreloadConfig}) =
 * 0% fails 7400 r/s @ 10k VU isolated. Liveness c'est juste un probe
 * "process alive" — n'importe quoi de 200 OK satisfait Docker/Traefik/K8s.</p>
 *
 * <p>Body identique à {@code HealthEndpoint.health()} avec
 * {@code management.health.livenessState.enabled=true} : {@code {"status":"UP"}}.</p>
 */
@Configuration
public class LivenessFastFilter {

    private static final String LIVENESS_PATH = "/actuator/health/liveness";
    private static final byte[] BODY = "{\"status\":\"UP\"}".getBytes(StandardCharsets.UTF_8);
    private static final String CONTENT_LENGTH = String.valueOf(BODY.length);

    @Bean
    public FilterRegistrationBean<Filter> livenessFastFilterRegistration() {
        Filter filter = (request, response, chain) -> serve(request, response, chain);
        FilterRegistrationBean<Filter> reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        reg.addUrlPatterns("/actuator/health/liveness");
        reg.setName("livenessFastFilter");
        return reg;
    }

    private static void serve(ServletRequest request, ServletResponse response, FilterChain chain)
            throws java.io.IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        if (!"GET".equals(req.getMethod()) && !"HEAD".equals(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        if (!LIVENESS_PATH.equals(req.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletResponse resp = (HttpServletResponse) response;
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.setHeader("Content-Length", CONTENT_LENGTH);
        resp.setHeader("Cache-Control", "no-cache, no-store");
        if ("HEAD".equals(req.getMethod())) {
            return;
        }
        ServletOutputStream out = resp.getOutputStream();
        out.write(BODY);
        out.flush();
    }
}
