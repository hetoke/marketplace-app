## 🧱 Core Entities & Relationships (Data Model)


### 1. **User**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `email` | String (unique) | Email address |
| `password_hash` | String | Encrypted password |
| `role` | Enum (`BUYER`, `SELLER`, `ADMIN`) | User role |
| `is_verified` | Boolean | Email verification status |
| `mfa_enabled` | Boolean | MFA enabled |
| `mfa_methods` | JSON[] | List of MFA methods |
| `display_name` | String | Name from IdP |
| `profile_picture_url` | String | Profile image |
| `last_login_at` | Timestamp | Last login |
| `authentication_type` | Enum (`LOCAL`, `OIDC`, `HYBRID`) | Auth type |
| `default_idp_id` | UUID (FK) | Preferred IdP |
| `recovery_codes` | String[] | Encrypted backup codes |
| `trusted_devices` | JSON | Trusted device tokens |
| `created_at` | Timestamp | Created at |
| `updated_at` | Timestamp | Last updated |

---

### 2. **IdentityProvider**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `name` | String | Provider name (Google, Auth0, etc.) |
| `provider_type` | Enum | Type of provider |
| `issuer_url` | String | OIDC issuer |
| `client_id` | String | OIDC client ID |
| `client_secret` | Encrypted String | Encrypted secret |
| `enabled` | Boolean | Active status |
| `created_at` | Timestamp | Created at |

---

### 3. **UserIdentity**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `user_id` | UUID (FK) | Internal user |
| `provider_id` | UUID (FK) | Identity provider |
| `subject` | String | OIDC `sub` claim |
| `email` | String | Email from provider |
| `name` | String | Display name |
| `picture_url` | String | Profile picture |
| `claims` | JSON | Full OIDC claims |
| `linked_at` | Timestamp | Account linked |
| `last_sync_at` | Timestamp | Last synced |

---

### 4. **OAuthClient**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `client_id` | String | OAuth client ID |
| `client_secret` | Encrypted String | Encrypted secret |
| `name` | String | App name |
| `client_type` | Enum (`CONFIDENTIAL`, `PUBLIC`) | Client type |
| `grant_types` | JSON | Allowed grants |
| `redirect_uris` | JSON | Allowed URIs |
| `scopes` | JSON | Allowed scopes |
| `pkce_required` | Boolean | PKCE enforcement |
| `active` | Boolean | Enabled |
| `created_at` | Timestamp | Created at |

---

### 5. **SellerProfile**
| Field | Type | Description |
|-------|------|-------------|
| `user_id` | UUID (FK) | Reference to User |
| `store_name` | String | Store name |
| `bio` | Text | Description |
| `logo_url` | String | Logo image |
| `payout_method` | JSON | Payout preferences |
| `rating` | Float | Average rating |
| `total_sales` | Integer | Number of sales |
| `joined_at` | Timestamp | Joined date |

---

### 6. **BuyerProfile**
| Field | Type | Description |
|-------|------|-------------|
| `user_id` | UUID (FK) | Reference to User |
| `wishlist` | UUID[] | Product IDs |
| `saved_payment_methods` | JSON | Encrypted payment info |
| `preferred_currency` | String | Default currency |
| `notifications_enabled` | Boolean | Receive notifications |

---

### 7. **Product**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `seller_id` | UUID (FK) | Seller |
| `name` | String | Product name |
| `description` | Text | Description |
| `price` | Decimal | Price |
| `currency` | String | Currency code |
| `category_id` | UUID (FK) | Category |
| `tags` | String[] | Searchable tags |
| `images` | String[] | Image URLs |
| `inventory_count` | Integer | Available quantity |
| `low_stock_alert` | Boolean | Notify when low |
| `is_active` | Boolean | Published or draft |
| `created_at` | Timestamp | Created |
| `updated_at` | Timestamp | Updated |

---

### 8. **Category**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `name` | String | Category name |
| `slug` | String | URL-friendly name |
| `parent_id` | UUID (FK) | Parent category |
| `description` | Text | Optional description |

---

