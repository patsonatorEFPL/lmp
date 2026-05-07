// ============================================================================
// LMP — k6 simulation 10k actifs simultanés (B)
// ============================================================================
// 5000 VUs concurrent (= ~50% du DAU actif simultanément, pic extrême).
// Mix réaliste 70% read + 30% write (register), think-time court (1-2s).
//
// Objectif: trouver point de rupture du backend sous trafic actif réel.
// Saturation probable étant donné 2 vCPU + bcrypt.
//
// Profile :
//   0 → 1000 (1m) → 3000 (2m) → 5000 (2m) → 5000 sustain (3m) → 0 (1m)
//
// Run :
//   k6 run tools/loadtest/k6-active-10k.js
// ============================================================================
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE || 'https://dev.lmp-services.ca';

export const options = {
  scenarios: {
    active_10k: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m',  target: 1000 },
        { duration: '2m',  target: 3000 },
        { duration: '2m',  target: 5000 },
        { duration: '3m',  target: 5000 },
        { duration: '1m',  target: 0    },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    http_req_failed:   ['rate<0.20'],          // accept 20% — c'est un test rupture
    http_req_duration: ['p(95)<5000'],
  },
};

const READ_URLS = ['/api/v1/config', '/api/v1/services', '/api/v1/blog', '/'];

function uniqueEmail() {
  return `active-${__VU}-${__ITER}-${Date.now()}@lmp.test`;
}

export default function () {
  const r = Math.random();

  if (r < 0.7) {
    // 70% reads
    const url = READ_URLS[Math.floor(Math.random() * READ_URLS.length)];
    const res = http.get(`${BASE}${url}`, { tags: { name: 'read' } });
    check(res, { 'read 2xx': (x) => x.status >= 200 && x.status < 300 });
  } else {
    // 30% writes — register
    const email = uniqueEmail();
    const password = 'ActiveTest1!';
    const payload = JSON.stringify({
      firstName: 'Active',
      lastName: `VU${__VU}`,
      email,
      password,
      confirmPassword: password,
      phone: '',
      address: '',
      city: '',
      postalCode: '',
      country: '',
      acceptTerms: true,
    });
    const res = http.post(`${BASE}/api/v1/auth/register`, payload, {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'write' },
    });
    check(res, {
      'register 2xx': (x) => x.status >= 200 && x.status < 300,
    });
  }

  // Think-time court (user actif, clique vite)
  sleep(1 + Math.random());
}
