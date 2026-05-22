package com.lmp.content.persistence;

import com.lmp.content.domain.BlogPost;

import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, UUID> {

    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Optional<BlogPost> findBySlugAndPublishedTrue(String slug);

    Page<BlogPost> findAllByPublishedTrueOrderByPublishedAtDesc(Pageable pageable);

    boolean existsBySlug(String slug);

    /**
     * Full-text search across published blog posts using the V42 GIN-indexed
     * {@code search_vector}. Ranks by {@code ts_rank_cd} so weight-A title
     * matches surface above content matches.
     *
     * <p>Uses {@code websearch_to_tsquery} which accepts user-friendly syntax
     * ({@code "exact phrase"}, {@code -negation}, {@code OR}) without throwing
     * on malformed input — the right primitive for a public search box.</p>
     */
    @Query(value = """
            SELECT * FROM blog_posts
             WHERE published_at IS NOT NULL
               AND search_vector @@ websearch_to_tsquery('french', :query)
             ORDER BY ts_rank_cd(search_vector, websearch_to_tsquery('french', :query)) DESC,
                      published_at DESC
             LIMIT :max
            """, nativeQuery = true)
    @QueryHints({
            @QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @QueryHint(name = "org.hibernate.fetchSize", value = "50")
    })
    List<BlogPost> searchPublished(@Param("query") String query, @Param("max") int max);

    /**
     * Trigram-similarity title suggestions, used as a "did you mean" fallback
     * when {@link #searchPublished} returns nothing. Uses the V42 GIN
     * trigram index ({@code lower(title) gin_trgm_ops}). The 0.2 similarity
     * floor is loose enough to forgive a 1–2 character typo on a short
     * title; tighten to 0.3 if results feel noisy on real traffic.
     */
    @Query(value = """
            SELECT title FROM blog_posts
             WHERE published_at IS NOT NULL
               AND similarity(lower(title), lower(:query)) > 0.2
             ORDER BY similarity(lower(title), lower(:query)) DESC
             LIMIT :max
            """, nativeQuery = true)
    List<String> suggestSimilarTitles(@Param("query") String query, @Param("max") int max);
}
