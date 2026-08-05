# 🏗️ Sprint Plan (Reformatted Based on Docs)

## Sprint 0 — Foundation (1 week)

### Goal
Project can run locally and deploy.

### Deliverables

#### Backend
- Spring Boot setup
- PostgreSQL setup
- Flyway/Liquibase migrations
- Global exception handling
- OpenAPI/Swagger

#### DevOps
- Docker
- Docker Compose
- GitHub Repository
- GitHub Actions CI

#### Architecture
Modular Monolith structure, based on modules.md:


### Definition of Done
```bash
docker compose up
```
starts application and database.

---

## Sprint 1 — Authentication & Users (2 weeks)

### Goal
Users can register and log in.

### Features

#### User Management
- Registration
- Login
- Logout
- JWT Authentication
- Refresh Tokens

#### Profile
- View Profile
- Update Profile

#### Security
- Password hashing
- Email verification
- Password reset

### Entities
- `User`
- `Role`
- `RefreshToken`
- `VerificationToken`

### API Endpoints
```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout
POST   /api/v1/auth/forgot-password
POST   /api/v1/auth/reset-password
POST   /api/v1/auth/verify-email
GET    /api/v1/users/profile
PUT    /api/v1/users/profile
PUT    /api/v1/users/profile/password
```

### Use Cases
- UC-001: User Registration

### Demo
```text
Register
Verify Email
Login
Access Protected Endpoint
```

---

## Sprint 2 — Product Catalog (2 weeks)

### Goal
Seller can manage products.

### Features

#### Categories
- Create Category
- Update Category
- View Categories

#### Products
- Create Product
- Update Product
- Delete Product
- Product Details

#### Search
- Search by name
- Filter by category
- Filter by price

### Entities
- `Category`
- `Product`
- `ProductImage`

### API Endpoints
```
POST   /api/v1/products
GET    /api/v1/products
GET    /api/v1/products/{productId}
PUT    /api/v1/products/{productId}
DELETE /api/v1/products/{productId}
GET    /api/v1/categories
POST   /api/v1/categories
GET    /api/v1/categories/{categoryId}
PUT    /api/v1/categories/{categoryId}
DELETE /api/v1/categories/{categoryId}
```

### Use Cases
- UC-005: Create Product
- UC-006: Search Products with Advanced Filtering

### Demo
```text
Seller creates product
Buyer browses products
```

---

## Sprint 3 — Cart & Wishlist (2 weeks)

### Goal
Buyer can prepare purchases.

### Features

#### Cart
- Add to cart
- Remove from cart
- Update quantity

#### Wishlist
- Add wishlist item
- Remove wishlist item

### Entities
- `Cart`
- `CartItem`
- `Wishlist`
- `WishlistItem`

### API Endpoints
```
GET    /api/v1/cart
POST   /api/v1/cart/items
PUT    /api/v1/cart/items/{itemId}
DELETE /api/v1/cart/items/{itemId}
DELETE /api/v1/cart
POST   /api/v1/buyers/wishlist/{productId}
DELETE /api/v1/buyers/wishlist/{productId}
GET    /api/v1/buyers/wishlist
```

### Use Cases
- UC-008: Add Item to Cart with Quantity Selection
- UC-009: Manage Wishlist

### Demo
```text
Browse Product
Add To Cart
Add To Wishlist
```

---

## Sprint 4 — Orders & Checkout (2 weeks)

### Goal
Complete purchasing flow.

### Features

#### Checkout
- Create order
- Address management

#### Orders
- Order history
- Order details
- Order status tracking

#### Inventory
- Automatic stock deduction

### Entities
- `Order`
- `OrderItem`
- `Address`
- `Inventory`

### API Endpoints
```
POST   /api/v1/orders
GET    /api/v1/orders
GET    /api/v1/orders/{orderId}
PUT    /api/v1/orders/{orderId}/status
POST   /api/v1/orders/{orderId}/cancel
POST   /api/v1/orders/{orderId}/return
GET    /api/v1/orders/{orderId}/items
```

### Use Cases
- UC-010: Place Order
- UC-011: Track Order Status
- UC-012: Cancel Order with Time Window
- UC-013: Request Return/Refund

### Demo
```text
Add To Cart
Checkout
View Order
```

---

## Sprint 5 — Payments & Refunds (2 weeks)

### Goal
Handle payment lifecycle.

### Features

#### Payments
- SEPay Sandbox
- VNPay Sandbox (optional)

#### Refunds
- Request refund
- Approve refund

