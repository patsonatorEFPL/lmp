// ============================================================================
// LMP — k6 stress test
// ============================================================================
// Pousse jusqu'au point de rupture pour identifier le seuil. ATTENTION :
// peut saturer le backend / DB. Lancer hors heures de pointe.
//
// Profil :
//   0 → 100 → 300 → 500 VUs sur 10 min
//
// Run :
//   k6 run tools/loadtest/k6-stress.js
// ============================================================================
import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.BASE || 'https://dev.lmp-services.ca';

export const options = {
  scenarios: {
    stress: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m',  target: 100 },
        { duration: '2m',  target: 300 },
        { duration: '3m',  target: 500 },
        { duration: '2m',  target: 500 },
        { duration: '2m',  target: 0   },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    // Sous stress on accepte plus d'erreurs et de latence
    http_req_failed:   ['rate<0.10'],
    http_req_duration: ['p(95)<3000'],
  },
};

export default function () {
  const res = http.get(`${BASE}/api/v1/config`);
  check(res, { 'status ok': (r) => r.status >= 200 && r.status < 500 });
}
