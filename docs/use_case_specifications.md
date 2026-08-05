# 🎯 Use Case Specifications

## 👤 1. User Management Use Cases

### UC-001: User Registration
**Actor:** Guest User  
**Preconditions:** None  
**Main Flow:**
1. User visits registration page
2. User enters email, password, and user type (buyer/seller)
3. System validates input data
4. System checks if email already exists
5. System creates user account with pending verification
6. System sends verification email
7. System returns success message

**Alternative Flows:**
- Email already exists → Show error
- Invalid input → Show validation errors

---

### UC-002: User Login with OIDC
**Actor:** Registered User  
**Preconditions:** User account exists  
**Main Flow:**
1. User selects login method (email/password or SSO)
2. If SSO: User selects identity provider
3. System redirects to IdP for authentication
4. User authenticates with IdP
5. IdP returns user claims
6. System creates/syncs user profile from IdP data
7. System generates JWT token
8. System returns user info and token

**Alternative Flows:**
- Local login: User enters email/password → System validates credentials
- If MFA enabled, system prompts for MFA code
- Invalid credentials → Show error
- Account not verified → Redirect to verification
- MFA code invalid → Retry MFA

---

### UC-003: Multi-Factor Authentication Setup
**Actor:** Registered User  
**Preconditions:** User logged in  
**Main Flow:**
1. User navigates to security settings
2. User selects MFA method (Email OTP - TOTP for future)
3. System generates/setup MFA credentials
4. User confirms MFA setup with test code
5. System enables MFA for account
6. System generates backup recovery codes
7. System stores encrypted recovery codes

**Postconditions:** MFA enabled for user account

**Notes:** Mandatory for sellers, optional for buyers

---

### UC-004: Profile Management
**Actor:** Registered User  
**Preconditions:** User logged in  
**Main Flow:**
1. User navigates to profile page
2. User updates profile information
3. System validates and saves changes
4. If using IdP: System syncs with external provider
5. System returns updated profile

---

## 🛍️ 2. Product Management Use Cases

### UC-005: Create Product
**Actor:** Seller  
**Preconditions:** Seller authenticated  
**Main Flow:**
1. Seller navigates to product creation page
2. Seller fills product details (name, description, price, images)
3. Seller selects category and adds tags
4. Seller sets inventory count and low stock threshold
5. Seller configures SEO metadata
6. Seller publishes product
7. System saves product and makes it active
8. System creates associated inventory record

**Alternative Flows:**
- Invalid data → Show validation errors
- Insufficient permissions → Access denied

---

### UC-006: Search Products with Advanced Filtering
**Actor:** Buyer  
**Preconditions:** None  
**Main Flow:**
1. User enters search query or browses categories
2. User applies filters (price range, category, ratings, tags)
3. User uses faceted search with auto-complete
4. User sorts results (price, ratings, popularity, new)
5. System searches product catalog
6. System returns paginated results with filter options

---

### UC-007: Product Review Submission
**Actor:** Buyer  
**Preconditions:** Buyer purchased product  
**Main Flow:**
1. Buyer navigates to product page
2. Buyer selects rating (1-5 stars)
3. Buyer optionally adds review comment
4. Buyer submits review
5. System validates purchase verification
6. System saves review and updates product rating
7. System marks review as verified purchase

---

## 🛒 3. Shopping Cart Use Cases

### UC-008: Add Item to Cart with Quantity Selection
**Actor:** Buyer  
**Preconditions:** Product exists and in stock  
**Main Flow:**
1. User views product details
2. User selects quantity (multiple item selection)
3. User clicks "Add to Cart"
4. System adds item to cart with selected quantity
5. System updates cart total and item count
6. System shows success notification
7. System reserves inventory for cart items

**Alternative Flows:**
- Insufficient inventory → Show availability error

---

### UC-009: Manage Wishlist
**Actor:** Buyer  
**Preconditions:** User authenticated  
**Main Flow:**
1. User views product details
2. User clicks "Add to Wishlist"
3. System adds product to user's wishlist
4. User can view wishlist from account page
5. User can remove items from wishlist
6. User can move wishlist items to cart

---

## 📦 4. Order Management Use Cases

