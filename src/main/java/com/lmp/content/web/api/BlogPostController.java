package com.lmp.content.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmp.content.domain.BlogPost;
import com.lmp.content.service.BlogPostService;
import com.lmp.shared.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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
 * API REST publique pour le blog.
 * GET endpoints sont publics (pas d'authentification).
 * POST/PUT/DELETE sont réservés aux admins.
 *
 * <p><b>Optim 2026-05-16:</b> response /search pre-sérialisée byte[] cached
 * par (query+limit). TTL 5 min. Évite Jackson serialize per request sous load.</p>
 */
@RestController
@RequestMapping("/api/v1/blog")
@Tag(name = "Blog", description = "Articles de blog pour le SEO")
public class BlogPostController {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final BlogPostService blogPostService;
    private final ObjectMapper objectMapper;

    private final Map<String, CachedResponse> searchCache = new ConcurrentHashMap<>();
    private final Map<String, CachedResponse> slugCache = new ConcurrentHashMap<>();

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
    public ResponseEntity<byte[]> getBySlug(@PathVariable String slug) throws IOException {
        CachedResponse c = slugCache.get(slug);
        Instant now = Instant.now();
        if (c != null && now.isBefore(c.expiresAt())) {
            return ResponseEntity.ok()
                    .cacheControl(PUBLIC_CACHE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(c.bytes());
        }
        var postOpt = blogPostService.findBySlug(slug);
        if (postOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        byte[] body = objectMapper.writeValueAsBytes(ApiResponse.ok(postOpt.get()));
        slugCache.put(slug, new CachedResponse(body, now.plus(CACHE_TTL)));
        return ResponseEntity.ok()
                .cacheControl(PUBLIC_CACHE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @GetMapping("/search")
    @Operation(summary = "Recherche full-text")
    public ResponseEntity<byte[]> search(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "limit", defaultValue = "20") int limit) throws IOException {
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
        CachedResponse c = searchCache.get(key);
        Instant now = Instant.now();
        if (c != null && now.isBefore(c.expiresAt())) {
            return ResponseEntity.ok()
                    .cacheControl(PUBLIC_CACHE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(c.bytes());
        }
        var result = blogPostService.searchPublishedWithFallback(query.trim(), safeLimit, 5);
        byte[] body = objectMapper.writeValueAsBytes(ApiResponse.ok(result));
        searchCache.put(key, new CachedResponse(body, now.plus(CACHE_TTL)));
        return ResponseEntity.ok()
                .cacheControl(PUBLIC_CACHE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
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

    private record CachedResponse(byte[] bytes, Instant expiresAt) {}
}
