# Item Variants Feature — Plan & Report

> Status: Planned (not yet implemented)
> Last updated: 2026-07-20

## 1. Context & Current State

The marketplace is a Spring Boot 4.1 backend + Next.js 16 / React 19 frontend. Products
are modeled as a flat `Product` with a **single** `price` and `stock`. There is **no**
existing notion of variants, SKUs, options, or attributes on either side — this is a
greenfield feature.

Key facts confirmed:

- **Backend:** `Product` entity (`product/model/Product.java`) has `price` (BigDecimal) +
  `stock` (Integer). Stock is decremented at product granularity via
  `ProductRepository.decrementStock/incrementStock`, called by `CartService` and
  `OrderService`. `CartItem`/`OrderItem` reference `product_id` only (no variant). Flyway
  migrations (`src/main/resources/db/migration/`), DTOs are manual `record`s with `from()`,
  seller isolation via `@permissionService.isOwnerOfProduct(...)`.
- **Frontend:** `lib/types.ts` `Product` has single `price`/`stock`.
  `app/seller/ProductForm.tsx` (create/edit) and `components/ProductDetail.tsx`
  (display + add-to-cart) are the prime UI touch points. Proxy route handlers under
  `app/api/...`.

## 2. Decisions

| Decision | Choice |
|---|---|
| Variant structure | **Simple variants** — a product has a list of variants; each variant has arbitrary attributes (e.g. `"Color": "Red"`, `"Size": "L"`) as a key/value map, plus its own SKU, price, stock. |
| Inventory | **Move to variant-level** — variant is the unit of stock; product-level stock becomes a derived/aggregate (nullable when variants exist). |
| Backward compat | **Support both** — products without variants keep using `product.price`/`product.stock`; products with variants use per-variant price/stock. |
| Scope | **Plan + report** (this document). Implementation deferred. |

## 3. Data Model

### New migration `V23__product_variants.sql`

```sql
CREATE TABLE product_variants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku VARCHAR(100) NOT NULL UNIQUE,
    price DECIMAL(10,2) NOT NULL,
    stock INTEGER NOT NULL DEFAULT 0,
    attributes JSONB NOT NULL DEFAULT '{}'::jsonb,  -- {"Size":"L","Color":"Red"}
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_variants_product ON product_variants(product_id);
```

- `product.stock` retained but **nullable** (keep existing NOT NULL default of 0 for
  backward compat); product-level stock is authoritative only when the product has zero
  variants. New migration `V24__make_product_stock_nullable.sql` to alter the column.
  `product.price` retained as fallback/display default for no-variant products.
- Use `JSONB` for attributes so arbitrary option combos are supported without a separate
  options/values schema (matches "simple variants").

### New entity `ProductVariant.java` (`product/model/`)

- Fields: `id` (UUID), `product` (`@ManyToOne`, FK `product_id`, cascade),
  `sku` (unique, validated), `price` (BigDecimal, `@Positive`),
  `stock` (Integer, `@Min(0)`), `attributes` (mapped via Hibernate
  `@JdbcTypeCode(SqlTypes.JSON)` or a JSON converter), `active`, `sortOrder`, timestamps.
- Add `@OneToMany(mappedBy="product", cascade=ALL, orphanRemoval=true)`
  `List<ProductVariant> variants` to `Product`.

## 4. Backend Changes

