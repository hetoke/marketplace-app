
FE:
NextJS, ReactJS, TypeScript

BE:
Java Spring Boot, Maven
Redis
NGINX

DB:
PostgreSQL

Media Storage:
Supabase

DevOps:
Docker
Docker Compose
Github Actions
Hetzner Cloud (CX23) 4.49 euros/month


---

# 🏗️ As-Built Stack

> Added 2026-07-26. Pinned versions verified against `pom.xml`, `package.json`,
> `docker-compose.yml`, `Dockerfile`, `application.yml` and `.github/workflows/ci.yml`.
> The summary above is retained as-is; nothing in it is wrong, it is just unpinned.

## Backend — `marketplace-backend/` (own git repo)

| | |
|---|---|
| Language | **Java 25** (`java.version`) |
| Framework | **Spring Boot 4.1.0** (`spring-boot-starter-parent`) |
| Build | Maven, wrapper committed (`mvnw`, `mvnw.cmd`) |
| Artifact | `com.marketplace:marketplace-app:0.0.1-SNAPSHOT` |
| Architecture | Modular monolith — vertical slices per domain, single deployable |

**Spring starters:** `web`, `data-jpa`, `data-redis`, `validation`, `security`,
`mail`, `flyway`, `configuration-processor`.

**Libraries:**