### 9. **Order**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `buyer_id` | UUID (FK) | Buyer |
| `items` | JSON | Product list |
| `total_amount` | Decimal | Total |
| `currency` | String | Currency |
| `status` | Enum (`PENDING`, `CONFIRMED`, `DELIVERY`, `COMPLETE`, `CANCELLED`) | Order status |
| `payment_status` | Enum (`PAID`, `PENDING`, `REFUNDED`) | Payment status |
| `payment_method` | String | Payment method |
| `shipping_address` | JSON | Delivery address |
| `placed_at` | Timestamp | Order placed |
| `delivered_at` | Timestamp | Delivered |
| `cancelled_at` | Timestamp | Cancelled |
| `return_requested` | Boolean | Return requested |
| `return_reason` | Text | Return reason |

---

### 10. **Payment**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `order_id` | UUID (FK) | Order |
| `amount` | Decimal | Amount |
| `currency` | String | Currency |
| `method` | String | Payment method |
| `status` | Enum (`SUCCESS`, `FAILED`, `PENDING`) | Status |
| `transaction_id` | String | Gateway ID |
| `fees` | Decimal | Fees |
| `processed_at` | Timestamp | Processed at |

---

### 11. **Review**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `product_id` | UUID (FK) | Product |
| `buyer_id` | UUID (FK) | Reviewer |
| `rating` | Integer (1-5) | Rating |
| `comment` | Text | Comment |
| `verified_purchase` | Boolean | Verified purchase |
| `created_at` | Timestamp | Posted at |

---

### 12. **Notification**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `user_id` | UUID (FK) | Recipient |
| `type` | Enum (`ORDER_UPDATE`, `SECURITY_ALERT`, `PROMOTIONAL`) | Type |
| `message` | Text | Content |
| `is_read` | Boolean | Read status |
| `created_at` | Timestamp | Sent at |

---

### 13. **Cart**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `user_id` | UUID (FK) | User (nullable for guest) |
| `status` | Enum (`ACTIVE`, `ABANDONED`, `CONVERTED`) | Cart status |
| `created_at` | Timestamp | Created |
| `updated_at` | Timestamp | Updated |
| `expires_at` | Timestamp | Guest cart expiry |

---

### 14. **CartItem**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `cart_id` | UUID (FK) | Cart |
| `product_id` | UUID (FK) | Product |
| `quantity` | Integer | Quantity |
| `unit_price` | Decimal | Price at add time |
| `total_price` | Decimal | Quantity × unit_price |
| `added_at` | Timestamp | Added at |

---

### 15. **Inventory**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `product_id` | UUID (FK) | Product |
| `quantity` | Integer | Total stock |
| `reserved_quantity` | Integer | Reserved stock |
| `available_quantity` | Integer | Available |
| `low_stock_threshold` | Integer | Alert threshold |
| `last_updated` | Timestamp | Last updated |

---

### 16. **SupportTicket**
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier |
| `user_id` | UUID (FK) | Creator |
| `subject` | String | Subject |
| `description` | Text | Issue description |
| `status` | Enum (`OPEN`, `IN_PROGRESS`, `RESOLVED`) | Ticket status |
| `priority` | Enum (`LOW`, `MEDIUM`, `HIGH`) | Priority |
| `assigned_to` | UUID (FK) | Admin |
| `created_at` | Timestamp | Created |
| `resolved_at` | Timestamp | Resolved |

---

## 🔗 Core Relationships

```plaintext
[User] 1--1 [SellerProfile]
[User] 1--1 [BuyerProfile]
[User] 1--* [Cart]
[Cart] 1--* [CartItem]
[CartItem] *--1 [Product]
[User] 1--* [Order]
[Order] 1--* [OrderItem]
[Order] 1--1 [Payment]
[Product] *--1 [Category]
[Product] 1--1 [Inventory]
[Product] 1--* [Review]
[Order] 1--* [Notification]
[User] 1--* [SupportTicket]
```

---

# 🏗️ As-Built Schema

> Added 2026-07-26. Verified against `src/main/java/com/marketplace/**/model/*.java`
> and `src/main/resources/db/migration/` (Flyway V1–V24).
> The design model above is retained unchanged; this section records the schema the
> application actually validates against (`spring.jpa.hibernate.ddl-auto: validate`,
> so these tables must match exactly at boot).

## B0. Design vs. as-built at a glance

**Design entities that were never built:**

