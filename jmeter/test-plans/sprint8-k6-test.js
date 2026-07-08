import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

const BASE_URL = 'http://localhost:8080';
const ADMIN_EMAIL = 'admin@marketplace.com';
const ADMIN_PASSWORD = 'Admin123!';

export const options = {
  stages: [
    { duration: '10s', target: 20 },
    { duration: '30s', target: 20 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    errors: ['rate<0.1'],
  },
};

export function setup() {
  const loginRes = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email: ADMIN_EMAIL, password: ADMIN_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(loginRes, {
    'login successful': (r) => r.status === 200,
  });

  const body = JSON.parse(loginRes.body);
  const token = body.data.accessToken;

  return { token };
}

export default function (data) {
  const token = data.token;
  const authHeaders = {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  };

  // Public endpoints
  const productsRes = http.get(`${BASE_URL}/api/v1/products?page=0&size=20`);
  check(productsRes, { 'GET /products 200': (r) => r.status === 200 }) || errorRate.add(1);

  const categoriesRes = http.get(`${BASE_URL}/api/v1/categories`);
  check(categoriesRes, { 'GET /categories 200': (r) => r.status === 200 }) || errorRate.add(1);

  const searchRes = http.get(`${BASE_URL}/api/v1/products?page=0&size=10&query=phone`);
  check(searchRes, { 'GET /products (search) 200': (r) => r.status === 200 }) || errorRate.add(1);

  // Admin endpoints
  const adminUsersRes = http.get(`${BASE_URL}/api/v1/admin/users?page=0&size=20`, authHeaders);
  check(adminUsersRes, { 'GET /admin/users 200': (r) => r.status === 200 }) || errorRate.add(1);

  const adminProductsRes = http.get(`${BASE_URL}/api/v1/admin/products?page=0&size=20`, authHeaders);
  check(adminProductsRes, { 'GET /admin/products 200': (r) => r.status === 200 }) || errorRate.add(1);

  const analyticsRevenueRes = http.get(`${BASE_URL}/api/v1/admin/analytics/revenue`, authHeaders);
  check(analyticsRevenueRes, { 'GET /admin/analytics/revenue 200': (r) => r.status === 200 }) || errorRate.add(1);

  const analyticsOrdersRes = http.get(`${BASE_URL}/api/v1/admin/analytics/orders`, authHeaders);
  check(analyticsOrdersRes, { 'GET /admin/analytics/orders 200': (r) => r.status === 200 }) || errorRate.add(1);

  const analyticsUsersRes = http.get(`${BASE_URL}/api/v1/admin/analytics/users`, authHeaders);
  check(analyticsUsersRes, { 'GET /admin/analytics/users 200': (r) => r.status === 200 }) || errorRate.add(1);

  const analyticsProductsRes = http.get(`${BASE_URL}/api/v1/admin/analytics/products`, authHeaders);
  check(analyticsProductsRes, { 'GET /admin/analytics/products 200': (r) => r.status === 200 }) || errorRate.add(1);

  // Login endpoint
  const loginRes = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email: ADMIN_EMAIL, password: ADMIN_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(loginRes, { 'POST /auth/login 200': (r) => r.status === 200 }) || errorRate.add(1);

  sleep(0.1);
}
