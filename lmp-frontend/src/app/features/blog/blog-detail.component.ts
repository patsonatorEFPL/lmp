import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { BlogService } from './blog.service';
import { BlogPost } from './blog.model';
import { SeoService } from '../../core/services/seo.service';

@Component({
  selector: 'lmp-blog-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="mx-auto max-w-3xl px-4 py-16 sm:px-6 lg:px-8">
      @if (loading()) {
        <div class="flex justify-center py-20">
          <div class="h-8 w-8 animate-spin rounded-full border-2 border-(--primary) border-t-transparent"></div>
        </div>
      } @else if (error()) {
        <div class="rounded-sm border border-red-200 bg-red-50 p-4 text-red-700 mb-6">
          {{ error() }}
        </div>
        <a routerLink="/blog" class="text-sm text-(--primary) hover:underline">
          ← Retour au blog
        </a>
      } @else if (post()) {
        <article>
          <div class="mb-8">
            <a routerLink="/blog" class="text-sm text-(--primary) hover:underline mb-4 inline-block">
              ← Blog
            </a>
            <h1 class="text-3xl sm:text-4xl font-bold tracking-tight text-(--foreground) mb-4">
              {{ post()!.title }}
            </h1>
            <div class="flex items-center gap-3 text-sm text-(--muted-foreground)">
              @if (post()!.publishedAt) {
                <time [attr.datetime]="post()!.publishedAt">
                  {{ post()!.publishedAt | date: 'longDate' : undefined : 'fr' }}
                </time>
              }
              @if (post()!.authorName) {
                <span>·</span>
                <span>{{ post()!.authorName }}</span>
              }
            </div>
          </div>

          @if (post()!.coverImage) {
            <div class="aspect-[21/9] overflow-hidden rounded-sm mb-10">
              <img [src]="post()!.coverImage" [alt]="post()!.title" class="h-full w-full object-cover" />
            </div>
          }

          <div class="prose prose-sm max-w-none dark:prose-invert text-(--foreground)">
            <div [innerHTML]="post()!.content"></div>
          </div>
        </article>
      }
    </div>
  `,
})
export class BlogDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly blogService = inject(BlogService);
  private readonly seo = inject(SeoService);

  post = signal<BlogPost | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug');
    if (!slug) {
      this.error.set('Article non trouvé');
      this.loading.set(false);
      return;
    }

    this.blogService.getPostBySlug(slug).subscribe({
      next: (res) => {
        const p = res.data;
        this.post.set(p);
        this.loading.set(false);

        this.seo.updateMeta({
          title: p.title,
          description: p.excerpt,
          url: `/blog/${p.slug}`,
          keywords: p.metaKeywords ?? 'blog, marketing digital, SEO',
          ogImage: p.coverImage,
          ogType: 'article',
        });

        this.seo.setJsonLd({
          '@context': 'https://schema.org',
          '@type': 'BlogPosting',
          headline: p.title,
          description: p.excerpt,
          image: p.coverImage ? [p.coverImage] : undefined,
          url: `https://lmp-services.ca/blog/${p.slug}`,
          datePublished: p.publishedAt,
          dateModified: p.updatedAt ?? p.publishedAt,
          author: p.authorName
            ? { '@type': 'Person', name: p.authorName }
            : { '@type': 'Organization', name: 'LMP Digital Services' },
          publisher: {
            '@type': 'Organization',
            name: 'LMP Digital Services',
            logo: {
              '@type': 'ImageObject',
              url: 'https://lmp-services.ca/images/logo-lmp.webp',
            },
          },
        });
      },
      error: () => {
        this.error.set("Article non trouvé ou indisponible.");
        this.loading.set(false);
      },
    });
  }
}