| Design entity | What happened instead |
|---|---|
| `IdentityProvider` | No table. OIDC is static config: `app.oidc.google.*` in `application.yml`. Single provider (Google). |
| `OAuthClient` | No table. The app is not an OAuth authorization server — it is only an OIDC *client*. |
| `SellerProfile` | No table. "Seller" is `users.role = SELLER`; store identity is the user's `display_name` / `profile_picture_url`. |
| `BuyerProfile` | No table. Wishlist became its own `wishlist_items` table; the other fields were dropped. |
| `Inventory` | Collapsed into a single `products.stock` integer. No reserved/available split. |
| `Transaction` | Collapsed into `payments`. |
| `Refund` | Collapsed into `payments` (`REFUND_REQUESTED`/`REFUNDED` status + `refunded_at`). |
| `AnalyticsReport` | Not persisted — analytics are computed on read and Redis-cached for 5 min. |
| `SupportTicket` | Not built. Module does not exist. |

**As-built tables with no design counterpart:** `order_items`, `product_images`,
`wishlist_items`, `refresh_tokens`, `verification_tokens`, `user_identities`,
`mfa_challenges`, `recovery_codes`, `images`, `upload_sessions`, `admin_action_log`.

**Cross-cutting differences from the design:**

- **Currency is effectively fixed to `VND`.** The `currency` column exists but
  defaults to `VND` (`V13__fix_currency_default_to_vnd.sql`). All USD examples in
  these docs are design-era artifacts.
- **Addresses are Vietnam-shaped** (`street` / `province` / `district` / `ward`), not
  the `street/city/state/zipCode/country` JSON in the design. On `users` they are
  four flat `default_*` columns; on `orders` the resolved address is a single string.
- **No JSON/array columns.** The design's `mfa_methods JSON[]`, `recovery_codes String[]`,
  `trusted_devices JSON`, `tags String[]`, `images String[]`, `items JSON`,
  `shipping_address JSON` all became either scalar columns or child tables.
- **Optimistic locking** via `@Version` on `products`, `carts`, `orders`, `payments`,
  `reviews` (`V9`, `V15`). Not in the design.
- **Denormalised counters** on `products`: `average_rating`, `review_count`, `sold_count`.
- **`trusted_devices` was never built** — there is no device-tracking table anywhere.

## B1. `users` — `user/model/User.java`

| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `email` | varchar unique NOT NULL | |
| `password_hash` | varchar NOT NULL | BCrypt |
| `role` | varchar(20) NOT NULL | `BUYER \| SELLER \| ADMIN`, default `BUYER` |
| `is_verified` | boolean NOT NULL | default false |
| `status` | varchar(20) NOT NULL | `ACTIVE \| SUSPENDED \| DEACTIVATED` — **new, not in design** |
| `mfa_enabled` | boolean NOT NULL | default false |
| `display_name` | varchar | |
| `profile_picture_url` | varchar(512) | |
| `default_street` | varchar(500) | **new** |
| `default_province` | varchar(255) | **new** |
| `default_district` | varchar(255) | **new** |
| `default_ward` | varchar(255) | **new** |
| `authentication_type` | varchar(20) NOT NULL | `LOCAL \| OIDC \| HYBRID`, default `LOCAL` |
| `created_at` / `updated_at` | timestamp NOT NULL | |

Design columns **not** present: `mfa_methods`, `default_idp_id`, `recovery_codes`
(→ `recovery_codes` table), `trusted_devices`, `last_login_at` (added in `V2` but not
mapped on the entity).

## B2. `user_identities` — `user/model/UserIdentity.java`

| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `user_id` | UUID FK → users | `@ManyToOne` |
| `provider` | varchar(30) NOT NULL | `GOOGLE \| FACEBOOK \| GITHUB` (only GOOGLE is wired) |
| `provider_user_id` | varchar NOT NULL | the OIDC `sub` claim |
| `email` | varchar | |
| `display_name` | varchar | |
| `profile_picture_url` | varchar(512) | |
| `created_at` / `updated_at` | timestamp NOT NULL | |

Unique constraint on (`provider`, `provider_user_id`). The design's `claims JSON`,
`linked_at`, `last_sync_at`, and `provider_id` FK are not present — provider is an
enum, not a row.

## B3. Auth support tables

**`refresh_tokens`** — `id` UUID PK, `user_id` FK, `token` varchar unique NOT NULL,
`expires_at`, `created_at`.

**`verification_tokens`** — `id` UUID PK, `user_id` FK, `token` varchar unique NOT NULL,
`expires_at`, `created_at`. Used for both email verification and password reset.

