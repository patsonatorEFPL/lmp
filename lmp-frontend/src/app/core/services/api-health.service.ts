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
  diskTotal?: number;
  diskFree?: number;
  diskThreshold?: number;
  diskUsagePercent?: number;
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

// ── Report types ─────────────────────────────────────────────────────────────

export interface ReportSummary {
  reportDate: string;
  generatedAt: string;
  totalRecords?: number;
  apiCount?: number;
}

export interface ApiReportEntry {
  name: string;
  totalCalls: number;
  successCount: number;
  successRate: number;
  avgLatencyMs: number;
  maxLatencyMs: number;
  p95LatencyMs: number;
  errorCount: number;
  topErrors: string[];
  hourlyBreakdown: { hour: number; calls: number; successRate: number; avgLatencyMs: number }[];
}

export interface DailyReport {
  reportDate: string;
  period: { from: string; to: string };
  apis: ApiReportEntry[];
  totalRecords: number;
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

  getReportsList(): Observable<ReportSummary[]> {
    return this.http
      .get<ApiResponse<ReportSummary[]>>('/api/v1/admin/api-health/reports')
      .pipe(map((res) => res.data!));
  }

  getReport(date: string): Observable<DailyReport> {
    return this.http
      .get<ApiResponse<DailyReport>>(`/api/v1/admin/api-health/reports/${date}`)
      .pipe(map((res) => res.data!));
  }
}
