package com.lmp.content.service;

import com.lmp.content.SitemapBlogQuery;
import com.lmp.content.SitemapEntry;
import com.lmp.content.persistence.BlogPostRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
class SitemapBlogQueryImpl implements SitemapBlogQuery {

    private final BlogPostRepository repository;

    SitemapBlogQueryImpl(BlogPostRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<SitemapEntry> findPublishedForSitemap() {
        return repository.findAllByPublishedTrueOrderByPublishedAtDesc(null)
                .getContent().stream()
                .map(p -> new SitemapEntry(p.getSlug(), p.getUpdatedAt(), p.getPublishedAt()))
                .toList();
    }
}