#### Transaction History
- Payment records
- Seller revenue tracking

### Entities
- `Payment`
- `Transaction`
- `Refund`

### API Endpoints
```
POST   /api/v1/payments
GET    /api/v1/payments/{paymentId}
POST   /api/v1/payments/{paymentId}/refund
GET    /api/v1/payments/methods
GET    /api/v1/payments/history
```

### Use Cases
- UC-014: Process Payment with Multiple Methods
- UC-015: View Payment History

### Demo
```text
Checkout
Pay
Refund
```

---

## Sprint 6 — Reviews & Notifications (2 weeks)

### Goal
Marketplace interaction.

### Features

#### Reviews
- Product ratings
- Product reviews

#### Notifications
- In-app notifications
- Email notifications

Types:
```
Order Updates
Payment Updates
Low Stock Alerts
Security Alerts
```

### Entities
- `Review`
- `Notification`

### API Endpoints
```
POST   /api/v1/reviews
GET    /api/v1/reviews
GET    /api/v1/reviews/{reviewId}
PUT    /api/v1/reviews/{reviewId}
DELETE /api/v1/reviews/{reviewId}
GET    /api/v1/products/{productId}/reviews
GET    /api/v1/notifications
PUT    /api/v1/notifications/{notificationId}/read
DELETE /api/v1/notifications/{notificationId}
PUT    /api/v1/notifications/settings
DELETE /api/v1/notifications
```

### Use Cases
- UC-007: Product Review Submission
- UC-016: Receive Order Status Notifications
- UC-017: Receive Security Notifications
- UC-018: Submit Product Review with Verification

### Demo
```text
Purchase Product
Leave Review
Receive Notification
```

---

## Sprint 7 — Google Login & MFA (2 weeks)

### Goal
Advanced authentication.

### Features

#### Google SSO
- Google OAuth Login

#### MFA
- Email OTP
- Recovery Codes

#### Security
- Login alerts
- Device tracking

### Entities
- `IdentityProvider`
- `UserIdentity`
- `MFAChallenge`
- `RecoveryCode`

### API Endpoints
```
GET    /api/v1/auth/providers
POST   /api/v1/auth/oidc/login
GET    /api/v1/auth/oidc/callback
POST   /api/v1/users/mfa/setup
POST   /api/v1/users/mfa/verify
DELETE /api/v1/users/mfa
POST   /api/v1/users/mfa/recovery
GET    /api/v1/users/mfa/status
```

### Use Cases
- UC-002: User Login with OIDC
- UC-003: Multi-Factor Authentication Setup

### Demo
```text
Login With Google
Enable MFA
Verify OTP
```

---

## Sprint 8 — Admin Dashboard & Analytics (2 weeks)

### Goal
Administration features.

### Features

#### Admin
- User Management
- Product Moderation

#### Analytics
- Revenue Statistics
- Orders Statistics
- User Statistics
- Product Statistics

### Entities
- `AnalyticsReport`
- `AdminActionLog`

### API Endpoints
```
GET    /api/v1/admin/users
PUT    /api/v1/admin/users/{userId}/status
GET    /api/v1/admin/products
PUT    /api/v1/admin/products/{productId}/status
GET    /api/v1/admin/analytics/revenue
GET    /api/v1/admin/analytics/orders
GET    /api/v1/admin/analytics/users
GET    /api/v1/admin/analytics/products
```

### Demo
```text
Admin Dashboard
Reports
```

---

## Sprint 9 — Hardening & Deployment (2 weeks)

### Goal
Production-ready portfolio project.

### Testing
- Unit Tests
- Integration Tests

### Security
- Rate Limiting
- Input Validation
- Audit Logging

### Performance
- Redis Cache
- Query Optimization

### Deployment
- Docker Deployment
- Production Environment
- Monitoring Basics

### Documentation
- Architecture Diagram
- ERD
- API Documentation
- README

### Demo
Public URL + GitHub Repository.

---

# 🏗️ As-Built Sprint Outcomes

> Added 2026-07-26. Verified against the codebase. The plan above is retained as
> written; this section records what each sprint actually delivered.
> Of the ten docs, this one held up best — the sprint sequence matches the migration
> history almost exactly (`V1`→`V24`).

## E1. Sprint-by-sprint

