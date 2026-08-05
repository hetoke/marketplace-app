# 🧱 Modules

## 📦 Core Modules

```
marketplace-app/
├── user-management/          # Authentication, profiles, MFA
├── product-catalog/          # Products, categories, inventory
├── shopping-cart/           # Cart management, guest checkout
├── order-management/        # Orders, status tracking
├── payment-processing/      # Payments, refunds
├── notification/            # Notifications system
├── review-system/          # Product reviews, ratings
├── support-ticket/         # Customer support tickets
├── shared-kernel/          # Common types, exceptions, utilities
└── api-gateway/            # REST API controllers, routing
```

---

## 📚 Module Details

### 1. **User Management Module**
**Responsibilities:**
- User registration, login, logout
- MFA management
- Profile management (buyer/seller)
- Password reset, account recovery
- Identity Provider integration (OIDC/OAuth2)

**Key Entities:** User, SellerProfile, BuyerProfile, IdentityProvider, UserIdentity, OAuthClient

---

### 2. **Product Catalog Module**
**Responsibilities:**
- Product CRUD operations
- Category management
- Inventory tracking
- Product activation/deactivation

**Key Entities:** Product, Category, Inventory

---

### 3. **Shopping Cart Module**
**Responsibilities:**
- Cart creation and management
- Persistent cart across sessions
- Guest checkout support

**Key Entities:** Cart, CartItem

---

### 4. **Order Management Module**
**Responsibilities:**
- Order creation from cart
- Order status tracking (PENDING → CONFIRMED → DELIVERY → COMPLETE)
- Order cancellation
- Return/refund requests

**Key Entities:** Order

---

### 5. **Payment Processing Module**
**Responsibilities:**
- Payment method management
- Transaction processing
- Refund handling

**Key Entities:** Payment

---

### 6. **Notification Module**
**Responsibilities:**
- Notification system (order updates, security alerts, promotional)
- Notification delivery tracking

**Key Entities:** Notification

---

### 7. **Review System Module**
**Responsibilities:**
- Product reviews and ratings
- Review moderation
- Verified purchase validation

**Key Entities:** Review

---

### 8. **Support Ticket Module**
**Responsibilities:**
- Customer support ticketing
- Ticket assignment and escalation
- Resolution tracking

**Key Entities:** SupportTicket

---

### 9. **Shared Kernel Module**
**Responsibilities:**
- Common domain types and value objects
- Base entity classes
- Common exceptions and error handling
- Utility functions
- Configuration and constants

---

### 10. **API Gateway Module**
**Responsibilities:**
- REST API controllers
- Request validation
- Authentication middleware
- API versioning



## 📁Project Structure

```
marketplace-app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── marketplace/
│   │   │           ├── MarketplaceApplication.java
│   │   │           ├── api/
│   │   │           │   ├── controller/
│   │   │           │   │   ├── AuthController.java
│   │   │           │   │   ├── ProductController.java
│   │   │           │   │   ├── OrderController.java
│   │   │           │   │   ├── UserController.java
│   │   │           │   │   ├── CartController.java
│   │   │           │   │   ├── PaymentController.java
│   │   │           │   │   ├── ReviewController.java
│   │   │           │   │   ├── NotificationController.java
│   │   │           │   │   └── TicketController.java
│   │   │           │   └── dto/
│   │   │           ├── shared/
│   │   │           ├── user/
│   │   │           │   ├── model/
│   │   │           │   │   ├── User.java
│   │   │           │   │   ├── SellerProfile.java
│   │   │           │   │   ├── BuyerProfile.java
│   │   │           │   │   ├── IdentityProvider.java
│   │   │           │   │   ├── UserIdentity.java
│   │   │           │   │   └── OAuthClient.java
│   │   │           ├── product/
│   │   │           │   ├── model/
│   │   │           │   │   ├── Product.java
│   │   │           │   │   ├── Category.java
│   │   │           │   │   └── Inventory.java
│   │   │           ├── cart/
│   │   │           │   ├── model/
│   │   │           │   │   ├── Cart.java
│   │   │           │   │   └── CartItem.java
│   │   │           ├── order/
│   │   │           │   ├── model/
│   │   │           │   │   └── Order.java
│   │   │           ├── payment/
│   │   │           │   ├── model/
│   │   │           │   │   └── Payment.java
│   │   │           ├── notification/
│   │   │           │   ├── model/
│   │   │           │   │   └── Notification.java
│   │   │           ├── review/
│   │   │           │   ├── model/
│   │   │           │   │   └── Review.java
│   │   │           └── ticket/
│   │   │               ├── model/
│   │   │               └── SupportTicket.java
```