**`mfa_challenges`** (`V21`) — `id` UUID PK, `user_id` FK, `code_hash` NOT NULL,
`type` varchar(20) `SETUP \| LOGIN \| DISABLE`, `expires_at`, `created_at`.
Email OTP only; codes are hashed, never stored in clear.

**`recovery_codes`** (`V21`) — `id` UUID PK, `user_id` FK, `code_hash` NOT NULL,
`used` boolean NOT NULL default false, `used_at`, `created_at`. Replaces the design's
`users.recovery_codes String[]`; single-use, hashed.

## B4. `products` — `product/model/Product.java`

| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `seller_id` | UUID NOT NULL | plain column, not a mapped FK |
| `category_id` | UUID FK → categories NOT NULL | `@ManyToOne` |
| `name` | varchar(255) NOT NULL | |
| `slug` | varchar(255) unique NOT NULL | **new** |
| `description` | TEXT | |
| `price` | decimal(10,2) NOT NULL | |
| `stock` | integer NOT NULL | default 0 — replaces the whole `Inventory` entity |
| `discount_type` | varchar(20) | `PERCENT \| FIXED`, NULL = none (`V23`) |
| `discount_value` | decimal(10,2) | |
| `discount_start` | timestamp | |
| `discount_end` | timestamp | NULL = open-ended |
| `average_rating` | double | default 0.0, maintained by `ReviewService` |
| `review_count` | integer NOT NULL | default 0 |
| `sold_count` | integer NOT NULL | default 0 (`V24`) |
| `is_active` | boolean NOT NULL | default true |
| `created_at` / `updated_at` | timestamp NOT NULL | |
| `version` | integer NOT NULL | `@Version` |

Design columns not present: `currency` (fixed VND at order level), `tags String[]`,
`images String[]` (→ `product_images`), `low_stock_alert` (low stock is a threshold
check in code, not a flag).

## B5. `product_images` — `product/model/ProductImage.java`

`id` UUID PK, `product_id` FK NOT NULL, `url` varchar(512) NOT NULL,
`alt_text` varchar(255), `sort_order` integer NOT NULL default 0,
`is_primary` boolean NOT NULL default false, `created_at`.
Max 10 per product (`app.upload.max-images-per-product`).

## B6. `categories` — `product/model/Category.java`

`id` UUID PK, `name` varchar(255) NOT NULL, `description` TEXT,
`slug` varchar(255) unique NOT NULL, `parent_id` FK → categories (self-referencing,
nullable), `is_active` boolean NOT NULL default true, `created_at`, `updated_at`.
Adds `is_active` and timestamps over the design.

## B7. `carts` / `cart_items`

**`carts`** — `id` UUID PK, `user_id` UUID **NOT NULL**, `status` varchar(20)
`ACTIVE \| ABANDONED \| CONVERTED` default `ACTIVE`, `created_at`, `updated_at`,
`version` bigint (`V15`).

> The design has `user_id` nullable for guest carts and an `expires_at` column.
> Neither is as-built: **`user_id` is NOT NULL and there is no `expires_at`.**
> Guest carts do not exist. `CartExpirationService` sweeps stale *authenticated*
> carts on a schedule instead.

**`cart_items`** — `id` UUID PK, `cart_id` FK NOT NULL (`@ManyToOne`),
`product_id` UUID NOT NULL, `quantity` integer NOT NULL default 1,
`unit_price` decimal(10,2) NOT NULL, `discount_amount` decimal(10,2) (`V23`),
`added_at`. Unique constraint on (`cart_id`, `product_id`).
`total_price` from the design is **not stored** — it is derived.

`unit_price` is the *discounted* effective price at add time, computed by
`DiscountService.computeEffectivePrice(product)`.

## B8. `orders` / `order_items`

**`orders`** — `order/model/Order.java`

| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `buyer_id` | UUID NOT NULL | |
| `status` | varchar NOT NULL | see enum below |
| `payment_status` | varchar NOT NULL | `PENDING \| PAID \| REFUND_REQUESTED \| REFUNDED` |
| `total_amount` | decimal NOT NULL | after discount |
| `discount_amount` | decimal | `V23` |
| `original_amount` | decimal | `V23`, pre-discount total |
| `currency` | varchar | default `VND` |
| `shipping_address` | varchar/text | flattened string, **not JSON** |
| `placed_at` | timestamp NOT NULL | |
| `delivered_at` | timestamp | |
| `cancelled_at` | timestamp | |
| `cancel_reason` | text | **new** |
| `return_requested` | boolean NOT NULL | default false |
| `return_reason` | text | |
| `return_requested_at` | timestamp | **new** |
| `created_at` / `updated_at` | timestamp NOT NULL | |
| `version` | bigint | `@Version` |

