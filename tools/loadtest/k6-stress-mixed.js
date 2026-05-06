// ============================================================================
// LMP — k6 stress test (mixed read + write)
// ============================================================================
// 1000 VUs cible, ramp progressif. 70% reads + 30% writes par VU iter.
// Le but: trouver le point de rupture sous trafic réaliste, pas read-only.
//
// ATTENTION: 1000 VUs avec writes = beaucoup d'utilisateurs créés en DB
// staging (cleanup TODO). bcrypt à 12 rounds saturera CPU bien avant 1000 VUs.
//
// Profile :
//   0 → 200 (1m) → 600 (2m) → 1000 (2m) → 1000 sustain (3m) → 0 (1m)
//
// Run :
//   k6 run tools/loadtest/k6-stress-mixed.js
// ============================================================================
import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.BASE || 'https://dev.lmp-services.ca';

export const options = {
  scenarios: {
    stress_mixed: {
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
    // Sous stress on accepte plus d'erreurs (objectif = trouver rupture)
    http_req_failed:   ['rate<0.20'],
    http_req_duration: ['p(95)<5000'],
  },
};

function uniqueEmail() {
  return `stress-${__VU}-${__ITER}-${Date.now()}@lmp.test`;
}

export default function () {
  const r = Math.random();

  if (r < 0.7) {
    // 70% lectures
    const reads = [
      `${BASE}/api/v1/config`,
      `${BASE}/api/v1/services`,
      `${BASE}/`,
    ];
    const url = reads[Math.floor(Math.random() * reads.length)];
    const res = http.get(url, { tags: { name: 'read' } });
    check(res, { 'read 2xx/3xx': (x) => x.status >= 200 && x.status < 400 });
  } else {
    // 30% writes — register seulement (contact = Mailtrap rate limit)
    const email = uniqueEmail();
    const password = 'StressTest1!';
    const payload = JSON.stringify({
      firstName: 'Stress',
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
}