---

# 🏗️ As-Built Module Layout

> Added 2026-07-26. Verified against `src/main/java/com/marketplace/`.
> The design layout above is retained; this section records the packages that
> actually exist. Two structural decisions diverged from the plan.

## D1. Structural divergences

**1. No `api-gateway` module — controllers live in their own domain slice.**
The design put every controller under a shared `api/controller/` package. As built,
each domain owns its full vertical stack (`controller` / `dto` / `service` /
`repository` / `model`), so `ProductController` lives at
`com.marketplace.product.controller`, not `com.marketplace.api.controller`. The only
survivor of the `api` package is `com.marketplace.api.OpenApiConfig`. Cross-cutting
concerns the design assigned to the gateway (auth filter, exception handling,
rate limiting) live in `shared/` instead.

**2. Package names are single words, not hyphenated.**
`user-management/` → `user/`, `product-catalog/` → `product/`,
`shopping-cart/` → `cart/`, `order-management/` → `order/`,
`payment-processing/` → `payment/`, `review-system/` → `review/`,
`shared-kernel/` → `shared/`.

**3. Modules added that the design did not anticipate:**
`upload/` (media), `webhook/` (inbound Supabase callbacks), `wishlist/`,
`admin/`, `email/`.

**4. Module not built:** `support-ticket/` — see §D4.

## D2. As-built package tree

```
com.marketplace/
├── MarketplaceApplication.java
├── api/
│   └── OpenApiConfig.java                 # all that remains of "api-gateway"
├── user/                                  # = design "user-management"
│   ├── config/OidcProperties
│   ├── controller/  AuthController, UserController, SellerController
│   ├── dto/         (13 records)
│   ├── model/       User, UserIdentity, RefreshToken, VerificationToken,
│   │                MFAChallenge, RecoveryCode
│   ├── repository/  (6)
│   └── service/     AuthService, UserService, MFAService, OidcService
├── product/                               # = design "product-catalog"
│   ├── controller/  ProductController, CategoryController, SellerProductController
│   ├── dto/         (6)
│   ├── model/       Product, Category, ProductImage, DiscountType
│   ├── repository/  ProductRepository, CategoryRepository, ProductImageRepository
│   └── service/     ProductService, CategoryService, DiscountService
├── cart/                                  # = design "shopping-cart"
│   ├── controller/  CartController
│   ├── dto/         (4)
│   ├── model/       Cart, CartItem
│   ├── repository/  CartRepository, CartItemRepository
│   └── service/     CartService, CartExpirationService
├── order/                                 # = design "order-management"
│   ├── controller/  OrderController, AdminOrderController
│   ├── dto/         (5)
│   ├── model/       Order, OrderItem, OrderStatus
│   ├── repository/  OrderRepository, OrderItemRepository
│   └── service/     OrderService
├── payment/                               # = design "payment-processing"
│   ├── config/      SePayProperties
│   ├── controller/  PaymentController
│   ├── dto/         (9)
│   ├── model/       Payment
│   ├── repository/  PaymentRepository
│   └── service/     PaymentService, SePayService
├── review/                                # = design "review-system"
├── notification/                          # = design "notification"
│   └── model/       Notification, NotificationType
├── wishlist/                              # NEW — was BuyerProfile.wishlist in design
├── upload/                                # NEW — media pipeline
│   ├── controller/  UploadController
│   ├── model/       Image, UploadSession, EntityType, UploadStatus
│   ├── service/     UploadService, ImageService
│   └── storage/     SupabaseStorageClient, SupabaseStorageProperties
├── webhook/                               # NEW — inbound Supabase storage callbacks
│   ├── config/WebhookConfig, controller/WebhookController,
│   └── dto/StorageWebhookRequest, service/WebhookService
├── admin/                                 # NEW — moderation + analytics
│   ├── controller/  AdminUserController, AdminProductController,
│   │                AdminAnalyticsController
│   ├── dto/         (8)
│   ├── model/       AdminActionLog
│   └── service/     AdminUserService, AdminProductService, AdminAnalyticsService
├── email/                                 # NEW
│   └── EmailService.java
└── shared/                                # = design "shared-kernel" (+ gateway duties)
    ├── config/      CacheConfig, DataSeeder, RateLimitConfig, RateLimitProperties
    ├── dto/         ApiResponse, PageResponse
    ├── exception/   GlobalExceptionHandler, BusinessException, ErrorResponse,
    │                ResourceNotFoundException, AccessDeniedException,
    │                EmailVerificationRequiredException
    ├── filter/      RateLimitFilter
    └── security/    SecurityConfig, JwtTokenProvider, JwtAuthenticationFilter,
                     JwtSecurityContextRepository, JwtProperties, TokenType,
                     PermissionService, CustomUserDetailsService,
                     CustomAuthenticationEntryPoint, CustomAccessDeniedHandler
```

