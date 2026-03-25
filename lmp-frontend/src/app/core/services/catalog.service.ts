import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

import { getAllServices1 } from '../../../app/generated/fn/services/get-all-services-1';
import { getFeaturedServices } from '../../../app/generated/fn/services/get-featured-services';
import { ApiResponseListServiceResponse } from '../../../app/generated/models/api-response-list-service-response';

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

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);

  getServices(): Observable<ServiceItem[]> {
    return this.http
      .get<ApiResponseListServiceResponse>(getAllServices1.PATH)
      .pipe(map((res) => (res.data ?? []) as ServiceItem[]));
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
      .get<ApiResponseListServiceResponse>(getFeaturedServices.PATH)
      .pipe(map((res) => (res.data ?? []) as ServiceItem[]));
  }
}
