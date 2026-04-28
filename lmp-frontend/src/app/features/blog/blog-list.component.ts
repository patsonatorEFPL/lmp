import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { BlogService } from './blog.service';
import { BlogPost } from './blog.model';
import { SeoService } from '../../core/services/seo.service';

@Component({
  selector: 'lmp-blog-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
      <div class="mx-auto max-w-3xl text-center mb-12">
        <h1 class="text-4xl font-bold tracking-tight text-(--foreground)">
          Blog & Ressources
        </h1>
        <p class="mt-4 text-base text-(--muted-foreground)">
          Conseils, stratégies et actualités du marketing digital et du référencement local.
        </p>
      </div>

      @if (loading()) {
        <div class="flex justify-center py-20">
          <div class="h-8 w-8 animate-spin rounded-full border-2 border-(--primary) border-t-transparent"></div>
        </div>
      } @else if (error()) {
        <div class="rounded-sm border border-red-200 bg-red-50 p-4 text-red-700">
          {{ error() }}
        </div>
      } @else if (posts().length === 0) {
        <div class="text-center py-20 text-(--muted-foreground)">
          Aucun article disponible pour le moment.
        </div>
      } @else {
        <div class="grid gap-8 md:grid-cols-2 lg:grid-cols-3">
          @for (post of posts(); track post.id) {
            <article class="group flex flex-col rounded-sm border border-(--border) bg-(--card) overflow-hidden hover:border-(--primary)/30 transition-colors">
              @if (post.coverImage) {
                <div class="aspect-[16/9] overflow-hidden">
                  <img [src]="post.coverImage" [alt]="post.title" class="h-full w-full object-cover transition-transform group-hover:scale-105" />
                </div>
              }
              <div class="flex flex-1 flex-col p-5">
                <div class="flex items-center gap-2 text-xs text-(--muted-foreground) mb-3">
                  @if (post.publishedAt) {
                    <time [attr.datetime]="post.publishedAt">
                      {{ post.publishedAt | date: 'longDate' : undefined : 'fr' }}
                    </time>
                  }
                  @if (post.authorName) {
                    <span>·</span>
                    <span>{{ post.authorName }}</span>
                  }
                </div>
                <h2 class="text-lg font-semibold text-(--foreground) mb-2 line-clamp-2">
                  <a [routerLink]="['/blog', post.slug]" class="hover:text-(--primary) transition-colors">
                    {{ post.title }}
                  </a>
                </h2>
                <p class="text-sm text-(--muted-foreground) line-clamp-3 mb-4 flex-1">
                  {{ post.excerpt }}
                </p>
                <a [routerLink]="['/blog', post.slug]" class="inline-flex items-center text-sm font-medium text-(--primary) hover:underline">
                  Lire l'article
                  <svg class="ml-1 h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 8l4 4m0 0l-4 4m4-4H3"/></svg>
                </a>
              </div>
            </article>
          }
        </div>
      }
    </div>
  `,
})
export class BlogListComponent implements OnInit {
  private readonly blogService = inject(BlogService);
  private readonly seo = inject(SeoService);

  posts = signal<BlogPost[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.seo.updateMeta({
      title: 'Blog — Conseils Marketing Digital & SEO',
      description: 'Découvrez nos conseils, stratégies et actualités sur le marketing digital, le référencement SEO local et la gestion de présence en ligne.',
      url: '/blog',
      keywords: 'blog marketing digital, conseils SEO, référencement local, marketing digital blog, LMP',
    });

    this.blogService.getPosts(0, 12).subscribe({
      next: (res) => {
        this.posts.set(res.data?.content ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set("Impossible de charger les articles.");
        this.loading.set(false);
      },
    });
  }
}
