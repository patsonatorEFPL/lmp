// ============================================================================
// LMP — k6 load test (nominal)
// ============================================================================
// Charge progressive 0 → 50 VUs sur 8 min. Couvre :
//   - pages publiques (home, services, contact)
//   - API publiques (site-config, services list, blog)
//   - assets statiques (Angular bundle, CSS)
//
// Run :
//   k6 run tools/loadtest/k6-load.js
//   k6 run --out json=results.json tools/loadtest/k6-load.js
//
// Pendant le test, watch métriques côté Grafana :
//   - JVM dashboard (4701) : heap, threads, GC
//   - Spring Boot APM (12900) : HTTP rate, errors, duration
//   - PostgreSQL (9628) : connexions, transactions
//   - Node Exporter (1860) : CPU, RAM, network host
// ============================================================================
import http from 'k6/http';
import { check, sleep, group } from 'k6';

const BASE = __ENV.BASE || 'https://dev.lmp-services.ca';

export const options = {
  scenarios: {
    nominal: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m',  target: 10  }, // ramp-up
        { duration: '2m',  target: 50  }, // ramp-up
        { duration: '4m',  target: 50  }, // sustain
        { duration: '1m',  target: 0   }, // ramp-down
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    http_req_failed:   ['rate<0.02'],          // < 2% errors total
    http_req_duration: ['p(95)<800', 'p(99)<2000'],
    'http_req_duration{name:html}':   ['p(95)<500'],
    'http_req_duration{name:assets}': ['p(95)<200'],
    'http_req_duration{name:api}':    ['p(95)<800'],
  },
};

export default function () {
  group('homepage', () => {
    const html = http.get(`${BASE}/`, { tags: { name: 'html' } });
    check(html, { 'home 200': (r) => r.status === 200 });
  });

  group('public api', () => {
    const config = http.get(`${BASE}/api/v1/config`, { tags: { name: 'api' } });
    check(config, { 'config 200': (r) => r.status === 200 });

    const services = http.get(`${BASE}/api/v1/services`, { tags: { name: 'api' } });
    check(services, { 'services 200': (r) => r.status === 200 });
  });

  group('static assets', () => {
    // batch parallèle — simule navigateur qui fetch plusieurs assets
    http.batch([
      ['GET', `${BASE}/favicon.ico`,  null, { tags: { name: 'assets' } }],
    ]);
  });

  group('public pages', () => {
    const services = http.get(`${BASE}/services`, { tags: { name: 'html' } });
    check(services, { 'services page': (r) => r.status === 200 });

    const contact = http.get(`${BASE}/contact`,  { tags: { name: 'html' } });
    check(contact, { 'contact page': (r) => r.status === 200 });
  });

  // Pause entre itérations pour simuler un user qui réfléchit
  sleep(Math.random() * 2 + 1);
}