### UC-010: Place Order
**Actor:** Buyer  
**Preconditions:** Cart contains items, user authenticated  
**Main Flow:**
1. User proceeds to checkout
2. User selects/enters shipping address
3. User selects payment method
4. User reviews order details including total amount
5. User confirms order placement
6. System validates inventory availability
7. System creates order record with PENDING status
8. System processes payment
9. System sends order confirmation notification
10. System updates inventory counts

---

### UC-011: Track Order Status
**Actor:** Buyer/Seller  
**Preconditions:** Order exists  
**Main Flow:**
1. User navigates to order history
2. User selects specific order
3. System displays current order status (PENDING → CONFIRMED → DELIVERY → COMPLETE)
4. System shows status timeline with timestamps
5. User can see shipping information and tracking (if available)

---

### UC-012: Cancel Order with Time Window
**Actor:** Buyer  
**Preconditions:** Order in cancellable status (PENDING/CONFIRMED)  
**Main Flow:**
1. User views order details
2. User clicks "Cancel Order" (within allowed time window)
3. System checks cancellation eligibility
4. System prompts for cancellation reason
5. User provides reason and confirms
6. System updates order status to CANCELLED
7. System initiates refund if payment processed
8. System sends cancellation notification to both parties

---

### UC-013: Request Return/Refund
**Actor:** Buyer  
**Preconditions:** Order completed, within return window  
**Main Flow:**
1. Buyer navigates to order details
2. Buyer clicks "Request Return/Refund"
3. Buyer selects reason and adds details
4. System validates return eligibility (time window, condition)
5. System creates return request with PENDING status
6. System notifies seller of return request
7. Seller reviews and approves/denies return
8. System processes refund if approved
9. System updates order status and inventory

---

## 💰 5. Payment Processing Use Cases

### UC-014: Process Payment with Multiple Methods
**Actor:** Payment System  
**Preconditions:** Order created, payment method selected  
**Main Flow:**
1. System validates payment method (Credit Card mock, PayPal Sandbox)
2. System charges selected payment method
3. System handles payment gateway response
4. System updates payment status (SUCCESS/FAILED/PENDING)
5. System creates transaction record
6. System notifies buyer and seller of payment status
7. System reserves funds in escrow until delivery

**Support Methods:** Credit Card (mock), PayPal Sandbox

---

### UC-015: View Payment History
**Actor:** Buyer/Seller  
**Preconditions:** Payment transactions exist  
**Main Flow:**
1. User navigates to payment history section
2. System retrieves user's payment transactions
3. System displays transaction details (amount, method, status, date)
4. User can filter by date range, status
5. User can view refund history
6. Seller can view revenue summary

---

## 🔔 6. Notification Use Cases

### UC-016: Receive Order Status Notifications
**Actor:** User  
**Preconditions:** Order status changes  
**Main Flow:**
1. System detects order status change (Confirmed, Shipped, Delivered, Cancelled)
2. System creates notification record
3. System sends notification via preferred channels:
   - In-app notification
   - Email notification
4. User receives notification
5. User can mark as read or dismiss
6. User can configure notification preferences

---

### UC-017: Receive Security Notifications
**Actor:** User  
**Preconditions:** Security event occurs  
**Main Flow:**
1. System detects security event (new device login, password change)
2. System creates security notification
3. System sends notification via email and in-app
4. User receives security alert with event details
5. User can review recent account activity

---

## ⭐ 7. Review & Rating Use Cases

### UC-018: Submit Product Review with Verification
**Actor:** Buyer  
**Preconditions:** Buyer completed purchase  
**Main Flow:**
1. Buyer navigates to purchased product
2. Buyer rates product (1-5 stars)
3. Buyer optionally writes review comment
4. Buyer submits review
5. System validates purchase history (verified purchase)
6. System saves review and updates product average rating
7. System displays review with verified purchase badge

---

## 🎫 8. Support Ticket Use Cases

### UC-019: Create Support Ticket
**Actor:** User  
**Preconditions:** User authenticated  
**Main Flow:**
1. User navigates to support page
2. User selects ticket category/priority
3. User enters subject and detailed description
4. User can attach files or screenshots
5. User submits ticket
6. System creates ticket record with OPEN status
7. System assigns ticket to support agent
8. System sends confirmation to user
9. System sends notification to assigned agent

