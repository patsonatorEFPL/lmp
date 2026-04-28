package com.lmp.content.persistence;

import com.lmp.content.domain.BlogPost;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, UUID> {

    Optional<BlogPost> findBySlugAndPublishedTrue(String slug);

    Page<BlogPost> findAllByPublishedTrueOrderByPublishedAtDesc(Pageable pageable);

    boolean existsBySlug(String slug);
}
