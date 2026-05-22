package com.lmp.content.dto;

import com.lmp.content.domain.BlogPost;

import java.util.List;

/**
 * Wraps a blog full-text search response with an optional "did you mean"
 * branch. Populated only when the primary tsvector query returned zero
 * results; otherwise {@code didYouMean} is an empty list.
 */
public record BlogSearchResult(
        List<BlogPost> results,
        List<String> didYouMean
) {
    public static BlogSearchResult of(List<BlogPost> results) {
        return new BlogSearchResult(results, List.of());
    }

    public static BlogSearchResult empty(List<String> didYouMean) {
        return new BlogSearchResult(List.of(), didYouMean);
    }
}