---

## 📋 Use Case Matrix

| Use Case ID | Module | Actor | Priority |
|-------------|---------|--------|----------|
| UC-001 | User Management | Guest | High |
| UC-002 | User Management | User | High |
| UC-003 | User Management | User | High |
| UC-004 | User Management | User | Medium |
| UC-005 | Product Catalog | Seller | High |
| UC-006 | Product Catalog | User | High |
| UC-007 | Review System | Buyer | Medium |
| UC-008 | Shopping Cart | User | High |
| UC-009 | User Management | Buyer | Medium |
| UC-010 | Order Management | Buyer | High |
| UC-011 | Order Management | User | High |
| UC-012 | Order Management | Buyer | Medium |
| UC-013 | Order Management | Buyer | Medium |
| UC-014 | Payment Processing | System | High |
| UC-015 | Payment Processing | User | Medium |
| UC-016 | Notification | User | High |
| UC-017 | Notification | User | Medium |
| UC-018 | Review System | Buyer | Medium |
| UC-019 | Support Ticket | User | Medium |

---

# 🏗️ As-Built Use Case Status

> Added 2026-07-26. Verified against the codebase. All flows above are retained as
> written; this section records how each one actually behaves and adds the use cases
> that were built without ever being specified.

## C1. Status matrix

| UC | Title | Status |
|---|---|---|
| UC-001 | User Registration | ✅ As specified |
| UC-002 | User Login with OIDC | ⚠️ Built, flow differs — see C2 |
| UC-003 | MFA Setup | ⚠️ Built, **not mandatory for sellers** — see C3 |
| UC-004 | Profile Management | ⚠️ Built; no IdP write-back — see C4 |
| UC-005 | Create Product | ⚠️ Built; no tags, no SEO metadata, no inventory record — see C5 |
| UC-006 | Search with Advanced Filtering | ⚠️ Partial — no facets, auto-complete, or rating filter — see C6 |
| UC-007 | Product Review Submission | ✅ As specified |
| UC-008 | Add Item to Cart | ⚠️ Built; **no inventory reservation** — see C7 |
| UC-009 | Manage Wishlist | ⚠️ Built; no "move to cart" — see C8 |
| UC-010 | Place Order | ⚠️ Built; payment is a **separate** step — see C9 |
| UC-011 | Track Order Status | ⚠️ Built; different statuses, no timeline or tracking — see C10 |
| UC-012 | Cancel Order with Time Window | ⚠️ Built; **no time window** — see C11 |
| UC-013 | Request Return/Refund | ⚠️ Built; **admin** approves, not seller — see C12 |
| UC-014 | Process Payment | ⚠️ Built on **SePay**; no escrow — see C13 |
| UC-015 | View Payment History | ✅ As specified (buyer + seller) |
| UC-016 | Order Status Notifications | ✅ As specified |
| UC-017 | Security Notifications | ⚠️ Partial — **no new-device detection** — see C14 |
| UC-018 | Review with Verification | ✅ As specified |
| UC-019 | Create Support Ticket | ❌ **Not implemented** — see C15 |

## C2. UC-002 — Login with OIDC (as built)

Local login and SSO are separate entry points, and the OIDC leg is a three-call
dance, not the two implied above:

1. `GET /api/v1/auth/oidc/login` → returns/redirects to the Google consent URL
2. Google → `GET /api/v1/auth/oidc/callback` → app resolves the code
3. `POST /api/v1/auth/oidc/token` → frontend exchanges for app JWTs
   (`app/auth/callback/page.tsx` drives this)

Local login: `POST /api/v1/auth/login`. If MFA is enabled, the response is an **MFA
challenge**, not tokens; the client then calls `POST /api/v1/auth/mfa/verify` (or
`/auth/mfa/recovery`). Unverified accounts are rejected with
`EmailVerificationRequiredException`. `SUSPENDED`/`DEACTIVATED` users are refused.
Identity linking is keyed on (`provider`, `provider_user_id`); `authentication_type`
becomes `HYBRID` when an OIDC identity is attached to a password account.
See `docs/diagrams/oidc-flow.mmd`, `sequence-login.mmd`, `mfa-login-flow.mmd`.

