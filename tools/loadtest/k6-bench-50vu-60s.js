// ============================================================================
// LMP — k6 micro-benchmark (50 VUs × 60s)
// ============================================================================
// Repeatable focused bench to measure incremental backend optimizations.
// Mix: 80% reads (cached + DB), 20% writes (register → bcrypt + INSERT).
//
// Run :
//   k6 run --summary-export=tmp-loadtest/bench-<label>.json tools/loadtest/k6-bench-50vu-60s.js
// ============================================================================
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE || 'https://dev.lmp-services.ca';

export const options = {
  scenarios: {
    bench: {
      executor: 'constant-vus',
      vus: 50,
      duration: '60s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<3000'],
  },
};

const READ_URLS = ['/api/v1/config', '/api/v1/services', '/api/v1/blog', '/'];

export default function () {
  const r = Math.random();
  if (r < 0.8) {
    const url = READ_URLS[Math.floor(Math.random() * READ_URLS.length)];
    const res = http.get(`${BASE}${url}`, { tags: { name: 'read' }, timeout: '10s' });
    check(res, { 'read 2xx/3xx': (x) => x.status >= 200 && x.status < 400 });
  } else {
    const email = `bench-${__VU}-${__ITER}-${Date.now()}@lmp.test`;
    const password = 'BenchTest1!';
    const payload = JSON.stringify({
      firstName: 'Bench',
      lastName: `VU${__VU}`,
      email,
      password,
      confirmPassword: password,
      acceptTerms: true,
    });
    const res = http.post(`${BASE}/api/v1/auth/register`, payload, {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'write' },
      timeout: '15s',
    });
    check(res, { 'register 2xx': (x) => x.status >= 200 && x.status < 300 });
  }
  sleep(0.5 + Math.random() * 0.5);
}
