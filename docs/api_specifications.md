# 📡 API Specifications


## 🌐 API Endpoints by Module

### 🔐 1. Authentication & User Management

#### Auth Endpoints
```
POST   /api/v1/auth/register              # User registration
POST   /api/v1/auth/login                 # User login (local/OIDC)
POST   /api/v1/auth/refresh               # Token refresh
POST   /api/v1/auth/logout                # User logout
POST   /api/v1/auth/forgot-password       # Password reset request
POST   /api/v1/auth/reset-password        # Password reset
POST   /api/v1/auth/verify-email          # Email verification
GET    /api/v1/auth/providers             # Get available IdPs
POST   /api/v1/auth/oidc/login            # OIDC login initiation
GET    /api/v1/auth/oidc/callback         # OIDC callback endpoint
```

#### User Profile Endpoints
```
GET    /api/v1/users/profile              # Get current user profile
PUT    /api/v1/users/profile              # Update user profile
PUT    /api/v1/users/profile/password     # Change password
GET    /api/v1/users/{userId}             # Get user public profile
```

#### MFA Endpoints
```
POST   /api/v1/users/mfa/setup            # Setup MFA
POST   /api/v1/users/mfa/verify           # Verify MFA code
DELETE /api/v1/users/mfa                 # Disable MFA
POST   /api/v1/users/mfa/recovery         # Use recovery code
GET    /api/v1/users/mfa/status           # Get MFA status
```

#### Seller Profile Endpoints
```
POST   /api/v1/sellers/profile            # Create seller profile
GET    /api/v1/sellers/profile            # Get seller profile
PUT    /api/v1/sellers/profile            # Update seller profile
GET    /api/v1/sellers/{sellerId}         # Get public seller info
```

#### Buyer Profile Endpoints
```
GET    /api/v1/buyers/profile             # Get buyer profile
PUT    /api/v1/buyers/profile             # Update buyer profile
POST   /api/v1/buyers/wishlist/{productId} # Add to wishlist
DELETE /api/v1/buyers/wishlist/{productId} # Remove from wishlist
GET    /api/v1/buyers/wishlist            # Get wishlist items
```

---

### 🛍️ 2. Product Catalog

#### Product Endpoints
```
POST   /api/v1/products                   # Create product (Seller)
GET    /api/v1/products                   # List products (with filters)
GET    /api/v1/products/{productId}       # Get product details
PUT    /api/v1/products/{productId}       # Update product (Seller)
DELETE /api/v1/products/{productId}       # Delete product (Seller)
PATCH  /api/v1/products/{productId}/status # Activate/Deactivate (Seller)
```

#### Category Endpoints
```
GET    /api/v1/categories                 # List all categories
POST   /api/v1/categories                 # Create category (Admin)
GET    /api/v1/categories/{categoryId}    # Get category
PUT    /api/v1/categories/{categoryId}    # Update category (Admin)
DELETE /api/v1/categories/{categoryId}    # Delete category (Admin)
```

---

### 🛒 3. Shopping Cart

#### Cart Endpoints
```
GET    /api/v1/cart                       # Get current user's cart
POST   /api/v1/cart/items                 # Add item to cart
PUT    /api/v1/cart/items/{itemId}        # Update cart item quantity
DELETE /api/v1/cart/items/{itemId}        # Remove item from cart
DELETE /api/v1/cart                       # Clear cart
POST   /api/v1/cart/checkout              # Proceed to checkout
GET    /api/v1/cart/guest/{cartToken}     # Get guest cart
```

---

### 📦 4. Order Management

#### Order Endpoints
```
POST   /api/v1/orders                     # Create order from cart
GET    /api/v1/orders                     # List user's orders
GET    /api/v1/orders/{orderId}           # Get order details
PUT    /api/v1/orders/{orderId}/status    # Update order status (Admin/Seller)
POST   /api/v1/orders/{orderId}/cancel    # Cancel order
POST   /api/v1/orders/{orderId}/return    # Request return
GET    /api/v1/orders/{orderId}/items     # Get order items
```

