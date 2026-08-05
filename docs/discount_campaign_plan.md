# Shop Discount Campaign — Plan & Report (Simplified)

> ⚠️ **The status line below is stale. As of 2026-07-26 this feature is IMPLEMENTED**
> (backend fully, frontend partially). The plan is retained verbatim for history;
> see **§10 Implementation Status** at the end for what actually shipped and the
> three items still open.

> Status: Planned (not yet implemented).
> Scope decision: **LOCAL / PER-SHOP discounts only.** One discount rule per product. No global/platform discount. No coupon codes. Kept intentionally simple.

## 1. Decisions

| Decision | Choice |
|---|---|
| Discount model | **Per-product local discount** set by the seller on their own products. |
| Rule count | **Exactly one** discount rule per product (type + value + optional window). |
| Global/platform discount | **Out of scope.** |
| Coupon codes | **None.** |
| Stacking | N/A (single rule per product). |

## 2. Current State (verified)

- `Product.price` (BigDecimal, `DECIMAL(10,2)`) is the only price. Snapshotted into `CartItem.unitPrice` (`CartService` lines 96/103/155) and `OrderItem.unitPrice`/`totalPrice` + `Order.totalAmount` (`OrderService` 132–144). Currency hardcoded VND.
- No discount concept exists anywhere.
- Totals are backend-computed; frontend only renders them. Checkout summary (`app/checkout/page.tsx:215-220`) shows subtotal == grand total.
- Roles: `BUYER, SELLER, ADMIN`. Sellers own their products via `PermissionService.isOwnerOfProduct`.
- Migrations: Flyway, latest **V22**. New → `V23__*.sql`.
- Variants: NOT present. Discounts are product-level only.

## 3. Data Model

### Extend `products` — single local discount rule per product
`V23__product_discounts.sql`:
```sql
ALTER TABLE products
  ADD COLUMN discount_type VARCHAR(20),    -- NULL = no discount; 'PERCENT' | 'FIXED'
  ADD COLUMN discount_value DECIMAL(10,2),  -- % (0-100) for PERCENT, fixed VND for FIXED
  ADD COLUMN discount_start TIMESTAMP,
  ADD COLUMN discount_end   TIMESTAMP;       -- NULL = open-ended
CREATE INDEX idx_products_discount ON products(seller_id, discount_type);
```
- A product has at most one rule: `discount_type` null ⇒ no discount. Active when `type IS NOT NULL AND now BETWEEN start AND end` (both ends optional → always active if only type+value set).
- Validation: `discount_value > 0`; if `PERCENT`, `<= 100`.

### Entity
- `Product.java`: add `discountType` (enum `DiscountType { PERCENT, FIXED }`, nullable), `discountValue` (BigDecimal, nullable), `discountStart` (Instant, nullable), `discountEnd` (Instant, nullable).

## 4. Discount Computation

New `DiscountService` (shared):
- `BigDecimal computeEffectivePrice(Product product)`:
  - if no active local discount → `product.getPrice()`.
  - PERCENT → `price * (1 - value/100)`.
  - FIXED → `max(0, price - value)`.
- `boolean isDiscountActive(Product product)` based on window + type.

### Where to wire
- **`ProductResponse`**: add `originalPrice` (always `product.price`), `discountPrice` (effective, null if none), `discountActive` (boolean).
- **`CartService`** (96/103/155): set `unitPrice` = `discountService.computeEffectivePrice(product)`.
- **`OrderService`** (132–144): `orderItem.unitPrice` = effective price; `order.totalAmount` accumulates correctly.
- **New columns `V23`** (optional, for display/audit): `cart_items.discount_amount DECIMAL(10,2)`, `order_items.discount_amount DECIMAL(10,2)`, `orders.discount_amount DECIMAL(10,2)`, `orders.original_amount DECIMAL(10,2)`.

## 5. Backend Changes

| Layer | File | Change |
|---|---|---|
| Migration | `V23__product_discounts.sql` | ALTER products + optional discount amount columns + index. |
| Entity | `Product.java`, enum `DiscountType` | discount fields. |
| Service | **new `DiscountService`** | `computeEffectivePrice`, `isDiscountActive`. |
| Service | `ProductService` | accept discount fields in create/update (seller-scoped); `ProductResponse.from` sets original/discount prices. |
| Service | `CartService`, `OrderService` | use effective price for `unitPrice`. |
| DTO | `ProductRequest` | add `discountType?`, `discountValue?`, `discountStart?`, `discountEnd?`. |
| DTO | `ProductResponse` | add `originalPrice`, `discountPrice?`, `discountActive`. |
| Validation | Jakarta | percent 0–100, fixed > 0, end > start. |

No new controllers needed — discount fields flow through the existing `ProductController` (seller-owned products already gated by `hasRole('SELLER')` + `isOwnerOfProduct`).

## 6. Frontend Changes

