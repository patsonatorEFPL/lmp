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
import java.util.Set;
import java.util.zip.GZIPOutputStream;

/**
 * Pre-charge {@code /index.html} (Angular SPA shell) en byte[] au boot et
 * sert direct sur GET {@code /} OR {@code /index.html} via Filter
 * HIGHEST_PRECEDENCE.
 *
 * <p>Pattern référencé : Pinterest static asset serving (pre-compressed at
 * boot), Cloudflare Workers static serving (sub-50µs per request).
 * {@code index.html} est statique (généré au build Angular).</p>
 *
 * <p><b>Note technique :</b> filter registered sur URL pattern {@code /*}
 * (pas {@code /} qui est ambigu en Servlet spec = default-servlet only).
 * URI match fait manuellement dans {@link #doFilter}. Pour non-match,
 * pass-through immédiat (overhead négligeable).</p>
 */
@Configuration
public class SpaShellPreloadFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(SpaShellPreloadFilter.class);
    private static final Set<String> SPA_URIS = Set.of("/", "/index.html");

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
        try (ByteArrayOutputStream gzOut = new ByteArrayOutputStream();
             GZIPOutputStream gz = new GZIPOutputStream(gzOut)) {
            gz.write(this.htmlBytes);
            gz.finish();
            this.gzipBytes = gzOut.toByteArray();
        }
        logger.info("[SpaShellPreload] index.html pré-chargé : raw={} bytes, gzip={} bytes ({}% reduction)",
                this.htmlBytes.length, this.gzipBytes.length,
                100 - (this.gzipBytes.length * 100 / Math.max(this.htmlBytes.length, 1)));
    }

    @Bean
    public FilterRegistrationBean<SpaShellPreloadFilter> spaShellPreloadFilterRegistration() {
        FilterRegistrationBean<SpaShellPreloadFilter> reg = new FilterRegistrationBean<>(this);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        reg.addUrlPatterns("/*");
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
        String uri = req.getRequestURI();
        if (!SPA_URIS.contains(uri)) {
            chain.doFilter(request, response);
            return;
        }
        String method = req.getMethod();
        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletResponse resp = (HttpServletResponse) response;
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

        byte[] payload = acceptsGzip ? gzipBytes : htmlBytes;
        if (acceptsGzip) {
            resp.setHeader("Content-Encoding", "gzip");
        }
        resp.setContentLength(payload.length);
        ServletOutputStream out = resp.getOutputStream();
        out.write(payload);
        out.flush();
    }
}