---

### 💰 5. Payment Processing

#### Payment Endpoints
```
POST   /api/v1/payments                   # Process payment
GET    /api/v1/payments/{paymentId}       # Get payment details
POST   /api/v1/payments/{paymentId}/refund # Initiate refund
GET    /api/v1/payments/methods           # Get available payment methods
GET    /api/v1/payments/history           # Get payment history
```

---

### 🔔 6. Notification System

#### Notification Endpoints
```
GET    /api/v1/notifications              # Get user notifications
PUT    /api/v1/notifications/{notificationId}/read # Mark as read
DELETE /api/v1/notifications/{notificationId} # Delete notification
PUT    /api/v1/notifications/settings     # Update notification preferences
DELETE /api/v1/notifications              # Clear all notifications
```

---

### ⭐ 7. Review System

#### Review Endpoints
```
POST   /api/v1/reviews                    # Submit review
GET    /api/v1/reviews                    # List reviews (with filters)
GET    /api/v1/reviews/{reviewId}         # Get review details
PUT    /api/v1/reviews/{reviewId}         # Update review
DELETE /api/v1/reviews/{reviewId}         # Delete review
GET    /api/v1/products/{productId}/reviews # Get product reviews
```

---

### 🎫 8. Support Ticket

#### Ticket Endpoints
```
POST   /api/v1/tickets                    # Create support ticket
GET    /api/v1/tickets                    # List user tickets
GET    /api/v1/tickets/{ticketId}         # Get ticket details
PUT    /api/v1/tickets/{ticketId}         # Update ticket
POST   /api/v1/tickets/{ticketId}/messages # Add message to ticket
GET    /api/v1/tickets/categories         # Get ticket categories
GET    /api/v1/tickets/priorities         # Get ticket priorities
```

---

## 📋 Request/Response Examples

### User Registration with OIDC Support
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "displayName": "John Doe",
  "role": "BUYER",
  "authenticationType": "LOCAL"  # or "OIDC"
}

Response: 201 Created
{
  "id": "uuid-here",
  "email": "user@example.com",
  "displayName": "John Doe",
  "role": "BUYER",
  "isVerified": false,
  "authenticationType": "LOCAL",
  "createdAt": "2024-01-01T10:00:00Z"
}
```

### Product Creation with Inventory
```http
POST /api/v1/products
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Wireless Headphones",
  "description": "High-quality wireless headphones",
  "price": 99.99,
  "currency": "USD",
  "categoryId": "category-uuid",
  "tags": ["electronics", "audio", "wireless"],
  "images": ["https://example.com/image1.jpg"],
  "inventory": {
    "quantity": 50,
    "lowStockThreshold": 5
  }
}

Response: 201 Created
{
  "id": "product-uuid",
  "name": "Wireless Headphones",
  "price": 99.99,
  "isActive": true,
  "createdAt": "2024-01-01T10:00:00Z"
}
```

### Order Creation with Payment Processing
```http
POST /api/v1/orders
Authorization: Bearer <token>
Content-Type: application/json

{
  "cartId": "cart-uuid",
  "shippingAddress": {
    "street": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "USA"
  }
}

Response: 201 Created
{
  "id": "order-uuid",
  "totalAmount": 99.99,
  "currency": "USD",
  "status": "PENDING",
  "paymentStatus": "PENDING",
  "placedAt": "2024-01-01T10:00:00Z"
}
```

### MFA Setup
```http
POST /api/v1/users/mfa/setup
Authorization: Bearer <token>
Content-Type: application/json

{
  "method": "EMAIL"  # Future: "TOTP"
}

