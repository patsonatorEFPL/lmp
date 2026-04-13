import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

// ── Types ────────────────────────────────────────────────────────────────────

export type ApiStatus = 'UP' | 'DEGRADED' | 'DOWN' | 'UNKNOWN';

export interface ApiHealthEntry {
  name: string;
  status: ApiStatus;
  avgLatencyMs: number;
  successRate: number;
  totalCalls: number;
  lastCallAt: string | null;
  lastError: string | null;
}

export interface InfraHealth {
  db: string;
  diskSpace: string;
}

export interface ApiHealthSnapshot {
  apis: ApiHealthEntry[];
  infra: InfraHealth;
  timestamp: string;
}

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
}

// ── Service ──────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class ApiHealthService {
  private readonly http = inject(HttpClient);

  getHealthSnapshot(): Observable<ApiHealthSnapshot> {
    return this.http
      .get<ApiResponse<ApiHealthSnapshot>>('/api/v1/admin/api-health')
      .pipe(map((res) => res.data!));
  }

  probeAll(): Observable<Record<string, string>> {
    return this.http
      .post<ApiResponse<Record<string, string>>>('/api/v1/admin/api-health/probe', {})
      .pipe(map((res) => res.data!));
  }
}
