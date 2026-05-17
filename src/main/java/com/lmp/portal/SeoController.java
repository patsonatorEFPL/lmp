package com.lmp.portal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Contrôleur SEO pour la gestion du robots.txt.
 * Le sitemap.xml est géré par {@link com.lmp.seo.web.SitemapController}.
 *
 * <p><b>Iter32 :</b> body bâti une seule fois au @PostConstruct ; ResponseEntity
 * pré-construit avec Cache-Control public. Skip StringBuilder + 2 INFO logs
 * par requête (5% du mix réaliste = 200 r/s à 4k r/s total).</p>
 */
@Controller
public class SeoController {

    @Value("${app.base.url}")
    private String baseUrl;

    private static final CacheControl PUBLIC_CACHE = CacheControl
            .maxAge(Duration.ofHours(1))
            .cachePublic();

    private byte[] cachedBody;

    @PostConstruct
    void init() {
        String body = """
                User-agent: *
                Allow: /
                Allow: /css/
                Allow: /js/
                Allow: /images/
                Allow: /favicon.ico

                # Pages privées et administratives
                Disallow: /admin/
                Disallow: /user/
                Disallow: /api/
                Disallow: /debug/

                # Pages de processus
                Disallow: /auth/
                Disallow: /payment/
                Disallow: /stripe/

                # Optimisations Google
                User-agent: Googlebot
                Allow: /
                Crawl-delay: 1

                # Sitemap
                Sitemap: %s/sitemap.xml
                """.formatted(baseUrl);
        this.cachedBody = body.getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> robots() {
        return ResponseEntity.ok()
                .cacheControl(PUBLIC_CACHE)
                .contentType(MediaType.TEXT_PLAIN)
                .body(cachedBody);
    }
}
