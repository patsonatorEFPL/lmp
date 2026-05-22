import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { BlogPost } from '../../features/blog/blog.model';
import { ServiceResponse } from '../../generated/models/service-response';

interface ApiResponse<T> {
  success?: boolean;
  data: T;
  message?: string;
}

export interface BlogSearchPayload {
  results: BlogPost[];
  didYouMean: string[];
}

export interface GlobalSearchResult {
  services: ServiceResponse[];
  blog: BlogPost[];
  didYouMean: string[];
}

/**
 * Wraps the two backend full-text endpoints into a single parallel call.
 * Caller decides debouncing / cancellation — this service stays stateless
 * so we don't have to think about request races at the consumer level.
 */
@Injectable({ providedIn: 'root' })
export class SearchService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  searchServices(q: string, limit = 10): Observable<ServiceResponse[]> {
    return this.http
      .get<ApiResponse<ServiceResponse[]>>(`${this.base}/services/search`, {
        params: { q, limit },
      })
      .pipe(
        map((r) => r.data ?? []),
        catchError(() => of([])),
      );
  }

  searchBlog(q: string, limit = 10): Observable<BlogSearchPayload> {
    return this.http
      .get<ApiResponse<BlogSearchPayload>>(`${this.base}/blog/search`, {
        params: { q, limit },
      })
      .pipe(
        map((r) => r.data ?? { results: [], didYouMean: [] }),
        catchError(() => of({ results: [], didYouMean: [] })),
      );
  }

  /**
   * Single-shot global search: services + blog in parallel.
   * Backend already clamps limit; we pass 10 each.
   */
  searchAll(q: string): Observable<GlobalSearchResult> {
    return forkJoin({
      services: this.searchServices(q, 10),
      blog: this.searchBlog(q, 10),
    }).pipe(
      map(({ services, blog }) => ({
        services,
        blog: blog.results,
        didYouMean: blog.didYouMean,
      })),
    );
  }
}