## C3. UC-003 — MFA setup (as built)

Email OTP only (TOTP remains future, as noted). Setup is
`POST /api/v1/users/mfa/setup` → `POST /api/v1/users/mfa/verify` with the emailed
code → MFA enabled and recovery codes returned **once**. Codes are stored hashed in
`recovery_codes`, single-use.

Two deviations from the spec:

- **"Mandatory for sellers" is not enforced anywhere.** MFA is opt-in for every role.
- **Disabling MFA requires a fresh OTP**, an extra step the spec doesn't describe:
  `POST /api/v1/users/mfa/disable/send-otp` → `DELETE /api/v1/users/mfa`.

See `mfa-setup-flow.mmd`, `mfa-disable-flow.mmd`, `mfa-recovery-flow.mmd`.

## C4. UC-004 — Profile management (as built)

`GET`/`PUT /api/v1/users/profile`, plus `PUT /api/v1/users/profile/password`.
Editable: display name, avatar, and the four default address fields
(street/province/district/ward). Avatar changes go through the signed-upload flow
(C16), not a direct file POST.

Step 4 "System syncs with external provider" is **one-directional only** — profile
data is pulled from Google at login; nothing is ever written back to the IdP.

## C5. UC-005 — Create product (as built)

`POST /api/v1/products` (SELLER). Name, slug, description, price, stock, category,
and optionally an initial discount rule. Images are attached afterwards via the
signed-upload flow, so publishing a product with media is a two-step client sequence.

Not built: step 3 tags (no `tags` column), step 5 SEO metadata (only `slug`),
step 8 "creates associated inventory record" (stock is a column on `products`; there
is no `Inventory` entity). Products are created `active = true`.

## C6. UC-006 — Search (as built)

`GET /api/v1/products` accepts `query`, `categoryId`, `sellerId`, `minPrice`,
`maxPrice`, `page`, `size`, `sortBy`. Results are Redis-cached 5 min and paginated
(`PageResponse`).

Not built: step 3 faceted search and auto-complete; rating filter; popularity sort.
`sortBy` is restricted to `name`, `slug`, `price`, `stock`, `createdAt`, `updatedAt`
and silently falls back to `createdAt` — so "sorts results by ratings, popularity"
in step 4 is not achievable through this API.

## C7. UC-008 — Add to cart (as built)

`POST /api/v1/cart/items/{productId}` (BUYER only — **no guest carts**). Quantity is
validated against `products.stock`, and `unit_price` is snapshotted at the
*discounted* effective price via `DiscountService`. Adding an existing product
increments the row (unique per cart+product).

Step 7 **"System reserves inventory for cart items" is not implemented.** Stock is
decremented only at order placement, so two buyers can hold the same last unit in
their carts; the loser gets the availability error at checkout. Optimistic locking
(`carts.version`, `products.version`) prevents lost updates, not overselling at cart
level. `CartExpirationService` sweeps stale carts on a schedule.
See `sequence-add-to-cart.mmd`.

## C8. UC-009 — Wishlist (as built)

`GET`/`POST {productId}`/`DELETE {productId}`/`DELETE` under
`/api/v1/buyers/wishlist`, unique per user+product, surfaced at `app/wishlist/page.tsx`.
Step 6 "move wishlist items to cart" is not a backend operation — the client would
have to add-to-cart then remove-from-wishlist itself.

## C9. UC-010 — Place order (as built)

`POST /api/v1/orders` (BUYER). The request carries the shipping address; the **cart
is resolved server-side** from the authenticated user — there is no `cartId`
parameter as shown in the spec's example payload. The service validates stock,
creates the order `PENDING` with `payment_status = PENDING`, snapshots each line into
`order_items` (product name + image preserved), decrements stock, bumps `sold_count`,
marks the cart `CONVERTED`, and notifies buyer and seller (including `LOW_STOCK` if a
product crossed the threshold).