Response: 200 OK
{
  "mfaEnabled": true,
  "methods": ["EMAIL"],
  "recoveryCodesGenerated": true
}
```

---

## 🚨 Error Response Format

```json
{
  "timestamp": "2024-01-01T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/users",
  "errors": [
    {
      "field": "email",
      "message": "must be a valid email address"
    }
  ]
}
```

---

## 🔐 Authentication Headers

All protected endpoints require:
```
Authorization: Bearer <jwt-token>
Content-Type: application/json
```

For guest cart operations:
```
X-Guest-Token: <guest-cart-token>
Content-Type: application/json
```

---

## 📈 Rate Limiting

```
Rate Limit: 1000 requests/hour per IP
Rate Limit: 100 requests/minute per authenticated user
Rate Limit: 10 requests/minute for auth endpoints
```

---

## 🔄 API Versioning

```
Current Version: v1
Base URL: /api/v1/
Future versions: /api/v2/, etc.
```

> **Note:** everything above is the original design spec and is retained as-is.
> The authoritative, verified list of endpoints that actually exist in the codebase is
> in **§ As-Built API Surface** at the end of this document. Where the two disagree,
> the as-built section wins.

---

## 📊 HTTP Status Codes

| Code | Description |
|------|-------------|
| 200 | OK - Successful GET, PUT, PATCH |
| 201 | Created - Successful POST |
| 204 | No Content - Successful DELETE |
| 400 | Bad Request - Invalid request data |
| 401 | Unauthorized - Authentication required |
| 403 | Forbidden - Access denied |
| 404 | Not Found - Resource not found |
| 409 | Conflict - Resource already exists |
| 422 | Unprocessable Entity - Validation errors |
| 429 | Too Many Requests - Rate limit exceeded |
| 500 | Internal Server Error - Server error |

---

# 🏗️ As-Built API Surface

> Added 2026-07-26. Verified against `src/main/java/com/marketplace/**/controller/*.java`.
> Nothing above this line was removed — this section records what the running
> application actually exposes, including endpoints the spec above never mentioned.
>
> **Auth column legend:** `public` = matched by a `permitAll()` rule in
> `SecurityConfig`; `auth` = authenticated, no role check; `BUYER`/`SELLER`/`ADMIN` =
> `@PreAuthorize` role; `+owner` = additionally gated by `PermissionService`
> row-level ownership check.

## A1. Authentication — `AuthController` (`/api/v1/auth`, all `public`)

| Method | Path | Notes |
|---|---|---|
| POST | `/api/v1/auth/register` | |
| POST | `/api/v1/auth/login` | May return an MFA challenge instead of tokens |
| POST | `/api/v1/auth/refresh` | Exempt from the `auth` rate-limit bucket |
| POST | `/api/v1/auth/logout` | |
| POST | `/api/v1/auth/verify-email` | |
| POST | `/api/v1/auth/resend-verification` | **not in original spec** |
| POST | `/api/v1/auth/forgot-password` | |
| POST | `/api/v1/auth/reset-password` | |
| GET | `/api/v1/auth/oidc/login` | Spec said `POST` — it is **`GET`** (redirect initiation) |
| GET | `/api/v1/auth/oidc/callback` | |
| POST | `/api/v1/auth/oidc/token` | **not in original spec** — exchanges the OIDC code for app tokens |
| POST | `/api/v1/auth/mfa/verify` | **not in original spec** — login-time MFA challenge |
| POST | `/api/v1/auth/mfa/recovery` | Spec placed this under `/users/mfa/recovery` |

## A2. Users & MFA — `UserController` (`/api/v1/users`, all `auth`)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/v1/users/profile` | |
| PUT | `/api/v1/users/profile` | |
| PUT | `/api/v1/users/profile/password` | |
| POST | `/api/v1/users/mfa/setup` | |
| POST | `/api/v1/users/mfa/verify` | Confirms setup (distinct from `/auth/mfa/verify`) |
| DELETE | `/api/v1/users/mfa` | Requires an OTP obtained below |
| POST | `/api/v1/users/mfa/disable/send-otp` | **not in original spec** |
| GET | `/api/v1/users/mfa/status` | |

## A3. Sellers — `SellerController`

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/api/v1/sellers/{sellerId}` | public | Public seller info |

## A4. Product Catalog — `ProductController` / `SellerProductController`

| Method | Path | Auth |
|---|---|---|
| POST | `/api/v1/products` | SELLER |
| GET | `/api/v1/products` | public |
| GET | `/api/v1/products/{productId}` | public |
| PUT | `/api/v1/products/{productId}` | SELLER +owner |
| DELETE | `/api/v1/products/{productId}` | SELLER +owner |
| DELETE | `/api/v1/products/{productId}/images/{imageId}` | SELLER +owner |
| GET | `/api/v1/seller/products` | SELLER |

Search query parameters accepted by `GET /api/v1/products` (`ProductSearchRequest`):
`query`, `categoryId`, `sellerId`, `minPrice`, `maxPrice`, `page`, `size`, `sortBy`.
`sortBy` is validated against an allowlist — `name`, `slug`, `price`, `stock`,
`createdAt`, `updatedAt` — and silently falls back to `createdAt`. `size` is clamped
to 1..100 (default 20), `page` to 0..10000.

## A5. Categories — `CategoryController` (`/api/v1/categories`)

| Method | Path | Auth |
|---|---|---|
| GET | `/api/v1/categories` | public |
| GET | `/api/v1/categories/{categoryId}` | public |
| POST | `/api/v1/categories` | ADMIN |
| PUT | `/api/v1/categories/{categoryId}` | ADMIN |
| DELETE | `/api/v1/categories/{categoryId}` | ADMIN |

## A6. Media Upload — `UploadController` / `WebhookController`

Entirely absent from the original spec. Uploads are **direct-to-Supabase**: the API
issues a short-lived signed upload URL, the client PUTs the bytes to Supabase
Storage, and Supabase calls the webhook to confirm.

| Method | Path | Auth |
|---|---|---|
| GET | `/api/v1/users/avatar/upload-url` | auth |
| GET | `/api/v1/products/{productId}/images/upload-url` | SELLER +owner |
| POST | `/api/v1/webhooks/storage` | public (HMAC-verified via `app.webhook.secret`) |

Upload policy (`app.upload.*`): max 10 images per product, max 5 MB per file,
content types `image/jpeg,image/png,image/webp,image/gif`, signed URL TTL 2 hours.
See also `docs/diagrams/sequence-image-upload.mmd`.

## A7. Cart — `CartController` (`/api/v1/cart`, all `BUYER`)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/v1/cart` | |
| POST | `/api/v1/cart/items/{productId}` | Spec said `POST /cart/items` with a body — the product ID is a **path variable** |
| PUT | `/api/v1/cart/items/{itemId}` | +owner |
| DELETE | `/api/v1/cart/items/{itemId}` | +owner |
| DELETE | `/api/v1/cart` | Clear cart |

**Guest carts are not implemented.** `GET /api/v1/cart/guest/{cartToken}`, the
`X-Guest-Token` header, and `POST /api/v1/cart/checkout` described above do not
exist; every cart operation requires an authenticated `BUYER`. Checkout is
`POST /api/v1/orders`, which reads the caller's active cart server-side.

## A8. Wishlist — `WishlistController` (`/api/v1/buyers/wishlist`, all `BUYER`)

| Method | Path |
|---|---|
| GET | `/api/v1/buyers/wishlist` |
| POST | `/api/v1/buyers/wishlist/{productId}` |
| DELETE | `/api/v1/buyers/wishlist/{productId}` |
| DELETE | `/api/v1/buyers/wishlist` |

The other `/api/v1/buyers/profile` endpoints in the spec do not exist — there is no
`BuyerProfile` entity; buyer data lives on `User`.

## A9. Orders — `OrderController` / `AdminOrderController`

| Method | Path | Auth |
|---|---|---|
| POST | `/api/v1/orders` | BUYER |
| GET | `/api/v1/orders` | BUYER |
| GET | `/api/v1/orders/{orderId}` | BUYER +owner |
| PUT | `/api/v1/orders/{orderId}/status` | BUYER +owner |
| POST | `/api/v1/orders/{orderId}/cancel` | BUYER +owner |
| POST | `/api/v1/orders/{orderId}/return` | BUYER +owner |
| GET | `/api/v1/admin/orders` | ADMIN |
| PUT | `/api/v1/admin/orders/{orderId}/status` | ADMIN |

⚠️ **`GET /api/v1/orders/{orderId}/items` (listed in §4 above) was never
implemented.** The frontend proxy `app/api/orders/[orderId]/items/route.ts` still
calls it and therefore fails. Order items are returned inline on
`GET /api/v1/orders/{orderId}`. Either implement the endpoint or drop the proxy.

Cancellation eligibility is **status-based only** — allowed from `PENDING` or
`CONFIRMED`, refused if the order is already paid (refund required instead) or has a
pending refund. There is no elapsed-time window.

## A10. Payments — `PaymentController`

`PaymentController` has no class-level `@RequestMapping`; each method carries its
own full path, so the routes are split across two prefixes.

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/v1/orders/{orderId}/pay` | BUYER +owner | Replaces the spec's `POST /api/v1/payments` |
| POST | `/api/v1/payments/ipn` | public | SePay server-to-server IPN, `consumes = */*` |
| POST | `/api/v1/payments/callback` | auth | Browser return leg |
| GET | `/api/v1/payments/{paymentId}` | auth +owner | |
| POST | `/api/v1/payments/{paymentId}/refund` | BUYER +owner | Raises a refund request |
| POST | `/api/v1/payments/{paymentId}/refund/approve` | ADMIN | **not in original spec** |
| GET | `/api/v1/payments/history` | BUYER | |
| GET | `/api/v1/payments/seller/history` | SELLER | **not in original spec** |

`GET /api/v1/payments/methods` does not exist. Methods are the fixed enum
`PaymentMethod { CREDIT_CARD, SEPAY }`.

**Gateway:** SePay (sandbox by default). The spec text elsewhere in this repo
variously names VNPay and PayPal; neither is integrated. Config lives under
`app.sepay.*` (`merchant-id`, `secret-key`, `webhook-secret`, `environment`,
`success-url`, `error-url`, `cancel-url`).

## A11. Reviews — `ReviewController` (base `/api/v1`)

| Method | Path | Auth |
|---|---|---|
| POST | `/api/v1/reviews` | BUYER |
| GET | `/api/v1/reviews` | BUYER (caller's own reviews) |
| GET | `/api/v1/reviews/{reviewId}` | auth |
| PUT | `/api/v1/reviews/{reviewId}` | BUYER +owner |
| DELETE | `/api/v1/reviews/{reviewId}` | BUYER +owner |
| GET | `/api/v1/products/{productId}/reviews` | public |

One review per (product, buyer) — enforced by a unique constraint. Writing a review
recalculates `products.average_rating` and `products.review_count`.

## A12. Notifications — `NotificationController` (`/api/v1/notifications`, all `auth`)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/v1/notifications` | Unread / recent |
| GET | `/api/v1/notifications/all` | **not in original spec** |
| GET | `/api/v1/notifications/unread-count` | **not in original spec** |
| PUT | `/api/v1/notifications/{notificationId}/read` | +owner |
| PUT | `/api/v1/notifications/read-all` | **not in original spec** |
| DELETE | `/api/v1/notifications/{notificationId}` | +owner |
| DELETE | `/api/v1/notifications` | Clear all |
| PUT | `/api/v1/notifications/settings` | |