| Sprint | Outcome |
|---|---|
| **0 — Foundation** | ✅ Delivered. Spring Boot 4.1.0 / Java 25, PostgreSQL 16, **Flyway** (Liquibase not used), `GlobalExceptionHandler`, springdoc/Swagger, Docker + Compose, GitHub Actions CI. DoD met: `docker compose up` starts nginx + app + db + redis. Modular monolith built, but as **vertical slices per domain**, not the `api/controller/` layout in `modules.md` — see that doc's §D1. |
| **1 — Auth & Users** | ✅ Delivered (`V1`, `V2`). All 9 planned endpoints exist. `Role` is an enum on `User`, not a separate entity. Added beyond plan: `POST /api/v1/auth/resend-verification`. |
| **2 — Product Catalog** | ✅ Delivered (`V3`, `V4`). All 10 endpoints exist. Search covers name, category and price as planned. Added: `slug`, `GET /api/v1/seller/products`. |
| **3 — Cart & Wishlist** | ⚠️ Delivered with two changes (`V5`). `POST /api/v1/cart/items` is actually **`POST /api/v1/cart/items/{productId}`**. `Wishlist`/`WishlistItem` collapsed to a single `wishlist_items` table. |
| **4 — Orders & Checkout** | ⚠️ Delivered (`V6`). `Address` became four `default_*` columns on `users` (`V16`) plus a flattened `orders.shipping_address` string; `Inventory` collapsed to `products.stock`. **`GET /api/v1/orders/{orderId}/items` was never implemented** — items come back inline on the order. Added: `AdminOrderController`. |
| **5 — Payments & Refunds** | ⚠️ Delivered on **SePay only** (`V8`, `V12`, `V14`); VNPay stayed optional and was skipped. `POST /api/v1/payments` became **`POST /api/v1/orders/{orderId}/pay`**; `GET /api/v1/payments/methods` was never built. `Transaction` and `Refund` collapsed into `payments`. Added: IPN + callback endpoints, admin refund approval, seller payment history, invoice numbers. |
| **6 — Reviews & Notifications** | ✅ Delivered (`V17`, `V18`, `V19`). All 4 notification types shipped, plus `PROMOTIONAL`. Added: `/notifications/all`, `/unread-count`, `/read-all`, and the `average_rating`/`review_count` rollup onto `products`. |
| **7 — Google Login & MFA** | ⚠️ Mostly delivered (`V20`, `V21`). Google OIDC, email OTP, recovery codes, `UserIdentity`, `MFAChallenge`, `RecoveryCode` all shipped. **Three gaps:** `GET /api/v1/auth/providers` not built (Google is static config, so `IdentityProvider` has no table); **"Device tracking" not built at all** — no device or location record anywhere; login alerts cover password/MFA changes only. `/users/mfa/recovery` actually lives at `/auth/mfa/recovery`, and `POST /api/v1/auth/oidc/login` is a **`GET`**. Added: `POST /api/v1/auth/oidc/token`, `POST /api/v1/auth/mfa/verify`, `POST /api/v1/users/mfa/disable/send-otp`. |
| **8 — Admin & Analytics** | ⚠️ Delivered (`V22`). All 8 endpoints exist and match the planned paths. `AnalyticsReport` was **not** persisted — analytics compute on read and cache in Redis for 5 min. `AdminActionLog` shipped. **Sellers got no analytics** — all four endpoints are `hasRole('ADMIN')`, leaving the "Sales analytics and revenue tracking" item in the Seller Dashboard requirements unmet. |
| **9 — Hardening & Deployment** | ⚠️ Partial — see §E2. |

## E2. Sprint 9 detail