⚠️ **`OrderStatus` differs from every prose description in these docs.** As-built:

```
PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED, RETURN_REQUESTED, RETURNED
```

The documented `PENDING → CONFIRMED → DELIVERY → COMPLETE` values `DELIVERY` and
`COMPLETE` **do not exist**. The real happy path is
`PENDING → CONFIRMED → SHIPPED → DELIVERED`, with `CANCELLED` reachable from
`PENDING`/`CONFIRMED` and `RETURN_REQUESTED → RETURNED` after delivery.
Transitions are validated in `OrderService`; `CANCELLED` and `RETURNED` are terminal.
See `docs/diagrams/state-order-payment.mmd`.

**`order_items`** — `id` UUID PK, `order_id` FK NOT NULL, `product_id` UUID NOT NULL,
`product_name` varchar NOT NULL, `product_image_url` varchar,
`unit_price` decimal(10,2) NOT NULL, `quantity` integer NOT NULL,
`total_price` decimal(10,2) NOT NULL, `discount_amount` decimal(10,2), `created_at`.
Name and image are **snapshotted** at order time so history survives product edits.
This table replaces the design's `orders.items JSON`.

## B9. `payments` — `payment/model/Payment.java`

| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `order_id` | UUID NOT NULL | |
| `amount` | decimal NOT NULL | |
| `currency` | varchar | default `VND` |
| `method` | varchar NOT NULL | `CREDIT_CARD \| SEPAY` |
| `status` | varchar NOT NULL | `PENDING \| COMPLETED \| FAILED \| REFUND_REQUESTED \| REFUNDED` |
| `card_last_four` | varchar | **new**, mock card path |
| `card_brand` | varchar | **new** |
| `provider_ref` | varchar | **new**, gateway transaction reference |
| `invoice_number` | varchar | **new** (`V12`) |
| `failure_reason` | varchar | **new** |
| `refunded_at` | timestamp | **new** |
| `created_at` / `updated_at` | timestamp NOT NULL | |
| `version` | bigint | `@Version` |

Differences from design: status is `COMPLETED`, **not** `SUCCESS`, and adds the two
refund states; there is no `fees` column and no `processed_at` (use `updated_at`);
`transaction_id` is named `provider_ref`. `V14__cleanup_duplicate_payments.sql`
de-duplicated rows created before idempotency handling was added.

## B10. `reviews` — `review/model/Review.java`

`id` UUID PK, `product_id` UUID NOT NULL, `buyer_id` UUID NOT NULL,
`rating` integer NOT NULL (1–5), `comment` TEXT,
`verified_purchase` boolean NOT NULL default false, `created_at`, `updated_at`,
`version` bigint. Unique constraint on (`product_id`, `buyer_id`) — one review per
buyer per product. `V19` added the rollup columns to `products`.

## B11. `notifications` — `notification/model/Notification.java`

`id` UUID PK, `user_id` UUID NOT NULL, `type` varchar(30) NOT NULL,
`title` varchar(255) NOT NULL, `message` TEXT NOT NULL,
`reference_id` UUID, `reference_type` varchar(50), `is_read` boolean NOT NULL
default false, `created_at`.

`NotificationType` as-built — the design listed only the first, third and fourth:

```
ORDER_UPDATE, PAYMENT_UPDATE, SECURITY_ALERT, PROMOTIONAL, LOW_STOCK
```

`title`, `reference_id` and `reference_type` (deep-linking to the order/payment that
triggered the notification) are new.

## B12. `wishlist_items` — `wishlist/model/WishlistItem.java`

`id` UUID PK, `user_id` UUID NOT NULL, `product_id` UUID NOT NULL, `added_at`.
Unique constraint on (`user_id`, `product_id`). Replaces the design's
`BuyerProfile.wishlist UUID[]`.

## B13. Media tables — `upload/model/`

**`images`** — `id` UUID PK, `file_url` varchar(1024) NOT NULL, `file_name` varchar(255),
`file_size` bigint, `content_type` varchar(100), `entity_type` varchar(20) NOT NULL
(`USER \| PRODUCT`), `entity_id` UUID NOT NULL, `uploaded_by` UUID NOT NULL, `created_at`.
Polymorphic by (`entity_type`, `entity_id`).