**Step 8 "System processes payment" is not part of this call.** Payment is a distinct
step the client initiates afterwards: `POST /api/v1/orders/{orderId}/pay`.
See `sequence-place-order.mmd`.

## C10. UC-011 — Track order status (as built)

`GET /api/v1/orders` and `GET /api/v1/orders/{orderId}` (owner-checked); admins use
`GET /api/v1/admin/orders`. Actual statuses are
`PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED, RETURN_REQUESTED, RETURNED` —
**not** `PENDING → CONFIRMED → DELIVERY → COMPLETE`.

Not built: step 4 status timeline with timestamps (only `placed_at`, `delivered_at`,
`cancelled_at`, `return_requested_at` exist — there is no per-transition history
table), and step 5 shipping/tracking information.

Note the actor split: buyers advance their own order via
`PUT /api/v1/orders/{orderId}/status`, and admins via
`PUT /api/v1/admin/orders/{orderId}/status`. **Sellers cannot change order status at
all** — no seller order endpoint exists, despite "Order management and status
updates" in the Seller Dashboard requirements.

## C11. UC-012 — Cancel order (as built)

`POST /api/v1/orders/{orderId}/cancel` with a reason (stored in `cancel_reason`).
Allowed from `PENDING` or `CONFIRMED` only. **There is no time window** — the
"within allowed time window" precondition is not enforced by any clock.

Step 7 differs materially: a **paid** order cannot be cancelled. The API refuses it
with "Cannot cancel a paid order. Request a refund first." (and refuses again if a
refund is already pending). So cancel-then-auto-refund does not happen; the buyer is
routed to the refund flow instead. See `sequence-cancel-order.mmd`.

## C12. UC-013 — Return/refund (as built)

`POST /api/v1/orders/{orderId}/return` with a reason → `return_requested = true`,
`return_requested_at` set, status `RETURN_REQUESTED`. Refunds are raised against the
payment: `POST /api/v1/payments/{paymentId}/refund`.

Two deviations: **step 4 return-window validation is not enforced**, and step 7
approval is performed by an **ADMIN** (`POST /api/v1/payments/{paymentId}/refund/approve`),
**not the seller** — sellers have no return-review endpoint. Step 9's inventory
restock happens on the refund path (`incrementStock`).

## C13. UC-014 — Process payment (as built)

Gateway is **SePay** (sandbox by default), plus a mock credit-card path:
`PaymentMethod { CREDIT_CARD, SEPAY }`. Neither PayPal (named in step 1 above) nor
VNPay (named in `requirements.md`) is integrated.

Flow: `POST /api/v1/orders/{orderId}/pay` creates a `PENDING` payment and returns a
SePay checkout URL → buyer pays → SePay posts `POST /api/v1/payments/ipn`
(public, HMAC-verified, `consumes = */*`) which is authoritative and flips the payment
to `COMPLETED` and the order to `PAID` → the browser returns via
`POST /api/v1/payments/callback` for UI purposes only. Payments carry
`provider_ref`, `invoice_number`, and `failure_reason`; statuses are
`PENDING, COMPLETED, FAILED, REFUND_REQUESTED, REFUNDED` (**`COMPLETED`, not
`SUCCESS`**).

**Step 7 "reserves funds in escrow until delivery" is not implemented** — there is no
escrow, ledger, or payout mechanism. See `sequence-process-payment.mmd` and
`state-order-payment.mmd`.

## C14. UC-017 — Security notifications (as built)

`SECURITY_ALERT` notifications are created for password changes and MFA state
changes, delivered in-app and by email.

Steps 1 and 4 are **not** achievable: there is no new-device or new-location
detection — nothing records device fingerprints, user agents, or IPs per login, and
the design's `users.trusted_devices` column was never built. Step 5 "review recent
account activity" has no endpoint.

## C15. UC-019 — Support ticket

**Not implemented.** There is no ticket entity, table, service, controller, or UI.
`requirements.md` lists "Support Ticketing" under *Future Extensibility*, which is the
accurate statement; this use case, `modules.md` §8, `api_specifications.md` §8 and
`data_model.md` §16 all describe it as in-scope and are stale on that point.

## C16. Additional use cases built but never specified

