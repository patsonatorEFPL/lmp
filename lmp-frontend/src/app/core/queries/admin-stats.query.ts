import { inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { injectQuery, injectQueryClient } from '@tanstack/angular-query-experimental';

import { AdminService, AdminDashboardStats } from '../services/admin.service';

/**
 * Iter40 — Test slice TanStack Query sur /api/v1/admin/stats.
 *
 * Setup actuel admin-dashboard.component utilise Angular 21 `resource()` API
 * = cache per-component. Si user navigue admin→users→admin, refetch chaque mount.
 *
 * injectQuery = cache global via QueryClient. Cross-components dedup. SW-revalidate.
 *
 * Mesure ROI : compter hits backend /admin/stats avant vs après migration.
 */
export const ADMIN_STATS_QUERY_KEY = ['admin', 'stats'] as const;

export function injectAdminStatsQuery() {
  const adminService = inject(AdminService);
  return injectQuery(() => ({
    queryKey: ADMIN_STATS_QUERY_KEY,
    queryFn: (): Promise<AdminDashboardStats> =>
      firstValueFrom(adminService.getDashboardStats()),
  }));
}

/** Invalide cache stats — appelé sur SSE event ou mutation admin. */
export function invalidateAdminStats(): Promise<void> {
  const queryClient = injectQueryClient();
  return queryClient.invalidateQueries({ queryKey: ADMIN_STATS_QUERY_KEY });
}