| Item | Status |
|---|---|
| Unit tests | ✅ 26 test classes, **357 test methods** — full catalogue in `docs/test_specifications.md` |
| Integration tests | ❌ **None, despite the plan.** All 26 classes are pure Mockito unit tests (`@ExtendWith(MockitoExtension.class)`); controller tests use *standalone* MockMvc, so no Spring context, no filter chain, no DB. The one `@SpringBootTest` is a context-load smoke test. Every repository is mocked — including `ImageRepositoryTest`, which mocks the repository it names. Because the `test` profile disables Flyway, **`mvn test` never runs a migration**. No Testcontainers, no test against real Postgres or Redis, and WireMock (declared) is never used. |
| Security tests | ❌ **No test verifies that any endpoint is protected.** With no filter chain and no method-security interceptor, `@PreAuthorize` and `PermissionService` are inert during tests; the `returns403` cases assert `GlobalExceptionHandler` mapping, not access control. Ownership *is* covered at the service layer (14 IDOR-class cases). See `test_specifications.md` §11.3. |
| Rate limiting | ✅ Bucket4j over Redis, five per-route buckets — but see the dead-regex note in `api_specifications.md` §A15 |
| Input validation | ✅ Bean Validation throughout, sort fields allowlisted, page size clamped |
| Audit logging | ✅ `admin_action_log` |
| Redis cache | ✅ 8 cache regions, 5–15 min TTLs |
| Query optimization | ✅ Indexes in migrations; denormalised counters (`sold_count`, `review_count`, `average_rating`); optimistic locking |
| Docker deployment | ✅ Compose stack with healthchecks |
| Production environment | ⚠️ **CI does not deploy** — no Hetzner workflow; deploy is a manual `docker compose up`. nginx serves **plain HTTP** (`8080:80`), so the HTTPS/TLS requirement is unmet inside this stack. |
| Monitoring basics | ❌ **Not delivered.** Actuator is not a dependency; no metrics, health endpoint or dashboards beyond application logs. |
| Architecture diagram | ⚠️ 14 Mermaid diagrams in `docs/diagrams/` cover sequences and flows, but there is **no C4/component architecture diagram** |
| ERD | ❌ Not produced as a diagram — the schema is documented in `data_model.md` §B (added 2026-07-26) |
| API documentation | ✅ Live OpenAPI at `/v3/api-docs` + Swagger UI; `api_specifications.md` §A added 2026-07-26 |
| Test documentation | ✅ `docs/test_specifications.md` added 2026-07-26 — 357 cases catalogued with traceability to use cases. **No coverage tooling** (JaCoCo absent), so no measured coverage figure exists. |
| README | ⚠️ Backend has only the Spring Initializr `HELP.md`; the frontend README is the unmodified `create-next-app` boilerplate. **A real project README does not exist.** |

## E3. Unplanned work that shipped

Not in any sprint above, but built and in production:

| Feature | Migrations | Notes |
|---|---|---|
| **Media upload pipeline** | `V4`, `V7`, `V10` | Direct-to-Supabase signed uploads, `upload_sessions` two-phase commit, storage webhook, avatar + product images. The single largest unplanned module. |
| **Per-product discount campaigns** | `V23` | `PERCENT`/`FIXED` with active window; `DiscountService`, `discount_amount`/`original_amount` on cart, order and order items. See `discount_campaign_plan.md`. |
| **Sold-count tracking** | `V24` | `products.sold_count`, bumped at order placement |
| **User suspension** | — | `UserStatus { ACTIVE, SUSPENDED, DEACTIVATED }`, enforced at login |
| **Optimistic locking** | `V9`, `V15` | products, carts, orders, payments, reviews |
| **Cart expiration sweeper** | — | `CartExpirationService` |
| **VN localisation** | `V13`, `V16` | VND currency default; province/district/ward addresses |
| **Load + security testing** | — | k6 + JMeter (`k8/`), OWASP ZAP (`zap-reports/`) — beyond Sprint 9's scope |

## E4. Suggested Sprint 10 backlog

Derived from the gaps above and the other as-built appendices, roughly by value:

1. **Seller analytics dashboard** — the biggest requirement gap. Needs seller-scoped
   revenue/order/product endpoints (currently ADMIN-only) plus a `app/seller` page.
2. **Fix `GET /api/v1/orders/{orderId}/items`** — the frontend proxy at
   `app/api/orders/[orderId]/items/route.ts` calls a backend endpoint that doesn't
   exist. Implement it or delete the proxy.
3. **Project README + architecture/ERD diagrams** — Sprint 9 deliverables still open;
   both repo READMEs are still generator boilerplate.
4. **Monitoring** — add Actuator, expose health/metrics, wire a healthcheck for the
   `app` service in Compose.
5. **TLS + secret hygiene** — terminate HTTPS; remove the insecure defaults for
   `JWT_*_SECRET`, `WEBHOOK_SECRET`, `ADMIN_PASSWORD`.
6. **Seller order management** — sellers currently cannot change order status at all.
7. **Fix the dead review rate-limit regex** (`RateLimitFilter` ~line 126: `\d+` vs UUID).
8. **Integration tests against real Postgres** (Testcontainers) so Flyway runs in CI;
   add a CD job.
9. **Search depth** — rating filter, popularity sort, auto-complete, trending
   (`sold_count` already supports the last two).
10. **Enforce mandatory seller MFA**, an order-status timeline, and cancellation /
    return windows — three specified behaviours that were never enforced.
11. **Product variants** — `docs/item_variants_plan.md` (renumber its migrations to
    `V25+`; `V23`/`V24` are taken).