### UC-A01: Upload product image / avatar (Seller, any User)
Direct-to-storage, three legs:
1. `GET /api/v1/products/{productId}/images/upload-url` (SELLER + owner) or
   `GET /api/v1/users/avatar/upload-url` (any authenticated user) → creates a
   `PENDING` `upload_sessions` row and returns a signed Supabase URL (TTL 2 h)
2. Client PUTs the bytes **directly to Supabase Storage** — they never transit the API
3. Supabase calls `POST /api/v1/webhooks/storage` (HMAC-verified) → session becomes
   `COMPLETED`, an `images` row is inserted, and the product image or avatar is linked

Limits: 10 images per product, 5 MB per file,
`image/jpeg,image/png,image/webp,image/gif`. Delete via
`DELETE /api/v1/products/{productId}/images/{imageId}`.
See `sequence-image-upload.mmd`. **Actor:** also the only flow where an external
system (Supabase) calls into the app besides the payment gateway.

### UC-A02: Run a product discount campaign (Seller)
Seller sets one rule per product — `PERCENT` or `FIXED`, with an optional
start/end window. `DiscountService` computes the effective price, which is snapshotted
into `cart_items.unit_price` and `order_items.unit_price`; `orders` keeps both
`original_amount` and `discount_amount`. See `docs/discount_campaign_plan.md`.

### UC-A03: Suspend or reactivate a user (Admin)
`PUT /api/v1/admin/users/{userId}/status` → `ACTIVE | SUSPENDED | DEACTIVATED`.
Suspended users are refused at login. Written to `admin_action_log`.

### UC-A04: Moderate a product listing (Admin)
`PUT /api/v1/admin/products/{productId}/status` toggles `is_active`, removing a
listing from public search without deleting it. Written to `admin_action_log`.

### UC-A05: Review admin audit trail
Every admin status change writes `admin_action_log` (`admin_id`, `action`,
`target_type`, `target_id`, `details`, `created_at`). No read endpoint yet — the table
is currently inspected directly.

### UC-A06: Refresh an expired session (any User)
`POST /api/v1/auth/refresh` exchanges a refresh token (15 min access / 7 day refresh).
Exempt from the auth rate-limit bucket so normal browsing isn't throttled.
See `sequence-token-refresh.mmd`.

### UC-A07: Resend a verification email (Guest)
`POST /api/v1/auth/resend-verification` — needed because the UC-001 verification mail
can expire or be lost. Surfaced at `app/verify-email/page.tsx`.

### UC-A08: Recover access with a backup code (User)
`POST /api/v1/auth/mfa/recovery` consumes one single-use hashed code from
`recovery_codes`. Referenced by UC-003's postcondition but never given its own flow.
See `mfa-recovery-flow.mmd`.

### UC-A09: Seller reviews own revenue
`GET /api/v1/payments/seller/history` — the only seller-facing financial report.
Not the analytics dashboard `requirements.md` asks for; see that document's §S3.

### UC-A10: Cart expiry sweep (System)
`CartExpirationService` runs on a schedule and abandons stale carts.

## C17. Frontend coverage

The Next.js app (`marketplace-frontend/marketplace`) realises these use cases at:

| Area | Routes |
|---|---|
| Auth | `app/login`, `app/register`, `app/verify-email`, `app/auth/callback` |
| Buyer | `app/page.tsx`, `app/products/[id]`, `app/cart`, `app/checkout`, `app/checkout/payment`, `app/orders`, `app/orders/[orderId]`, `app/wishlist`, `app/profile` |
| Seller | `app/seller`, `app/seller/products`, `app/seller/products/new`, `app/seller/products/[id]/edit` |
| Public seller | `app/sellers/[sellerId]` |
| Admin | `app/admin`, `app/admin/users`, `app/admin/products`, `app/admin/orders`, `app/admin/analytics`, `app/admin/categories` |

Route handlers under `app/api/**` proxy to the backend so JWTs stay in httpOnly
cookies. ⚠️ `app/api/orders/[orderId]/items/route.ts` proxies to
`GET /api/v1/orders/{orderId}/items`, which **was never implemented** in the backend —
see `api_specifications.md` §A9.

