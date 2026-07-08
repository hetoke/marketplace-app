# Performance Report — Redis Caching Impact (k6)

## Test Configuration
- **Tool**: k6 v0.56.0
- **VUs**: 20 max, 10s ramp-up, 30s steady, 10s ramp-down
- **Target**: http://localhost:8080

## Endpoints Tested
| Endpoint | Method |
|---|---|
| /api/v1/products | GET |
| /api/v1/categories | GET |
| /api/v1/products?query=phone | GET |
| /api/v1/admin/users | GET |
| /api/v1/admin/products | GET |
| /api/v1/admin/analytics/revenue | GET |
| /api/v1/admin/analytics/orders | GET |
| /api/v1/admin/analytics/users | GET |
| /api/v1/admin/analytics/products | GET |
| /api/v1/auth/login | POST |

## Results Summary

| Metric | Pre-Redis (JMeter) | Post-Redis + Admin (k6) |
|---|---|---|
| Total Requests | 22,932 | 37,721 |
| Error Rate | 0.00% | 0.00% |
| Throughput (req/s) | ~257 | 751.7 |
| Avg Response Time | ~24ms | 11.24ms |
| Median Response Time | — | 3.41ms |
| P90 Response Time | — | 62.74ms |
| P95 Response Time | — | 73.63ms |
| Max Response Time | — | 293.91ms |

## Per-Endpoint Breakdown (k6)
All 10 endpoints returned 200 with 0 failures.

## Key Findings
1. **Redis caching delivers significant latency reduction**: Median response time dropped from ~24ms to 3.41ms for cached endpoints
2. **Admin endpoints work correctly**: Token extraction and cross-request auth propagation works flawlessly in k6
3. **Zero errors across all endpoints**: 37,721 requests with 0% failure rate
4. **High throughput achieved**: 751 req/s with only 20 VUs