## A13. Admin — `AdminUserController` / `AdminProductController` / `AdminAnalyticsController`

| Method | Path | Auth |
|---|---|---|
| GET | `/api/v1/admin/users` | ADMIN |
| PUT | `/api/v1/admin/users/{userId}/status` | ADMIN |
| GET | `/api/v1/admin/products` | ADMIN |
| PUT | `/api/v1/admin/products/{productId}/status` | ADMIN |
| GET | `/api/v1/admin/analytics/revenue` | ADMIN |
| GET | `/api/v1/admin/analytics/orders` | ADMIN |
| GET | `/api/v1/admin/analytics/users` | ADMIN |
| GET | `/api/v1/admin/analytics/products` | ADMIN |

Analytics responses are Redis-cached for 5 minutes. Status changes are written to
`admin_action_log`. **These endpoints are ADMIN-only** — the seller-facing sales
analytics described in `requirements.md` has no endpoint; sellers get
`GET /api/v1/payments/seller/history` only.

## A14. Spec'd but not implemented

| Documented above | Status |
|---|---|
| `GET /api/v1/auth/providers` | Not implemented — Google is the only provider, configured statically |
| `GET /api/v1/users/{userId}` | Not implemented |
| `POST\|GET\|PUT /api/v1/sellers/profile` | Not implemented — no `SellerProfile` entity |
| `GET\|PUT /api/v1/buyers/profile` | Not implemented — no `BuyerProfile` entity |
| `POST /api/v1/cart/checkout` | Not implemented — use `POST /api/v1/orders` |
| `GET /api/v1/cart/guest/{cartToken}` | Not implemented — no guest carts |
| `GET /api/v1/orders/{orderId}/items` | Not implemented (frontend proxy is broken — see §A9) |
| `POST /api/v1/payments` | Not implemented — use `POST /api/v1/orders/{orderId}/pay` |
| `GET /api/v1/payments/methods` | Not implemented |
| `PATCH /api/v1/products/{productId}/status` | Not implemented — sellers toggle `active` via `PUT /api/v1/products/{productId}`; admins use `PUT /api/v1/admin/products/{productId}/status` |
| All 7 `/api/v1/tickets/*` endpoints | Not implemented — Support Ticket module does not exist in code |

