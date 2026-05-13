-- ============================================================================
-- V42 — Full-text search on blog_posts + services (PostgreSQL native tsvector)
-- ============================================================================
-- Replaces LIKE '%term%' sequential scans by GIN-indexed tsvector queries.
-- Weighted columns (title > excerpt/description > content) so title matches
-- rank highest. French config bundles French stemming + stop words.
--
-- Indexes scoped to published_at IS NOT NULL on blog_posts so drafts don't
-- pollute search results, while services index covers active rows only.
-- ============================================================================

-- pg_trgm enables fuzzy/typo-tolerant matching via similarity() in a follow-up
-- query layer. Cheap to enable — install once, idempotent.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ==================== BLOG POSTS ====================

ALTER TABLE blog_posts
    ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('french', coalesce(title, '')),         'A') ||
        setweight(to_tsvector('french', coalesce(excerpt, '')),       'B') ||
        setweight(to_tsvector('french', coalesce(meta_keywords, '')), 'B') ||
        setweight(to_tsvector('french', coalesce(content, '')),       'C')
    ) STORED;

CREATE INDEX idx_blog_posts_search
    ON blog_posts USING GIN (search_vector)
    WHERE published_at IS NOT NULL;

-- Trigram index on title for "similar title" / typo-tolerant lookups
CREATE INDEX idx_blog_posts_title_trgm
    ON blog_posts USING GIN (lower(title) gin_trgm_ops)
    WHERE published_at IS NOT NULL;

COMMENT ON COLUMN blog_posts.search_vector IS
    'Generated full-text index. Weights: A=title, B=excerpt+keywords, C=content. Config=french.';

-- ==================== SERVICES ====================

ALTER TABLE services
    ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('french', coalesce(title, '')),       'A') ||
        setweight(to_tsvector('french', coalesce(description, '')), 'B')
    ) STORED;

CREATE INDEX idx_services_search
    ON services USING GIN (search_vector)
    WHERE active = true;

CREATE INDEX idx_services_title_trgm
    ON services USING GIN (lower(title) gin_trgm_ops)
    WHERE active = true;

COMMENT ON COLUMN services.search_vector IS
    'Generated full-text index. Weights: A=title, B=description. Config=french.';