| Layer | File | Change |
|---|---|---|
| DTO | `ProductVariantRequest` / `ProductVariantResponse` (records) | `sku`, `price`, `stock`, `attributes` (Map<String,String>), `sortOrder`. Manual `from()`. |
| DTO | `ProductResponse` | Add `List<ProductVariantResponse> variants`; keep `price`/`stock` (resolved to aggregate if variants exist). |
| DTO | `ProductRequest` | Make `price`/`stock` optional; add `List<ProductVariantRequest> variants`. |
| Repo | `ProductVariantRepository` | `JpaRepository<ProductVariant,UUID>` + `findByProductIdOrderBySortOrderAsc`. |
| Repo | `ProductRepository` | Add `decrementVariantStock(variantId, qty)` / `incrementVariantStock(variantId, qty)`; keep product-level ones for no-variant products. |
| Service | `ProductVariantService` (new) | CRUD for variants scoped to seller (reuse `isOwnerOfProduct` check). On create/update, validate SKU uniqueness + at least one variant if product opts into variants. |
| Service | `ProductService` | `createProduct`/`updateProduct` accept optional variant list; when variants present, compute `product.stock` as sum of variant stock for the aggregate badge. Evict caches. |
| Controller | `ProductVariantController` (new) `@RequestMapping("/api/v1/products/{productId}/variants")` | `POST` (create), `GET` (list), `PUT /{variantId}`, `DELETE /{variantId}`; `@PreAuthorize` owner check mirroring `ProductController`. |
| Validation | Jakarta Bean Validation | `@NotBlank` SKU, `@Positive` price, `@Min(0)` stock; reject duplicate SKU within a product. |

## 5. Inventory & Cart/Order Integration (variants move to variant-level)

This is the highest-risk area because stock logic currently keys off `product_id` only.

- **`CartItem`** (`cart/model`): add `variantId UUID` (+ optional `sku` snapshot)
  alongside existing `productId`. Keep `productId` for backward compat.
- **`OrderItem`** (`order/model`): same — add `variantId`/`sku`.
- **`CartService`**: when adding to cart with a `variantId`, call `decrementVariantStock`;
  otherwise `decrementStock` (product-level). Release on remove/expire accordingly.
- **`OrderService`**: on order placement, deduct from variant stock if `variantId` present
  else product stock; restore on cancel. Fire `LOW_STOCK` notification against variant
  stock when variants exist.
- **Admin analytics** `findByStockLessThan` / `lowStockProducts`: when a product has
  variants, derive low-stock from variants.

## 6. Frontend Changes

| Area | File | Change |
|---|---|---|
| Types | `lib/types.ts` | Add `ProductVariant` interface (`id`, `sku`, `price`, `stock`, `attributes`, `isActive`, `sortOrder`); extend `Product` with `variants?`. |
| Seller form | `app/seller/ProductForm.tsx` | Add a dynamic "variants" section (repeatable rows: attributes key/value, SKU, price, stock). When ≥1 variant added, product-level price/stock become optional. Submit variant list in POST/PUT body. |
| Detail | `components/ProductDetail.tsx` | Render variant selectors from `attributes` keys; selecting a variant updates displayed price/stock and the quantity stepper max; pass `variantId` to add-to-cart. |
| Cart | `CartContext.tsx` / `CartItem` type | Carry `variantId`/`sku`; display variant attributes on the line item. |
| API proxy | `app/api/...` | Add `app/api/seller/products/[id]/variants/route.ts` (+ `[variantId]`); proxy to new backend controller. Detail fetch already returns variants via `ProductResponse`. |
| Seller/Admin lists | `ProductList.tsx`, `app/admin/products/page.tsx` | Optionally show variant count; keep showing product-level stock (aggregate) for backward compat. |

## 7. Backward-Compatibility Strategy

- Existing products: untouched, keep `price`/`stock`. No variant rows → backend uses
  product-level stock; cart/order deduct product-level.
- New/edited products with variants: product-level `stock` is recomputed as the sum of
  variant stocks (kept for the listing badge / search filter). `price` falls back to the
  cheapest variant or the product price for display; selection-driven price comes from the
  chosen variant.
- Validation: a product may have **either** product-level stock/price **or** variants, not a
  confusing mix. If variants present, product `price`/`stock` become optional in the request.

## 8. Build Order (recommended phases)

1. **DB + entities + migration** (V23, V24), `ProductVariant` entity, `Product.variants` relation.
2. **Backend DTOs + repository + service + controller** for variant CRUD; wire into `ProductService` create/update.
3. **Inventory refactor** — variant stock methods + cart/order wiring + low-stock notification.
4. **Frontend types + API proxy + seller form**.
5. **Frontend detail + cart** variant selection/display.
6. **Tests** — Flyway migration test (H2 PG mode), variant CRUD service tests, cart/order stock deduction, frontend form/type checks (`npm run lint`, `tsc`).