| Area | File | Change |
|---|---|---|
| Types | `lib/types.ts` | `Product`: add `originalPrice`, `discountPrice?`, `discountActive?`. `Cart`/`Order`/`CartItem`/`OrderItem`: add `discountAmount?`, `originalAmount?`. |
| Price helper | new `components/Price.tsx` | strikethrough `originalPrice` + bold `discountPrice` when discounted; else single price. Replace direct `formatCurrency(price)` call sites. |
| Product detail | `ProductDetail.tsx:126` | use `<Price product={product} />`. |
| Listing | `HomepageProducts.tsx:166`, `SellerProfileClient.tsx:244` | use `<Price>`. |
| Cart | `app/cart/page.tsx` | show per-line discount + discount summary line. |
| Checkout | `app/checkout/page.tsx:215-220` | "Giảm giá" line using `cart.discountAmount`. |
| Order detail | `app/orders/[orderId]/page.tsx`, `app/orders/page.tsx` | show discount line if present. |
| Seller mgmt | `app/seller/ProductForm.tsx` | add discount type/value/start/end inputs (one rule per product). |

No admin discounts page (global out of scope).

## 7. Example
Product price 1,000,000₫. Local rule PERCENT 10% → effective 900,000₫. `discountAmount` = 100,000₫. Cart/order charge 900,000₫.

## 8. Build Order

1. Migration `V23` + `Product` discount fields + `DiscountType` enum.
2. `DiscountService`.
3. `ProductResponse`/`ProductRequest` discount fields; `ProductService` create/update.
4. `CartService`/`OrderService` effective-price wiring + discount amount columns.
5. Frontend types + `<Price>` helper + display sites.
6. Seller `ProductForm` discount inputs.
7. Tests — `DiscountService` unit (windows, percent/fixed), cart/order effective-price, migration test (H2 PG mode), frontend `tsc`/`lint`.

## 9. Open Risks / Notes

- Variants not built → product-level discounts only.
- Discount evaluated at add-to-cart and order-placement; order price frozen at placement time.
- Refunds restore stock only; discount amount is historical on the order.

---

## 10. Implementation Status

> Added 2026-07-26. Verified against the codebase. **Supersedes the "Planned (not yet
> implemented)" header and §2's "No discount concept exists anywhere"** — both were
> accurate when written and are now stale. Sections 1–9 are unchanged.

### 10.1 Shipped as planned

**Database** — `V23__product_discounts.sql` applied. `products` gained
`discount_type`, `discount_value`, `discount_start`, `discount_end`; the optional
audit columns were taken up too: `cart_items.discount_amount`,
`order_items.discount_amount`, `orders.discount_amount`, `orders.original_amount`.

**Backend** — all of §5 landed:

| Planned | As built |
|---|---|
| `DiscountType` enum | `product/model/DiscountType.java` — `PERCENT`, `FIXED` |
| `Product` discount fields | `product/model/Product.java:48-59` |
| `DiscountService` | `product/service/DiscountService.java` |
| `ProductResponse` additions | `originalPrice`, `discountPrice`, `discountActive` (lines 27-29) |
| `ProductRequest` additions | discount type/value/start/end, seller-scoped |
| `CartService` effective price | `cart_items.unit_price` = discounted price at add time |
| `OrderService` effective price | `order_items.unit_price` + `orders.original_amount` / `discount_amount` |
| No new controllers | Confirmed — fields flow through `ProductController`, gated by `hasRole('SELLER')` + `isOwnerOfProduct` |

**Frontend** — three of the seven touch points in §6:

- `components/Price.tsx` — the `<Price>` helper exists as designed
- `lib/types.ts` — `originalPrice` / `discountPrice` / `discountActive` present
- `app/seller/ProductForm.tsx` — seller discount inputs present

### 10.2 Still open

1. **Cart / checkout / order discount display was not done.** `discountAmount` and
   `originalAmount` do not appear in `app/cart/page.tsx`, `app/checkout/page.tsx`,
   `app/orders/page.tsx` or `app/orders/[orderId]/page.tsx`. The backend returns both
   and charges correctly, but the buyer never sees a "Giảm giá" line — the summary
   still renders subtotal == grand total. This is the main user-visible gap.
2. **No `DiscountServiceTest`.** Step 7 of the build order is unmet: nothing covers
   window boundaries, `PERCENT` vs `FIXED`, or the cart/order effective-price wiring.
3. **`<Price>` is not used everywhere.** Verify the `ProductDetail.tsx`,
   `HomepageProducts.tsx` and `SellerProfileClient.tsx` call sites named in §6 were
   actually migrated off direct `formatCurrency(price)`.

### 10.3 Corrections to the plan's own text

- §2 claimed "Migrations: Flyway, latest **V22**. New → `V23__*.sql`." That held —
  `V23` is this feature. The migration head is now **V24** (`sold_count`).
- §2's "Variants: NOT present" is still accurate; see `item_variants_plan.md`.
- Note that `item_variants_plan.md` also reserves `V23`/`V24`. Both numbers are now
  taken by this feature and `sold_count` — that plan must renumber to `V25+`.
