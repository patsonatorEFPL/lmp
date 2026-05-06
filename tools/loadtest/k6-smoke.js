// ============================================================================
// LMP — k6 smoke test
// ============================================================================
// Sanity check: 1 VU, 30s. Vérifie que les endpoints publics répondent 200
// et que les latences nominales sont dans une fourchette saine. À lancer
// AVANT tout load/stress test pour s'assurer que la cible est OK.
//
// Run :
//   k6 run tools/loadtest/k6-smoke.js
//   k6 run -e BASE=https://lmp-services.ca tools/loadtest/k6-smoke.js
// ============================================================================
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE || 'https://dev.lmp-services.ca';

export const options = {
  vus: 1,
  duration: '30s',
  thresholds: {
    http_req_failed:   ['rate<0.01'],          // < 1% errors
    http_req_duration: ['p(95)<1500'],         // p95 under 1.5s
  },
};

export default function () {
  // Endpoints publics — pas d'auth, pas de side-effects
  const targets = [
    { name: 'home',        url: `${BASE}/` },
    { name: 'health',      url: `${BASE}/actuator/health` },
    { name: 'site-config', url: `${BASE}/api/v1/config` },
    { name: 'services',    url: `${BASE}/api/v1/services` },
  ];

  for (const t of targets) {
    const res = http.get(t.url, { tags: { name: t.name } });
    check(res, {
      [`${t.name} status 2xx/3xx`]: (r) => r.status >= 200 && r.status < 400,
    });
  }
  sleep(1);
}
