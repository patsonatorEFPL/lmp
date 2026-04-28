package com.lmp.content.web.api;

import com.lmp.content.domain.BlogPost;
import com.lmp.content.service.BlogPostService;
import com.lmp.shared.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * API REST publique pour le blog.
 * GET endpoints sont publics (pas d'authentification).
 * POST/PUT/DELETE sont réservés aux admins.
 */
@RestController
@RequestMapping("/api/v1/blog")
@Tag(name = "Blog", description = "Articles de blog pour le SEO")
public class BlogPostController {

    private final BlogPostService blogPostService;

    public BlogPostController(BlogPostService blogPostService) {
        this.blogPostService = blogPostService;
    }

    @GetMapping
    @Operation(summary = "Lister les articles publiés", description = "Retourne les articles de blog publiés, paginés")
    public ResponseEntity<ApiResponse<Page<BlogPost>>> listPublished(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<BlogPost> posts = blogPostService.findPublished(pageable);
        return ResponseEntity.ok(ApiResponse.ok(posts));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Détail d'un article", description = "Retourne un article par son slug")
    public ResponseEntity<ApiResponse<BlogPost>> getBySlug(@PathVariable String slug) {
        return blogPostService.findBySlug(slug)
                .map(post -> ResponseEntity.ok(ApiResponse.ok(post)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un article", description = "Créer un nouvel article de blog (admin only)")
    public ResponseEntity<ApiResponse<BlogPost>> create(@RequestBody BlogPost post) {
        BlogPost created = blogPostService.create(post);
        return ResponseEntity.ok(ApiResponse.ok(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier un article", description = "Modifier un article existant (admin only)")
    public ResponseEntity<ApiResponse<BlogPost>> update(@PathVariable UUID id, @RequestBody BlogPost post) {
        BlogPost updated = blogPostService.update(id, post);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un article", description = "Supprimer un article (admin only)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        blogPostService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
