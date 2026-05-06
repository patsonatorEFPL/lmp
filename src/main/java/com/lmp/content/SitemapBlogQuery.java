package com.lmp.content;

import java.util.List;

/**
 * API publique du module content pour la consommation SEO du sitemap.
 * Implémentation interne dans content.service.
 */
public interface SitemapBlogQuery {

    List<SitemapEntry> findPublishedForSitemap();
}
