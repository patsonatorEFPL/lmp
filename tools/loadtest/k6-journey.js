// ============================================================================
// LMP — k6 user journey load test (browse + write + auth)
// ============================================================================
// Couvre un parcours réaliste: navigation publique, soumission formulaire
// contact (DB write + email), inscription utilisateur (bcrypt + DB insert),
// connexion (bcrypt verify + session), accès page authentifiée.
//
// Trafic généré (par VU iter):
//   - 5 GETs publiques (HTML + API READ + assets)
//   - 1 POST /api/v1/contact (validation + DB INSERT + Mailtrap async)
//   - 1 POST /api/v1/auth/register (bcrypt + DB INSERT user)
//   - 1 POST /api/v1/auth/login (bcrypt verify + session create)
//   - 1 GET /api/v1/auth/me (session check)
//
// Génération users uniques: email = `loadtest-${VU}-${ITER}-${ts}@lmp.test`
// → laisse traces en DB staging (acceptable, non-prod). Cleanup TODO.
//
// Run :
//   k6 run tools/loadtest/k6-journey.js
//   k6 run -e BASE=https://dev.lmp-services.ca tools/loadtest/k6-journey.js
//
// Watch Grafana :
//   - SpringBoot APM (12900) : HTTP rate par URI, HikariCP usage
//   - PostgreSQL (9628) : commits, transactions, locks
//   - JVM (4701) : threads, GC pauses
// ============================================================================
import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { randomString } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const BASE = __ENV.BASE || 'https://dev.lmp-services.ca';

// Le formulaire contact appelle Mailtrap synchroniquement (free tier ~100/h).
// Sous load > 100 req/min il sature et renvoie 500 → fausse les métriques backend.
// SKIP_CONTACT=1 (défaut) désactive l'étape pour mesurer la vraie capacité LMP.
// SKIP_CONTACT=0 réactive — utile pour tester l'intégration email à faible charge.
const SKIP_CONTACT = (__ENV.SKIP_CONTACT ?? '1') !== '0';

export const options = {
  scenarios: {
    journey: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m',  target: 10 },
        { duration: '2m',  target: 30 }, // moins agressif que k6-load car writes coûtent + cher
        { duration: '4m',  target: 30 },
        { duration: '1m',  target: 0  },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    http_req_failed:   ['rate<0.05'],          // tolérance plus haute (login échoue avant register fini)
    http_req_duration: ['p(95)<1500', 'p(99)<3000'],
    'http_req_duration{name:read}':     ['p(95)<500'],
    'http_req_duration{name:write}':    ['p(95)<2000'],   // bcrypt = lent
    'http_req_duration{name:auth}':     ['p(95)<2000'],   // bcrypt = lent
    checks:            ['rate>0.90'],
  },
};

function uniqueEmail() {
  return `loadtest-${__VU}-${__ITER}-${Date.now()}@lmp.test`;
}

export default function () {
  const email = uniqueEmail();
  const password = 'TestPassw0rd!';

  group('public browse', () => {
    const home = http.get(`${BASE}/`, { tags: { name: 'read' } });
    check(home, { 'home 200': (r) => r.status === 200 });

    const config = http.get(`${BASE}/api/v1/config`, { tags: { name: 'read' } });
    check(config, { 'config 200': (r) => r.status === 200 });

    const services = http.get(`${BASE}/api/v1/services`, { tags: { name: 'read' } });
    check(services, { 'services 200': (r) => r.status === 200 });
  });

  if (!SKIP_CONTACT) {
    group('contact form', () => {
      const payload = JSON.stringify({
        name: `LoadTest VU${__VU}`,
        email,
        subject: 'Load test message',
        message: 'Automated k6 load test — please ignore. ' + randomString(50),
        phone: '',
        company: '',
      });
      const res = http.post(`${BASE}/api/v1/contact`, payload, {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'write' },
      });
      check(res, {
        'contact 2xx': (r) => r.status >= 200 && r.status < 300,
      });
    });
  }

  let cookieJar = null;
  group('register', () => {
    const payload = JSON.stringify({
      firstName: 'Load',
      lastName: `Test${__VU}`,
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
      'register 2xx': (r) => r.status >= 200 && r.status < 300,
    });
  });

  group('login', () => {
    const payload = JSON.stringify({
      email,
      password,
      rememberMe: false,
    });
    const res = http.post(`${BASE}/api/v1/auth/login`, payload, {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'auth' },
    });
    check(res, {
      'login 2xx': (r) => r.status >= 200 && r.status < 300,
    });
    // k6 conserve les cookies dans le jar VU par défaut → /me utilisera la session
    cookieJar = http.cookieJar();
  });

  group('authenticated', () => {
    const me = http.get(`${BASE}/api/v1/auth/me`, { tags: { name: 'auth' } });
    check(me, {
      'me 2xx': (r) => r.status >= 200 && r.status < 300,
    });
  });

  // pause user simulée
  sleep(Math.random() * 2 + 1);
}
