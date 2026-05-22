package com.lmp.shared.web;

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
 * boot), Cloudflare Workers static serving (sub-50µs per request).</p>
 *
 * <p><b>Note implem:</b> classe @Configuration NE doit PAS implementer Filter.
 * Sinon Spring auto-détecte le bean Filter ET la FilterRegistrationBean
 * l'enregistre aussi → double-registration → Tomcat context init fail.
 * Solution : Filter défini en classe interne, instantiée et passée à
 * FilterRegistrationBean — pas de bean Filter standalone.</p>
 */
@Configuration
public class SpaShellPreloadConfig {

    private static final Logger logger = LoggerFactory.getLogger(SpaShellPreloadConfig.class);
    private static final Set<String> SPA_URIS = Set.of("/", "/index.html");

    @Bean
    public FilterRegistrationBean<Filter> spaShellPreloadFilterRegistration() throws IOException {
        SpaShell shell = SpaShell.load();
        Filter filter = (request, response, chain) -> doFilter(shell, request, response, chain);
        FilterRegistrationBean<Filter> reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        reg.addUrlPatterns("/*");
        reg.setName("spaShellPreloadFilter");
        return reg;
    }

    private static void doFilter(SpaShell shell, ServletRequest request, ServletResponse response,
                                  FilterChain chain) throws IOException, ServletException {
        if (shell == null || shell.htmlBytes == null) {
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
            int len = acceptsGzip ? shell.gzipBytes.length : shell.htmlBytes.length;
            if (acceptsGzip) {
                resp.setHeader("Content-Encoding", "gzip");
            }
            resp.setContentLength(len);
            return;
        }

        byte[] payload = acceptsGzip ? shell.gzipBytes : shell.htmlBytes;
        if (acceptsGzip) {
            resp.setHeader("Content-Encoding", "gzip");
        }
        resp.setContentLength(payload.length);
        ServletOutputStream out = resp.getOutputStream();
        out.write(payload);
        out.flush();
    }

    private record SpaShell(byte[] htmlBytes, byte[] gzipBytes) {
        static SpaShell load() throws IOException {
            Resource indexResource = new ClassPathResource("static/index.html");
            if (!indexResource.exists()) {
                logger.warn("[SpaShellPreload] static/index.html introuvable — filter no-op");
                return new SpaShell(null, null);
            }
            byte[] html;
            try (InputStream in = indexResource.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                in.transferTo(out);
                html = out.toByteArray();
            }
            byte[] gzip;
            try (ByteArrayOutputStream gzOut = new ByteArrayOutputStream();
                 GZIPOutputStream gz = new GZIPOutputStream(gzOut)) {
                gz.write(html);
                gz.finish();
                gzip = gzOut.toByteArray();
            }
            logger.info("[SpaShellPreload] index.html pré-chargé : raw={} bytes, gzip={} bytes ({}% reduction)",
                    html.length, gzip.length, 100 - (gzip.length * 100 / Math.max(html.length, 1)));
            return new SpaShell(html, gzip);
        }
    }
}