| Library | Version | Used for |
|---|---|---|
| `io.jsonwebtoken:jjwt` (api/impl/jackson) | 0.12.6 | Access + refresh JWTs |
| `com.bucket4j:bucket4j-core` / `bucket4j-redis` | 8.10.1 | Distributed rate limiting |
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` | 2.8.6 | OpenAPI 3 + Swagger UI |
| `org.flywaydb:flyway-database-postgresql` | managed | Migrations (head **V24**) |
| `org.postgresql:postgresql` | managed | JDBC driver (runtime) |
| `org.projectlombok:lombok` | managed | Optional, excluded from the fat jar |

**Test-scope:** `spring-boot-starter-test` (JUnit 5, Mockito, AssertJ, MockMvc),
`spring-security-test`, `com.h2database:h2`,
`com.github.tomakehurst:wiremock-jre8-standalone` 2.35.2.
26 test classes / **357 test methods** — see `docs/test_specifications.md`.

> ⚠️ Two of these are effectively unused: **WireMock is never imported anywhere in
> `src/test`** (external calls are mocked at the service interface instead — drop the
> dependency or start using it), and **H2 is exercised only by the single
> `@SpringBootTest` context-load test**, since every other class is a pure Mockito unit
> test with no database. `spring-security-test` is likewise unused — controller tests
> use standalone MockMvc with no filter chain, so no security rule is exercised.

## Frontend — `marketplace-frontend/marketplace/` (own git repo)

| | |
|---|---|
| Framework | **Next.js 16.2.9** (App Router) |
| UI | **React 19.2.4** / `react-dom` 19.2.4 |
| Language | **TypeScript 5** |
| Styling | **Tailwind CSS v4** (`@tailwindcss/postcss`), `tw-animate-css` |
| Components | `shadcn` 4.12.0 on **`@base-ui/react` 1.6.0**, `class-variance-authority`, `clsx`, `tailwind-merge` |
| Icons | `lucide-react` 1.21.0 |
| Lint | ESLint 9 + `eslint-config-next` 16.2.9 |

**Pattern:** ~57 route handlers under `app/api/**` act as a BFF proxy
(`lib/api.ts` `backendFetch`, plus `proxy.ts`) so JWTs stay in httpOnly cookies and
never reach client JS. Backend base URL from `.env.local`.

> ⚠️ `AGENTS.md` in that repo warns that this Next.js major has breaking changes vs.
> common training data — consult `node_modules/next/dist/docs/` before editing.

## Data & infrastructure

| | |
|---|---|
| Database | **PostgreSQL 16** (`postgres:16-alpine`) |
| Migrations | Flyway, `classpath:db/migration`, `baseline-on-migrate: true`, `ddl-auto: validate` |
| Cache | **Redis 7** (`redis:7-alpine`) — Spring Cache (`spring.cache.type: redis`) + Bucket4j backing store |
| Reverse proxy | **nginx:alpine**, config at `nginx/nginx.conf`, published `8080:80` |
| Object storage | **Supabase Storage**, bucket `marketplace-images`, direct-to-storage signed uploads + webhook confirmation |
| Payments | **SePay** (sandbox default) — `app.sepay.*` |
| Identity | **Google OIDC** — `app.oidc.google.*`, scopes `openid email profile` |
| Email | SMTP via **Brevo** (`smtp-relay.brevo.com:587`, STARTTLS) |

**Redis cache regions** (`shared/config/CacheConfig.java`, default TTL 10 min, nulls
not cached, `transactionAware`):

| Cache | TTL |
|---|---|
| `products` | 5 min |
| `productById` | 10 min |
| `categoriesById`, `categoriesAll` | 15 min |
| `analyticsRevenue`, `analyticsOrders`, `analyticsUsers`, `analyticsProducts` | 5 min |

**Test profile:** H2 in-memory (`MODE=PostgreSQL`), `ddl-auto: create-drop`, Flyway
**disabled** — so migrations are not exercised by the unit suite.

## DevOps

**`docker-compose.yml`** — 4 services: `nginx` → `app` (built from `Dockerfile`) →
`db` + `redis`, both gated by healthchecks. Named volumes `pgdata`, `redisdata`.
Everything is driven by env vars with defaults; see `.env.example`.

**CI** — `.github/workflows/ci.yml`, on push/PR to `main`, `ubuntu-latest`,
JDK 25 (Temurin) with Maven cache: `mvn compile` → `mvn test` → `mvn package` →
`docker compose build`. Read-only `contents` permission. **No deploy step** — release
to Hetzner is manual.

**Load testing** — k6 (binary vendored at repo root as `k6-v0.56.0-windows-amd64/`)
and Apache JMeter. Plans in `k8/test-plans/`, results in `k8/results/`, write-up in
`k8/PERFORMANCE-REPORT.md`.

**Security testing** — OWASP ZAP: baseline, full and API scans in `zap-reports/`
(config `zap.yaml`).

> Naming note: the `k8/` directory holds **k6/JMeter load tests**, not Kubernetes
> manifests. There is no Kubernetes in this project.

## Required environment variables

From `docker-compose.yml` / `application.yml` (see `.env.example`):

```
DB_HOST DB_PORT DB_NAME DB_USERNAME DB_PASSWORD
REDIS_HOST REDIS_PORT
JWT_ACCESS_SECRET JWT_REFRESH_SECRET          # 256-bit; defaults are placeholders
SUPABASE_PROJECT_URL SUPABASE_PUBLIC_URL SUPABASE_SERVICE_ROLE_KEY SUPABASE_BUCKET_NAME
SEPAY_MERCHANT_ID SEPAY_SECRET_KEY SEPAY_WEBHOOK_SECRET SEPAY_ENVIRONMENT
SEPAY_SUCCESS_URL SEPAY_ERROR_URL SEPAY_CANCEL_URL
GOOGLE_CLIENT_ID GOOGLE_CLIENT_SECRET GOOGLE_REDIRECT_URI
MAIL_HOST MAIL_PORT MAIL_USERNAME MAIL_PASSWORD MAIL_FROM
WEBHOOK_SECRET                                 # Supabase storage webhook HMAC
ADMIN_EMAIL ADMIN_PASSWORD                     # seeded by V11
FRONTEND_URL SPRING_PROFILES_ACTIVE
UPLOAD_MAX_IMAGES_PER_PRODUCT UPLOAD_MAX_FILE_SIZE UPLOAD_ALLOWED_TYPES UPLOAD_SIGNED_URL_EXPIRY_HOURS
```

⚠️ `JWT_ACCESS_SECRET` / `JWT_REFRESH_SECRET`, `WEBHOOK_SECRET` and
`ADMIN_PASSWORD` all have insecure development defaults in `application.yml` and
`docker-compose.yml`. They must be set for any real deployment.

## Deployment gaps vs. the plan above

- **TLS is not in this stack.** nginx serves plain HTTP on `8080:80`; the
  `requirements.md` HTTPS/TLS requirement has to be met at the hosting edge.
- **CI does not deploy.** No workflow targets Hetzner; `docker compose up` on the host
  is the deploy procedure.
- **No monitoring/metrics.** Spring Boot Actuator is not a dependency; "Monitoring
  Basics" from Sprint 9 is unmet beyond application logging.

