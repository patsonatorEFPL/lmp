package com.lmp.catalog.web.api;

import com.lmp.shared.web.PrecompressedResponse;
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

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;

/**
 * Raw Servlet Filter HIGHEST_PRECEDENCE pour {@code GET /api/v1/services/featured}.
 *
 * <p>Bench isolated 10k VU 2026-05-17 :
 * <ul>
 *   <li>SPA shell via raw filter HIGHEST_PRECEDENCE = 0% fails, 7428 r/s</li>
 *   <li>Liveness via raw filter HIGHEST_PRECEDENCE = 0% fails, 6632 r/s</li>
 *   <li>featured via Order(0) bypass + Spring MVC = 18.77% fails, 3500 r/s</li>
 * </ul>
 *
 * <p>Filter cache hit = bypass Spring MVC + filter chain complet. Cache miss
 * = fallthrough vers {@link ServiceRestController#getFeaturedServices} qui
 * build et populate {@link FeaturedHotCache} via le même bean partagé.</p>
 *
 * <p>Country key dérivée du header {@code CF-IPCountry} (Cloudflare). Si absent
 * → "XX" (matches controller fastCountry()).</p>
 */
@Configuration
public class FeaturedFastFilter {

    private static final String FEATURED_PATH = "/api/v1/services/featured";

    @Bean
    public FilterRegistrationBean<Filter> featuredFastFilterRegistration(FeaturedHotCache cache) {
        Filter filter = (request, response, chain) -> doFilter(cache, request, response, chain);
        FilterRegistrationBean<Filter> reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        reg.addUrlPatterns(FEATURED_PATH);
        reg.setName("featuredFastFilter");
        return reg;
    }

    private static void doFilter(FeaturedHotCache cache, ServletRequest request, ServletResponse response,
                                  FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        if (!"GET".equals(req.getMethod()) && !"HEAD".equals(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        if (!FEATURED_PATH.equals(req.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String country = fastCountry(req);
        PrecompressedResponse r = cache.get(country);
        Instant now = Instant.now();
        if (r == null || now.isAfter(r.expiresAt())) {
            // Cache miss : delegate to Spring controller — it populates cache then.
            chain.doFilter(request, response);
            return;
        }

        HttpServletResponse resp = (HttpServletResponse) response;
        String ifNoneMatch = req.getHeader("If-None-Match");
        if (ifNoneMatch != null && ifNoneMatch.contains(r.etag())) {
            resp.setStatus(304);
            resp.setHeader("ETag", r.etag());
            resp.setHeader("Vary", "Accept-Encoding");
            resp.setHeader("Cache-Control", "max-age=300, public");
            return;
        }

        boolean gzip = acceptsGzip(req);
        byte[] body = gzip ? r.gzip() : r.raw();
        resp.setStatus(200);
        resp.setContentType("application/json");
        resp.setHeader("ETag", r.etag());
        resp.setHeader("Vary", "Accept-Encoding");
        resp.setHeader("Cache-Control", "max-age=300, public");
        if (gzip) {
            resp.setHeader("Content-Encoding", "gzip");
        }
        resp.setContentLength(body.length);
        if ("HEAD".equals(req.getMethod())) {
            return;
        }
        ServletOutputStream out = resp.getOutputStream();
        out.write(body);
        out.flush();
    }

    private static String fastCountry(HttpServletRequest req) {
        String cf = req.getHeader("CF-IPCountry");
        if (cf == null || cf.isBlank()) {
            return "XX";
        }
        return cf.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean acceptsGzip(HttpServletRequest req) {
        String ae = req.getHeader("Accept-Encoding");
        return ae != null && ae.contains("gzip");
    }
}
