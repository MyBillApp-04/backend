import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 25 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const jwt = __ENV.JWT;

export default function () {
  const response = http.get(`${baseUrl}/api/dashboard/summary`, {
    headers: {
      Authorization: `Bearer ${jwt}`,
      Accept: 'application/json',
    },
  });

  check(response, {
    'status is 200': (r) => r.status === 200,
    'has JSON body': (r) => r.headers['Content-Type']?.includes('application/json'),
  });

  sleep(1);
}
