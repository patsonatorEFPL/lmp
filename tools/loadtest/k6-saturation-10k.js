// ============================================================================
// LMP — k6 saturation 10k VUs literal (D)
// ============================================================================
// Goal: find the breaking point of staging backend under 10k concurrent VUs.
// Expected: saturation well below 10k due to 2 vCPU + bcrypt 12 rounds.
// Capture WHERE it breaks (CPU? heap? Hikari pool? socket?).
//
// Stages:
//   0 → 2k  (1m)   warm-up
//   2k → 5k (2m)   first knee
//   5k → 10k (3m)  push past
//   10k sustain (3m)  characterize failure mode
//   10k → 0 (1m)   ramp down
//
// Run from laptop:
//   BASE=https://dev.lmp-services.ca k6 run tools/loadtest/k6-saturation-10k.js
//
// Watch Grafana while it runs:
//   https://grafana.lmp-services.ca/d/spring-boot/spring-boot-2-x-statistics
// ============================================================================
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE = __ENV.BASE || 'https://dev.lmp-services.ca';

const readErrors = new Counter('read_errors');
const writeErrors = new Counter('write_errors');
const writeLatency = new Trend('write_latency', true);
const breakRate = new Rate('break_rate');

export const options = {
  discardResponseBodies: true,
  scenarios: {
    saturation_10k: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 2000 },
        { duration: '2m', target: 5000 },
        { duration: '3m', target: 10000 },
        { duration: '3m', target: 10000 },
        { duration: '1m', target: 0 },
      ],
      gracefulRampDown: '30s',
      gracefulStop: '30s',
    },
  },
  thresholds: {
    // No hard fail — this is a discovery test
    http_req_failed: ['rate<0.99'],
    http_req_duration: ['p(95)<30000'],
  },
};

const READ_URLS = ['/api/v1/config', '/api/v1/services', '/api/v1/blog', '/'];

function uniqueEmail() {
  return `sat-${__VU}-${__ITER}-${Date.now()}@lmp.test`;
}

export default function () {
  const r = Math.random();

  if (r < 0.85) {
    const url = READ_URLS[Math.floor(Math.random() * READ_URLS.length)];
    const res = http.get(`${BASE}${url}`, { tags: { name: 'read' }, timeout: '20s' });
    const ok = res.status >= 200 && res.status < 400;
    check(res, { 'read ok': () => ok });
    breakRate.add(!ok);
    if (!ok) readErrors.add(1);
  } else {
    const email = uniqueEmail();
    const password = 'SatTest1!';
    const payload = JSON.stringify({
      firstName: 'Sat',
      lastName: `VU${__VU}`,
      email,
      password,
      confirmPassword: password,
      acceptTerms: true,
    });
    const res = http.post(`${BASE}/api/v1/auth/register`, payload, {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'write' },
      timeout: '30s',
    });
    const ok = res.status >= 200 && res.status < 300;
    check(res, { 'register ok': () => ok });
    writeLatency.add(res.timings.duration);
    breakRate.add(!ok);
    if (!ok) writeErrors.add(1);
  }

  sleep(1 + Math.random() * 1.5);
}