**`upload_sessions`** (`V7`) — `id` UUID PK, `entity_type`, `entity_id`, `uploaded_by`,
`file_name` NOT NULL, `file_size`, `content_type`, `storage_path` varchar(1024) NOT NULL,
`supabase_token` varchar(512), `status` varchar(20) (`PENDING \| COMPLETED \| FAILED \| EXPIRED`),
`expires_at` NOT NULL, `created_at`, `updated_at`.

Two-phase upload: a `PENDING` session is created with a signed Supabase token, the
client uploads directly to Supabase, and the storage webhook flips the session to
`COMPLETED` and inserts the `images` row. `V10__fix_bytea_column_types.sql` corrected
columns Hibernate had inferred as `bytea`.

## B14. `admin_action_log` — `admin/model/AdminActionLog.java` (`V22`)

`id` UUID PK, `admin_id` UUID NOT NULL, `action` varchar(50) NOT NULL
(`USER_STATUS_CHANGE \| PRODUCT_STATUS_CHANGE`), `target_type` varchar(50) NOT NULL,
`target_id` UUID NOT NULL, `details` TEXT, `created_at`.
Satisfies the "Audit Logging" line in Sprint 9; not in the design model.

## B15. As-built relationships

```plaintext
[User] 1--* [UserIdentity]        (provider is an enum, not a table)
[User] 1--* [RefreshToken]
[User] 1--* [VerificationToken]
[User] 1--* [MFAChallenge]
[User] 1--* [RecoveryCode]
[User] 1--1 [Cart]                (ACTIVE cart per user; user_id NOT NULL)
[Cart] 1--* [CartItem]            (unique per cart+product)
[CartItem] *--1 [Product]         (by id, not a mapped FK)
[User] 1--* [Order]               (by buyer_id)
[Order] 1--* [OrderItem]
[Order] 1--* [Payment]            (1--* in practice: retries/refunds create rows)
[Product] *--1 [Category]
[Category] *--1 [Category]        (self-referencing parent)
[Product] 1--* [ProductImage]
[Product] 1--* [Review]           (unique per product+buyer)
[User] 1--* [WishlistItem]
[User] 1--* [Notification]        (by user_id — NOT [Order] 1--* [Notification])
[User] 1--* [AdminActionLog]      (by admin_id)
[Image]/[UploadSession] *--1 (USER|PRODUCT)  polymorphic by entity_type+entity_id

no SellerProfile / BuyerProfile / IdentityProvider / OAuthClient / Inventory /
Transaction / Refund / AnalyticsReport / SupportTicket
```

## B16. Migration history

Flyway, `classpath:db/migration`, `baseline-on-migrate: true`, `ddl-auto: validate`.
Current head is **V24**. Tests run on H2 (`MODE=PostgreSQL`) with Flyway disabled and
`ddl-auto: create-drop`, so migrations are **not** exercised by the unit test suite.

| | |
|---|---|
| `V1` | baseline schema |
| `V2` | add `last_login_at` |
| `V3` | product catalog |
| `V4` | images table |
| `V5` | cart and wishlist |
| `V6` | orders |
| `V7` | upload sessions |
| `V8` | payments |
| `V9` | version columns (optimistic locking) |
| `V10` | fix bytea column types |
| `V11` | seed admin and categories |
| `V12` | add `invoice_number` to payments |
| `V13` | fix currency default to VND |
| `V14` | cleanup duplicate payments |
| `V15` | add `version` to carts |
| `V16` | add user address |
| `V17` | reviews |
| `V18` | notifications |
| `V19` | add review fields to products |
| `V20` | OIDC identity tables |
| `V21` | MFA tables |
| `V22` | admin action log |
| `V23` | product discounts |
| `V24` | add `sold_count` to products |

`V11` seeds the admin user (`ADMIN_EMAIL` / `ADMIN_PASSWORD`, defaulting to
`admin@marketplace.com` / `Admin123!`) plus base categories; `DataSeeder` handles
development data.

## B17. Planned schema changes (not yet applied)

- `product_variants` + variant-level stock — see `docs/item_variants_plan.md`.
  Status still accurate: **not implemented.** Note that plan reserves `V23`/`V24`,
  which are now taken; renumber to `V25+`.