195 Java files total (169 main, 26 test).

## D3. Responsibilities the design didn't assign

**`upload/` (new).** Direct-to-Supabase media pipeline: issues signed upload URLs,
tracks two-phase `UploadSession` state, records `Image` rows polymorphically by
(`entity_type`, `entity_id`) for both `USER` avatars and `PRODUCT` images, enforces
count/size/content-type limits.
**Key entities:** `Image`, `UploadSession`.

**`webhook/` (new).** Terminates inbound calls from Supabase Storage with HMAC
verification, and confirms the matching upload session.
**Key entities:** none (operates on `upload/`).

**`wishlist/` (new).** Standalone `wishlist_items` table — in the design this was an
array field on the never-built `BuyerProfile`.
**Key entities:** `WishlistItem`.

**`admin/` (new).** User suspension, product moderation, and the four read-only
analytics endpoints (computed on read, Redis-cached 5 min — no `AnalyticsReport`
table). Writes an audit trail.
**Key entities:** `AdminActionLog`.

**`email/` (new).** Single `EmailService` over `spring-boot-starter-mail`, called by
`user/` (verification, reset, security alerts) and `notification/`.

**`shared/` (expanded).** Beyond the design's common types and exceptions, it now owns
the whole security stack (stateless JWT resolution, `PermissionService` row-level
ownership checks used by `@PreAuthorize` across every module), Bucket4j rate limiting,
Redis cache configuration, and dev data seeding — the duties the design had assigned
to `api-gateway`.

**`product/DiscountService` (new).** Per-product discount rules; the single source of
effective price, consumed by `cart/` and `order/`.

## D4. `support-ticket/` — not built

No `ticket` package, entity, table, service, controller or UI exists.
`requirements.md` lists Support Ticketing under *Future Extensibility*, which is
accurate; this document's §8, `use_case_specifications.md` UC-019,
`api_specifications.md` §8 and `data_model.md` §16 all describe it as in-scope and are
stale on that point. §8 above is retained for when it is picked up.

## D5. Module dependency direction

```
                    ┌──────────────────────────────────────┐
                    │ shared (security, exceptions, cache, │
                    │ rate limit, DTO envelopes)           │
                    └──────────────────────────────────────┘
                                    ▲ (everything depends on shared)
   admin ──▶ user, product, order, payment
   order ──▶ cart, product, payment, notification
   cart  ──▶ product (DiscountService)
   payment ──▶ order, notification
   review ──▶ product, order   (verified-purchase check + rating rollup)
   upload ──▶ product, user     ◀── webhook
   user  ──▶ email, notification
   wishlist ──▶ product
```

Coupling is by **direct service injection across packages**, not by events or
published interfaces — e.g. `OrderService` calls `NotificationService` and
`ProductRepository` directly. This keeps it a single transactional monolith; any
future split into services would need these seams broken first (the "Event sourcing"
line in `requirements.md` § Future Extensibility is the intended direction).

## D6. Frontend module layout

Separate repo, `marketplace-frontend/marketplace/`:

```
app/                    # Next.js App Router — pages + ~57 BFF route handlers
├── api/**              # proxy layer to the backend (keeps JWTs in httpOnly cookies)
├── (buyer)            # page.tsx, products/, cart/, checkout/, orders/, wishlist/, profile/
├── seller/             # dashboard, products CRUD
├── admin/             # users, products, orders, analytics, categories
└── login/ register/ verify-email/ auth/callback/
components/             # shared UI (shadcn on @base-ui/react)
hooks/                  # client-side state
lib/                    # api.ts (backendFetch), types.ts
proxy.ts                # shared proxy helper
```

`lib/types.ts` mirrors the backend DTOs by hand — there is no generated client, so
backend DTO changes must be mirrored manually.

