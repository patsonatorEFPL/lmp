package com.lmp.content.service;

import com.lmp.content.domain.BlogPost;
import com.lmp.content.dto.BlogSearchResult;
import com.lmp.content.persistence.BlogPostRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BlogPostService {

    private final BlogPostRepository repository;

    public BlogPostService(BlogPostRepository repository) {
        this.repository = repository;
    }

    public Page<BlogPost> findPublished(Pageable pageable) {
        return repository.findAllByPublishedTrueOrderByPublishedAtDesc(pageable);
    }

    public Optional<BlogPost> findBySlug(String slug) {
        return repository.findBySlugAndPublishedTrue(slug);
    }

    /**
     * Full-text search across published posts. Caller is responsible for
     * trimming/validating the query string and clamping {@code max}.
     */
    public List<BlogPost> searchPublished(String query, int max) {
        return repository.searchPublished(query, max);
    }

    /**
     * Full-text search with a trigram "did you mean" fallback. When the
     * tsvector query returns nothing we run a similarity() probe on titles
     * and surface the top {@code suggestionsMax} closest matches so the
     * client can offer them as one-click corrections.
     */
    public BlogSearchResult searchPublishedWithFallback(String query, int max, int suggestionsMax) {
        List<BlogPost> hits = repository.searchPublished(query, max);
        if (!hits.isEmpty()) {
            return BlogSearchResult.of(hits);
        }
        List<String> suggestions = repository.suggestSimilarTitles(query, suggestionsMax);
        return BlogSearchResult.empty(suggestions);
    }

    @Transactional
    public BlogPost create(BlogPost post) {
        if (repository.existsBySlug(post.getSlug())) {
            throw new IllegalArgumentException("Un article avec ce slug existe déjà : " + post.getSlug());
        }
        return repository.save(post);
    }

    @Transactional
    public BlogPost update(UUID id, BlogPost updated) {
        BlogPost existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article non trouvé : " + id));

        existing.setTitle(updated.getTitle());
        existing.setSlug(updated.getSlug());
        existing.setExcerpt(updated.getExcerpt());
        existing.setContent(updated.getContent());
        existing.setCoverImage(updated.getCoverImage());
        existing.setMetaKeywords(updated.getMetaKeywords());
        existing.setAuthorName(updated.getAuthorName());
        existing.setPublished(updated.getPublished());

        return repository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
