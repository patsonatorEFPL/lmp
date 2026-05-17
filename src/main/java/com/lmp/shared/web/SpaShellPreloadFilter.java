package com.lmp.shared.web;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

/**
 * Pre-charge {@code /index.html} (Angular SPA shell) en byte[] au boot et
 * sert direct sur GET {@code /} via Filter HIGHEST_PRECEDENCE.
 *
 * <p>Pattern référencé : Pinterest static asset serving (pre-compressed at
 * boot), Cloudflare Workers static serving (sub-50µs per request). Le
 * fichier {@code index.html} est statique (généré au build Angular) — pas
 * de logique runtime requise. Bypass tout : Spring Security FilterChain,
 * DispatcherServlet, HandlerMapping, FrontendRedirectController.</p>
 *
 * <p>Variante : aussi pré-compresse en gzip + sert Content-Encoding: gzip
 * si client supporte (Accept-Encoding: gzip). Réduit bandwidth ~75% sur
 * HTML+JS (texte hautement compressible).</p>
 *
 * <p>10% du mix bench prod-like cible `/`. Sous load 5k VU, élimine ~500
 * concurrent requests de la chaîne @Order(2) backend (15+ filters).</p>
 */
@Configuration
public class SpaShellPreloadFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(SpaShellPreloadFilter.class);
    private static final String[] SPA_PATHS = {"/", "/index.html"};

    private byte[] htmlBytes;
    private byte[] gzipBytes;

    @PostConstruct
    void preload() throws IOException {
        Resource indexResource = new ClassPathResource("static/index.html");
        if (!indexResource.exists()) {
            logger.warn("[SpaShellPreload] static/index.html introuvable — filter no-op");
            this.htmlBytes = null;
            return;
        }
        try (InputStream in = indexResource.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            this.htmlBytes = out.toByteArray();
        }
        // Pré-compression gzip une seule fois au boot
        try (ByteArrayOutputStream gzOut = new ByteArrayOutputStream();
             GZIPOutputStream gz = new GZIPOutputStream(gzOut)) {
            gz.write(this.htmlBytes);
            gz.finish();
            this.gzipBytes = gzOut.toByteArray();
        }
        logger.info("[SpaShellPreload] index.html pré-chargé : raw={} bytes, gzip={} bytes (ratio {}%)",
                this.htmlBytes.length, this.gzipBytes.length,
                100 - (this.gzipBytes.length * 100 / Math.max(this.htmlBytes.length, 1)));
    }

    @Bean
    public FilterRegistrationBean<SpaShellPreloadFilter> spaShellPreloadFilterRegistration() {
        FilterRegistrationBean<SpaShellPreloadFilter> reg = new FilterRegistrationBean<>(this);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        reg.addUrlPatterns(SPA_PATHS);
        return reg;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (htmlBytes == null) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // Honor uniquement GET + HEAD (POST etc. → pass-through, ne devrait pas arriver pour `/`)
        String method = req.getMethod();
        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            chain.doFilter(request, response);
            return;
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/html;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-cache, must-revalidate");

        boolean acceptsGzip = false;
        String acceptEncoding = req.getHeader("Accept-Encoding");
        if (acceptEncoding != null && acceptEncoding.contains("gzip")) {
            acceptsGzip = true;
        }

        if ("HEAD".equals(method)) {
            int len = acceptsGzip ? gzipBytes.length : htmlBytes.length;
            if (acceptsGzip) {
                resp.setHeader("Content-Encoding", "gzip");
            }
            resp.setContentLength(len);
            return;
        }

        if (acceptsGzip) {
            resp.setHeader("Content-Encoding", "gzip");
            resp.setContentLength(gzipBytes.length);
            ServletOutputStream out = resp.getOutputStream();
            out.write(gzipBytes);
            out.flush();
        } else {
            resp.setContentLength(htmlBytes.length);
            ServletOutputStream out = resp.getOutputStream();
            out.write(htmlBytes);
            out.flush();
        }
    }
}
