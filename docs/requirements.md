# Marketplace Web App Requirements

## 🔐 Authentication & Authorization

### MFA Requirements
- Two-Factor Authentication (2FA) mandatory for seller accounts and optional for buyer accounts.
- Support verification methods such as Email One-Time Password (OTP)
- Backup recovery codes for account recovery when MFA devices are unavailable.
- Security notifications for login attempts from new devices or locations.

### OIDC & OAuth2 Integration
- Support Single Sign-On (SSO) using external identity providers such as Google.
- Allow users to authenticate using OpenID Connect (OIDC) in addition to traditional email/password login.
- Store and synchronize basic user profile information from identity providers, including email, display name, and profile picture.
- Use JWT tokens for authenticated sessions and API access.
- Support secure user session management, including login, logout, and session expiration.
- Encrypt and securely store sensitive authentication data.
- Maintain compatibility with OAuth 2.0 and OIDC standards for future third-party integrations.


## 🖥️ User Interfaces

### Seller Interface
**Dashboard Features:**
- Product inventory management (CRUD operations)
- Order management and status updates
- Sales analytics and revenue tracking
- Customer communication tools

**Product Management:**
- Add/edit/remove products with rich media support
- Inventory tracking and low-stock alerts
- Pricing management
- Category and tag organization

### Buyer Interface
**Marketplace Features:**
- Advanced search and filtering capabilities
- Wishlist functionality
- Product reviews and ratings

**Shopping Experience:**
- Persistent shopping cart across sessions
- Automatic inventory synchronization after orders
- Multiple item quantity selection

**Order Management:**
- Order status tracking (PENDING → CONFIRMED → DELIVERY → COMPLETE)
- Order history with detailed information
- Reorder functionality
- Order cancellation within time windows
- Return/refund request system

## 💰 Payment & Financial

### Payment Requirements
- **Multiple payment methods**: Credit Card (mock), VNPay Sandbox
- Payment transaction history
- Refund request management

### Financial Tracking
- Transaction history for buyers and sellers.
- Payment status tracking (Pending, Paid, Refunded).
- Revenue and sales summaries for sellers.
- Basic refund request and refund tracking.
- Monthly sales reports and transaction statistics.


## 🔔 Notifications & Communication

### Notification Types

- Order status updates (Order Confirmed, Shipped, Delivered, Cancelled)
- Payment confirmation notifications
- Inventory low-stock alerts for sellers
- Security notifications (login attempts, password changes)
- Promotional announcements and discounts

### Communication Channels

- In-app notifications
- Email notifications
- Basic messaging between buyers and sellers


## ⚙️ Technical Requirements

### Performance Requirements

- Page load times should not exceed 5 seconds under normal operating conditions.
- API requests should return results within 2 seconds for typical operations.
- The system should support at least 100 concurrent users during testing.
- Product searches should return results within 3 seconds.
- Database operations should be optimized using indexing where appropriate.


### Security Requirements
- Passwords must be securely stored using hashing algorithms.
- Input validation and sanitization to prevent common attacks.
- Rate limiting on authentication and API endpoints to reduce abuse.
- Secure communication using HTTPS/TLS.
- Access control based on user roles (Buyer, Seller, Admin).
- Logging of important security events such as login attempts and password changes.


### Monitoring & Analytics

- Dashboard showing total users, products, orders, and revenue.
- Sales analytics for sellers, including revenue and order statistics.
- Order and transaction reports for administrators.
- Basic system logging for important events and errors.
- Product performance statistics, such as views, purchases, and ratings.

## 📱 Additional Features

### Search & Discovery
- **Advanced filtering** by price, category, ratings
- **Faceted search** with auto-complete
- **Trending products** and popular categories


## 📈 Future Extensibility

### Planned Features
- TOTP
- Support Ticketing
- Event sourcing
- **Subscription services** and recurring payments
- **Multi-vendor marketplace** capabilities
- **AI-powered recommendations** and chatbots (RAG)

---

# ✅ Implementation Status

> Added 2026-07-26. Verified against the codebase. Every requirement above is
> retained as written; this section records which of them shipped, which shipped
> differently, and which are still open. Treat the list above as *intent* and this
> section as *status*.

## S1. Shipped as specified

- Registration, login, logout, JWT sessions, refresh tokens, session expiration
- Email verification (+ resend), forgot/reset password, change password
- Password hashing (BCrypt), role-based access control (`BUYER`/`SELLER`/`ADMIN`)
- Email OTP MFA with hashed, single-use backup recovery codes
- Google SSO via OIDC; profile email / display name / picture synced from the IdP
- Seller product CRUD with images, pricing, stock, category
- Buyer search + filtering by price and category, wishlist, reviews and ratings
- Persistent cart across sessions; multi-quantity selection; stock decremented on order
- Order history and detail, status tracking, cancellation, return/refund requests
- Payment transaction history (buyer and seller), refund request → admin approval
- Payment status tracking; in-app + email notifications for order, payment,
  security and low-stock events; notification preferences
- Admin dashboard totals: users, products, orders, revenue
- Rate limiting on auth and mutating API endpoints (Bucket4j + Redis)
- Input validation (Bean Validation) and centralised error handling
- Redis caching and DB indexing for query performance
- Unit + controller tests (26 test classes), Docker/Compose deploy, GitHub Actions CI

## S2. Shipped, but differently than written

