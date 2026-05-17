package com.lmp.content.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmp.content.domain.BlogPost;
import com.lmp.content.service.BlogPostService;
import com.lmp.shared.dto.ApiResponse;
import com.lmp.shared.web.PrecompressedResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API REST publique pour le blog. GET endpoints publics, POST/PUT/DELETE admin.
 *
 * <p><b>Optim 2026-05-17:</b> response cached en {@link PrecompressedResponse}
 * (raw + gzip pre-compressed). Skip Jackson serialize + gzip compress per
 * request sur cache hit.</p>
 */
@RestController
@RequestMapping("/api/v1/blog")
@Tag(name = "Blog", description = "Articles de blog pour le SEO")
public class BlogPostController {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final BlogPostService blogPostService;
    private final ObjectMapper objectMapper;

    private final Map<String, PrecompressedResponse> searchCache = new ConcurrentHashMap<>();
    private final Map<String, PrecompressedResponse> slugCache = new ConcurrentHashMap<>();

    public BlogPostController(BlogPostService blogPostService, ObjectMapper objectMapper) {
        this.blogPostService = blogPostService;
        this.objectMapper = objectMapper;
    }

    private static final CacheControl PUBLIC_CACHE = CacheControl
            .maxAge(Duration.ofMinutes(5))
            .cachePublic();

    @GetMapping
    @Operation(summary = "Lister les articles publiés", description = "Retourne les articles de blog publiés, paginés")
    public ResponseEntity<ApiResponse<Page<BlogPost>>> listPublished(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<BlogPost> posts = blogPostService.findPublished(pageable);
        return ResponseEntity.ok()
                .cacheControl(PUBLIC_CACHE)
                .body(ApiResponse.ok(posts));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Détail d'un article", description = "Retourne un article par son slug")
    public ResponseEntity<byte[]> getBySlug(@PathVariable String slug, HttpServletRequest httpRequest) throws IOException {
        PrecompressedResponse c = slugCache.get(slug);
        Instant now = Instant.now();
        if (c != null && now.isBefore(c.expiresAt())) {
            return serve(httpRequest, c);
        }
        var postOpt = blogPostService.findBySlug(slug);
        if (postOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        byte[] body = objectMapper.writeValueAsBytes(ApiResponse.ok(postOpt.get()));
        PrecompressedResponse r = PrecompressedResponse.build(body, now.plus(CACHE_TTL));
        slugCache.put(slug, r);
        return serve(httpRequest, r);
    }

    @GetMapping("/search")
    @Operation(summary = "Recherche full-text")
    public ResponseEntity<byte[]> search(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            HttpServletRequest httpRequest) throws IOException {
        if (query == null || query.isBlank()) {
            byte[] empty = objectMapper.writeValueAsBytes(
                    ApiResponse.ok(com.lmp.content.dto.BlogSearchResult.of(java.util.List.of())));
            return ResponseEntity.ok()
                    .cacheControl(PUBLIC_CACHE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(empty);
        }
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        String key = query.trim() + "|" + safeLimit;
        PrecompressedResponse c = searchCache.get(key);
        Instant now = Instant.now();
        if (c != null && now.isBefore(c.expiresAt())) {
            return serve(httpRequest, c);
        }
        var result = blogPostService.searchPublishedWithFallback(query.trim(), safeLimit, 5);
        byte[] body = objectMapper.writeValueAsBytes(ApiResponse.ok(result));
        PrecompressedResponse r = PrecompressedResponse.build(body, now.plus(CACHE_TTL));
        searchCache.put(key, r);
        return serve(httpRequest, r);
    }

    private ResponseEntity<byte[]> serve(HttpServletRequest req, PrecompressedResponse r) {
        String ifNoneMatch = req.getHeader("If-None-Match");
        if (ifNoneMatch != null && ifNoneMatch.contains(r.etag())) {
            return ResponseEntity.status(304)
                    .cacheControl(PUBLIC_CACHE)
                    .header("ETag", r.etag())
                    .header("Vary", "Accept-Encoding")
                    .build();
        }
        boolean gz = false;
        String ae = req.getHeader("Accept-Encoding");
        if (ae != null && ae.contains("gzip")) {
            gz = true;
        }
        ResponseEntity.BodyBuilder b = ResponseEntity.ok()
                .cacheControl(PUBLIC_CACHE)
                .header("ETag", r.etag())
                .header("Vary", "Accept-Encoding")
                .contentType(MediaType.APPLICATION_JSON);
        if (gz) {
            b.header("Content-Encoding", "gzip");
            return b.body(r.gzip());
        }
        return b.body(r.raw());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un article")
    public ResponseEntity<ApiResponse<BlogPost>> create(@RequestBody BlogPost post) {
        BlogPost created = blogPostService.create(post);
        searchCache.clear();
        slugCache.clear();
        return ResponseEntity.ok(ApiResponse.ok(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier un article")
    public ResponseEntity<ApiResponse<BlogPost>> update(@PathVariable UUID id, @RequestBody BlogPost post) {
        BlogPost updated = blogPostService.update(id, post);
        searchCache.clear();
        slugCache.clear();
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un article")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        blogPostService.delete(id);
        searchCache.clear();
        slugCache.clear();
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