## A15. Actual security & rate-limit configuration

`SecurityConfig`: stateless (`SessionCreationPolicy.STATELESS`), CSRF disabled, CORS
configured, JWT read via `JwtSecurityContextRepository` + `JwtAuthenticationFilter`.
`permitAll()` matchers, in order:

```
/api/v1/auth/**
/swagger-ui/**, /api-docs/**, /v3/api-docs/**, /swagger-ui.html
/api/v1/webhooks/**
POST /api/v1/payments/ipn
GET  /api/v1/products/**, /api/v1/categories/**
GET  /api/v1/sellers/**
anyRequest() -> authenticated()
```

Rate limiting is **Bucket4j over Redis**, applied per authenticated user (falling
back to client IP) and per route group — not the flat per-IP/per-user hourly quotas
described in § Rate Limiting above. Actual defaults from `application.yml`
(`app.rate-limit.*`, all 1-minute refill windows):

| Group | Capacity/min | Matches |
|---|---|---|
| `auth` | 10 | `/api/v1/auth/**` except `/api/v1/auth/refresh` |
| `orders` | 20 | POST/PUT/DELETE on `/api/v1/orders*` |
| `payments` | 10 | POST/PUT/DELETE on `/api/v1/payments*` |
| `reviews` | 10 | POST/PUT/DELETE on `/api/v1/reviews*` |
| `uploads` | 5 | POST/PUT/DELETE on upload paths |

> ⚠️ Known defect in `RateLimitFilter` (~line 126): the review-group matcher is
> `^/api/v1/products/\d+/reviews$`, but product IDs are UUIDs, so that branch is
> dead. Review writes are still limited because `/api/v1/reviews` matches by prefix.

JWT lifetimes: access token 900 000 ms (15 min), refresh token 604 800 000 ms (7 days).

## A16. Response envelope & OpenAPI

Responses use `ApiResponse<T>` and `PageResponse<T>` from `shared/dto`, and Jackson
is configured with `default-property-inclusion: non_null`, so null fields are
omitted. Errors are produced by `GlobalExceptionHandler` as `ErrorResponse`.

Live, generated contract — prefer this over any hand-written list, including this one:

```
GET /v3/api-docs          # OpenAPI JSON
GET /swagger-ui/index.html # Swagger UI
```