# Performance Report — Redis Caching Impact (k6)

## Test Configuration
- **Tool**: k6 v0.56.0
- **VUs**: 20 max, 10s ramp-up, 30s steady, 10s ramp-down
- **Target**: http://localhost:8080
- **Method**: Git checkout to pre-Redis commit (`1c55518`) for baseline, restore to `ad6ded8` for post-Redis

## Endpoints Tested (10 total)
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

## Results: Before vs After Redis Caching

| Metric | Pre-Redis (baseline) | Post-Redis | Improvement |
|---|---|---|---|
| Total Requests | 35,321 | 37,721 | +6.8% |
| Throughput (req/s) | 699.4 | 751.7 | **+7.5%** |
| Avg Response Time | 12.71ms | 11.24ms | **-11.6%** |
| Median Response Time | 4.35ms | 3.41ms | **-21.6%** |
| P90 Response Time | 62.86ms | 62.74ms | -0.2% |
| P95 Response Time | 77.28ms | 73.63ms | **-4.7%** |
| Max Response Time | 433.34ms | 293.91ms | **-32.2%** |
| Error Rate | 0.00% | 0.00% | — |

## Key Findings
1. **Median latency improved 22%** — typical requests are significantly faster with Redis caching
2. **Max latency reduced 32%** — cache hits eliminate the slowest database queries
3. **Throughput increased 7.5%** — Redis offloading reduces database load
4. **Zero errors in both runs** — caching does not introduce regressions
5. **P90/P95 improvement is modest** — cache misses still hit the database at tail latencies

## Caches Implemented
- `products` (5min TTL) — Product search results
- `productById` (10min TTL) — Individual product lookups
- `categoriesById` (15min TTL) — Individual category lookups
- `categoriesAll` (15min TTL) — Full category list
- `analyticsRevenue` (5min TTL) — Revenue analytics
- `analyticsOrders` (5min TTL) — Order analytics
- `analyticsUsers` (5min TTL) — User analytics
- `analyticsProducts` (5min TTL) — Product analytics
