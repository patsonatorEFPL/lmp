// ============================================================================
// LMP — k6 stress test DB-focused (1000 VUs, no bcrypt)
// ============================================================================
// Hammer Postgres via JPA/Hibernate, sans bcrypt côté CPU. Le bottleneck
// recherché est la DB (HikariCP, lock contention, query plan, write IO),
// pas le coût crypto.
//
// READS-only multi-endpoint pour stresser Postgres :
//   /api/v1/services (joins offers/benefits/categories)
//   /api/v1/services/{slug} (lookup par slug + relations)
//   /api/v1/blog (paginated SELECT)
//   /api/v1/blog/{slug} (article par slug + comments?)
//   /api/v1/config (JOIN sur SiteConfig)
//
// Écritures volontairement omises ici (appointments → validation business
// + UUID resolution + time conflicts; contact → Mailtrap; register → bcrypt
// CPU-bound). Pour tester l'écriture DB pure, voir test futur dédié.
//
// Profile :
//   0 → 200 (1m) → 600 (2m) → 1000 (2m) → 1000 sustain (3m) → 0 (1m)
//
// Run :
//   k6 run tools/loadtest/k6-stress-db.js
//
// Watch Grafana :
//   - PostgreSQL (9628) : connections, transactions, lock waits, IO
//   - SpringBoot APM (12900) : HikariCP active/pending
//   - Node Exporter (1860) : disk IO, RAM Postgres
// ============================================================================
import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.BASE || 'https://dev.lmp-services.ca';

export const options = {
  scenarios: {
    db_stress: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m',  target: 200  },
        { duration: '2m',  target: 600  },
        { duration: '2m',  target: 1000 },
        { duration: '3m',  target: 1000 },
        { duration: '1m',  target: 0    },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    http_req_failed:   ['rate<0.20'],
    http_req_duration: ['p(95)<5000'],
    'http_req_duration{name:db-read}':  ['p(95)<1000'],
  },
};

// Multiple read endpoints pour solliciter différents query plans et tables
const READ_ENDPOINTS = [
  '/api/v1/services',
  '/api/v1/blog',
  '/api/v1/config',
];

export default function () {
  const url = READ_ENDPOINTS[Math.floor(Math.random() * READ_ENDPOINTS.length)];
  const res = http.get(`${BASE}${url}`, { tags: { name: 'db-read' } });
  check(res, { 'db-read 2xx': (x) => x.status >= 200 && x.status < 300 });
}
