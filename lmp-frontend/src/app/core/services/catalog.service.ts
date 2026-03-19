import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ServiceOffer {
  id: string;
  name: string;
  price: number;
  originalPrice: number | null;
  durationType: string | null;
  isDefault: boolean;
}

export interface ServiceItem {
  id: string;
  title: string;
  slug: string;
  description: string;
  icon: string;
  categoryName: string;
  categorySlug: string;
  featured: boolean;
  active: boolean;
  displayOrder: number;
  benefits: string[];
  currentOffer: ServiceOffer | null;
}

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
}

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);

  getServices(): Observable<ServiceItem[]> {
    return this.http
      .get<ApiResponse<ServiceItem[]>>(
        `${environment.apiUrl}/api/v1/services`,
        { withCredentials: true },
      )
      .pipe(map((res) => res.data ?? []));
  }

  getCategories(): Observable<{ name: string; slug: string }[]> {
    return this.getServices().pipe(
      map((services) => {
        const seen = new Set<string>();
        const cats: { name: string; slug: string }[] = [];
        for (const s of services) {
          if (!seen.has(s.categorySlug)) {
            seen.add(s.categorySlug);
            cats.push({ name: s.categoryName, slug: s.categorySlug });
          }
        }
        return cats;
      }),
    );
  }

  getFeaturedServices(): Observable<ServiceItem[]> {
    return this.http
      .get<ApiResponse<ServiceItem[]>>(
        `${environment.apiUrl}/api/v1/services/featured`,
        { withCredentials: true },
      )
      .pipe(map((res) => res.data ?? []));
  }
}
