import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BlogPost } from './blog.model';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../../shared/models/api.models';

interface ApiResponse<T> {
  data: T;
  message?: string;
}

@Injectable({ providedIn: 'root' })
export class BlogService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/blog`;

  getPosts(page = 0, size = 10): Observable<ApiResponse<PageResponse<BlogPost>>> {
    return this.http.get<ApiResponse<PageResponse<BlogPost>>>(
      `${this.baseUrl}?page=${page}&size=${size}`,
    );
  }

  getPostBySlug(slug: string): Observable<ApiResponse<BlogPost>> {
    return this.http.get<ApiResponse<BlogPost>>(`${this.baseUrl}/${slug}`);
  }
}
