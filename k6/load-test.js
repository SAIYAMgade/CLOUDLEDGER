import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 50 },  // Ramp-up to 50 concurrent virtual users
    { duration: '1m', target: 100 },  // High load: 100 concurrent users
    { duration: '30s', target: 0 },   // Ramp-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'], // 95% of requests under 500ms
    http_req_failed: ['rate<0.01'],                  // Error rate < 1%
  },
};

const BASE_URL = __ENV.API_URL || 'http://localhost:8080/api/v1';

export default function () {
  // 1. Authenticate as Finance Operator
  const loginRes = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
    username: 'operator',
    password: 'password123'
  }), {
    headers: { 'Content-Type': 'application/json' },
  });

  check(loginRes, {
    'login status 200': (r) => r.status === 200,
  });

  const token = loginRes.json('data.accessToken');
  const authHeaders = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
    'Idempotency-Key': `k6-${__VU}-${__ITER}-${Date.now()}`
  };

  // 2. Submit high-frequency idempotent transaction transfer
  const txPayload = JSON.stringify({
    sourceAccountId: 'a0000000-0000-0000-0000-000000000002',
    destinationAccountId: 'a0000000-0000-0000-0000-000000000003',
    amount: (Math.random() * 50 + 10).toFixed(4),
    currency: 'USD',
    transactionType: 'TRANSFER',
    description: 'k6 automated ingestion load benchmark'
  });

  const txRes = http.post(`${BASE_URL}/transactions`, txPayload, { headers: authHeaders });

  check(txRes, {
    'tx accepted or posted': (r) => r.status === 200 || r.status === 202,
  });

  // 3. Query trial balance
  const tbRes = http.get(`${BASE_URL}/ledger/trial-balance`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });

  check(tbRes, {
    'trial balance is balanced': (r) => r.status === 200 && r.json('data.balanced') === true,
  });

  sleep(0.1);
}
