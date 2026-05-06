package com.lmp.content;

import java.time.LocalDateTime;

/**
 * Donnée publique exposée par le module content pour le module seo.
 * Évite à seo de dépendre de l'entité BlogPost interne (Modulith encapsulation).
 */
public record SitemapEntry(String slug, LocalDateTime updatedAt, LocalDateTime publishedAt) {
}