## 9. Open Risks / Notes

- JSONB mapping with Hibernate on Java 25/Hibernate 7 — verify the JSON converter approach
  (`@JdbcTypeCode` + a `Map<String,String>` or a wrapper type).
- `next@16` has breaking changes (project `AGENTS.md` notes reading
  `node_modules/next/dist/docs/` before coding) — follow that for any new route handlers.
- SKU uniqueness is global (`UNIQUE` constraint); confirm that's acceptable vs. per-product
  uniqueness.
- `product.stock` NULL vs 0 semantics for the existing `findByStockLessThan` admin query
  must be handled (treat NULL as "has variants, check variants").

---

## 10. Status Re-check (2026-07-26)

> Added on a docs audit. **The plan's "Planned (not yet implemented)" status is still
> accurate** — no part of this feature has been built. Sections 1–9 are unchanged and
> remain the design of record. What follows are corrections needed before picking it up.

### 10.1 Still not implemented — confirmed

No `ProductVariant` entity, `product_variants` table, `ProductVariantRepository`,
`ProductVariantService`, `ProductVariantController`, or variant DTOs exist.
`CartItem` and `OrderItem` still reference `product_id` only. `lib/types.ts` has no
`ProductVariant`. §1's "Key facts confirmed" all still hold.

### 10.2 ⚠️ Migration numbers in §3 and §8 are now taken — renumber to `V25`/`V26`

The plan reserves `V23__product_variants.sql` and
`V24__make_product_stock_nullable.sql`. Both numbers were consumed while this plan
sat unimplemented:

| Version | Now occupied by |
|---|---|
| `V23` | `V23__product_discounts.sql` (shipped — see `discount_campaign_plan.md`) |
| `V24` | `V24__add_sold_count_to_products.sql` (shipped) |

Flyway head is **V24**. Use **`V25__product_variants.sql`** and
**`V26__make_product_stock_nullable.sql`**.

### 10.3 New interactions since the plan was written

Two features shipped in the interim that this plan does not account for:

1. **Per-product discounts (`V23`).** `products` now carries `discount_type`,
   `discount_value`, `discount_start`, `discount_end`, and `DiscountService`
   computes the effective price consumed by `CartService` and `OrderService` —
   exactly the code paths §5 wants to change. A decision is now required:
   **does a discount rule apply at product level (all variants) or per variant?**
   Product level is the smaller change and consistent with "one rule per product",
   but `DiscountService.computeEffectivePrice(Product)` takes a `Product`, so
   variant-level pricing means it needs a variant-aware overload. `cart_items`,
   `order_items` and `orders` already have `discount_amount` columns to populate.
2. **`products.sold_count` (`V24`).** Bumped per product at order placement. Decide
   whether variants get their own `sold_count` or the product-level counter stays
   authoritative (the latter is fine and cheaper).

### 10.4 Additions to §5's risk list

- **`ProductResponse` now has three price fields** (`originalPrice`, `discountPrice`,
  `discountActive`) plus `price`/`stock`. Adding variant-resolved pricing on top makes
  four overlapping price concepts in one DTO — worth collapsing deliberately rather
  than appending a fifth.
- **`components/Price.tsx` already exists** (from the discount work) and is the right
  place to absorb variant-selected pricing; §6's frontend table predates it.
- **Optimistic locking:** `products.version` exists. `product_variants` should get its
  own `@Version` column, or concurrent variant stock decrements will conflict on the
  parent row instead of the variant.
- **Cache eviction:** the `products` and `productById` Redis caches (5/10 min TTL) hold
  serialised `ProductResponse` objects. Variant mutations must evict both, or stock
  will read stale for up to 10 minutes.

### 10.5 Unchanged risks worth re-confirming

§9's JSONB-on-Hibernate-7 question, the global-vs-per-product SKU uniqueness
decision, the `next@16` breaking-changes caveat, and the `stock` NULL-vs-0 handling
for `findByStockLessThan` all still stand and are still unresolved.