| Requirement | As built |
|---|---|
| Payment methods: "Credit Card (mock), VNPay Sandbox" (line 57) | **SePay** sandbox + a mock credit-card path. `PaymentMethod { CREDIT_CARD, SEPAY }`. VNPay was never integrated; PayPal (named in `use_case_specifications.md`) was never integrated either. |
| Order status `PENDING → CONFIRMED → DELIVERY → COMPLETE` (line 48) | `PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED, RETURN_REQUESTED, RETURNED`. `DELIVERY` and `COMPLETE` do not exist. |
| "MFA mandatory for seller accounts" (line 6) | MFA is **optional for all roles**. Nothing enforces it for sellers. |
| "Order cancellation within time windows" (line 51) | Eligibility is **status-based only** — `PENDING` or `CONFIRMED`, and refused if already paid. No elapsed-time window. |
| "Return/refund request system" within a return window (line 52) | Return requests work; there is **no** enforced return window. |
| "Inventory tracking and low-stock alerts" (line 32) | Stock is a single integer on `products`; low-stock fires a `LOW_STOCK` notification to the seller at order time. No `Inventory` entity, no reserved/available split. |
| "System reserves inventory for cart items" (UC-008) | **Not implemented.** Stock is decremented at order placement, not reserved at add-to-cart. |
| "Encrypt and securely store sensitive authentication data" (line 17) | MFA codes and recovery codes are **hashed** (not reversibly encrypted); OIDC client secrets live in env vars, not the DB. |
| "Secure communication using HTTPS/TLS" (line 101) | Not terminated by this stack — `docker-compose.yml` publishes plain HTTP (`8080:80`) via nginx. TLS is expected at the hosting edge. |
| "Product performance statistics, such as views, purchases, and ratings" (line 112) | Purchases (`sold_count`) and ratings (`average_rating`, `review_count`) exist. **View tracking does not.** |
| Rate limits "1000 req/hour per IP, 100/min per user, 10/min auth" | Actual: per-route-group token buckets — auth 10/min, orders 20/min, payments 10/min, reviews 10/min, uploads 5/min. No global hourly quota. |

## S3. Not implemented

| Requirement | Notes |
|---|---|
| **Sales analytics / revenue tracking for sellers** (lines 27, 64, 109) | All four analytics endpoints are `hasRole('ADMIN')`. A seller's only reporting surface is `GET /api/v1/payments/seller/history`. This is the largest single gap against the Seller Interface section. |
| **Customer communication tools / buyer↔seller messaging** (lines 28, 83) | No messaging entity, endpoint or UI. |
| **Faceted search with auto-complete** (line 118) | Search is a flat filtered query. No facets, no suggest endpoint. |
| **Trending products / popular categories** (line 119) | Not implemented. `sold_count` exists and would support it, but nothing sorts or surfaces by it. |
| **Filter by ratings** (line 117) | `ProductSearchRequest` accepts no rating filter, and the `sortBy` allowlist has no rating or popularity option. |
| **Reorder functionality** (line 50) | Not implemented. |
| **Product tags / tag organization** (lines 35, 108) | No `tags` column, no tag search. |
| **SEO metadata** (UC-005 step 5) | Only `slug`; no meta title/description. |
| **Monthly sales reports** (line 66) | Analytics are point-in-time totals, not periodised monthly reports. |
| **Security notifications for logins from new devices or locations** (lines 9, 76) | `SECURITY_ALERT` exists and fires on password changes, but there is **no device or location tracking** — no `trusted_devices` table, no fingerprinting. |
| **Support Ticketing** | Correctly listed under Future Extensibility here — but note that `modules.md` §8, `use_case_specifications.md` UC-019, `api_specifications.md` §8 and `data_model.md` §16 all describe it as in-scope. It is **not** built; those four docs are the stale ones. |
| **Guest checkout** (`modules.md`, `api_specifications.md`) | Not implemented. Cart requires an authenticated `BUYER`. |
| TOTP, event sourcing, subscriptions, RAG recommendations | Still future, as documented. |

## S4. Performance requirements — measured

Load and security testing artefacts live outside `docs/`:

- `k8/test-plans/sprint8-k6-test.js` — k6 script; `k8/test-plans/sprint8-performance-test.jmx` — JMeter plan
- `k8/results/k6-baseline.json`, `k8/results/test-results.jtl`,
  `k8/results/test-results-post-redis.jtl` — before/after the Redis cache
- `k8/PERFORMANCE-REPORT.md` — write-up against the 100-concurrent-user target
- `zap-reports/` — OWASP ZAP baseline, full and API scans (`report.html`,
  `full-report.html`, `api-report.html`, `zap.yaml`)

Consult `k8/PERFORMANCE-REPORT.md` for whether the 2s API / 3s search / 5s page-load
targets on line 90–93 were met; this document does not restate its numbers.

## S5. Scope added that no requirement asked for

- **Direct-to-Supabase image upload** — signed upload URLs, `upload_sessions`
  two-phase commit, storage webhook confirmation, per-product image limits
- **Per-product discount campaigns** — `PERCENT`/`FIXED` with an optional active
  window, applied through cart and order totals (`docs/discount_campaign_plan.md`)
- **Admin audit log** — `admin_action_log` records user and product status changes
- **Admin user suspension** — `UserStatus { ACTIVE, SUSPENDED, DEACTIVATED }`
- **Optimistic locking** on products, carts, orders, payments, reviews
- **Invoice numbers** on payments; **cart expiration sweeper**
- **Vietnam-market localisation** — VND currency, province/district/ward addresses
