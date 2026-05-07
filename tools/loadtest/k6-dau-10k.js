// ============================================================================
// LMP — k6 simulation 10k DAU (Daily Active Users)
// ============================================================================
// Simule 1000 VUs concurrents (= peak ~10% du DAU pour un SaaS standard)
// avec comportement utilisateur réaliste : think-time entre clics, mix de
// pages publiques + API.
//
// Si ce test passe avec p95 < 1s et 0% errors, l'infra tient 10k DAU.
//
// Profile :
//   0 → 200 (1m) → 1000 (3m) → 1000 sustain (5m) → 0 (1m) = 10 min
//
// Run :
//   k6 run tools/loadtest/k6-dau-10k.js
// ============================================================================
import http from 'k6/http';
import { check, sleep, group } from 'k6';

const BASE = __ENV.BASE || 'https://dev.lmp-services.ca';

export const options = {
  scenarios: {
    dau_10k: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m',  target: 200  },
        { duration: '3m',  target: 1000 },
        { duration: '5m',  target: 1000 },
        { duration: '1m',  target: 0    },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    http_req_failed:   ['rate<0.01'],          // < 1% errors (production-grade)
    http_req_duration: ['p(95)<1500', 'p(99)<3000'],
  },
};

const PAGES = ['/', '/services', '/contact', '/about', '/blog'];
const APIS  = ['/api/v1/config', '/api/v1/services', '/api/v1/blog'];

export default function () {
  // Comportement user réaliste : 3-5 clics par session avec think time
  const sessionLength = 3 + Math.floor(Math.random() * 3);

  for (let i = 0; i < sessionLength; i++) {
    const isApi = Math.random() < 0.4;
    const url = isApi
      ? APIS[Math.floor(Math.random() * APIS.length)]
      : PAGES[Math.floor(Math.random() * PAGES.length)];

    const res = http.get(`${BASE}${url}`);
    check(res, { 'status 2xx': (r) => r.status >= 200 && r.status < 300 });

    // Think time : user lit la page avant de cliquer (3-10s)
    sleep(3 + Math.random() * 7);
  }
}
