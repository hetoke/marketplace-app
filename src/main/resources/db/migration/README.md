# Database Migrations

Managed by **Flyway** (PostgreSQL). Migrations run in `validate` mode, so applied
files (`V1`–`V22`) must **never** be edited, renamed, or deleted — their checksums
are stored in `flyway_schema_history` and any change breaks startup.

## Policy (keeps the folder clean — stops the old one-liner sprawl)

- **One feature / logical change = ONE migration file.** Do not create a new
  migration for every single `ADD COLUMN`.
- Use **idempotent guards**: `ADD COLUMN IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`,
  and guard `UPDATE`s so a re-run is safe.
- Name: `V<nn>__<kebab_description>.sql`. Keep descriptions domain-based
  (`product_discounts`, `cart_discounts`), not action-based (`add_x_to_y`).
- Group related tables in one file (e.g. all `products` columns together).
- New migrations continue from the latest version. Current latest: **V22**.

## Migration index (grouped by domain)

### Users & auth (`users`, `refresh_tokens`, `verification_tokens`, MFA, OIDC)
- `V1__baseline_schema.sql` — users, refresh_tokens, verification_tokens (+ pgcrypto).
- `V2__add_last_login_at.sql` — `users.last_login_at`.
- `V9__add_version_columns.sql` — `@Version` on orders/payments.
- `V15__add_version_to_carts.sql` — `@Version` on carts.
- `V16__add_user_address.sql` — user address fields.
- `V20__oidc_identity_tables.sql` — OIDC identities.
- `V21__mfa_tables.sql` — MFA secrets/recovery.
- `V22__admin_action_log.sql` — admin audit log + `users.status`.

### Catalog (`categories`, `products`, `product_images`)
- `V3__product_catalog.sql` — categories, products, product_images.
- `V19__add_review_fields_to_products.sql` — `average_rating`, `review_count`.

### Cart, wishlist, orders
- `V5__cart_and_wishlist.sql` — carts, cart_items, wishlist.
- `V6__orders.sql` — orders, order_items.
- `V17__reviews.sql` — reviews.

### Payments
- `V8__payments.sql` — payments.
- `V12__add_invoice_number_to_payments.sql` — invoice number.
- `V13__fix_currency_default_to_vnd.sql` — currency → VND default + backfill.
- `V14__cleanup_duplicate_payments.sql` — dedupe PENDING payments.

### Media & notifications
- `V4__add_images_table.sql` — upload sessions / images.
- `V7__upload_sessions.sql` — upload sessions.
- `V10__fix_bytea_column_types.sql` — bytea type fixes.
- `V18__notifications.sql` — notifications.

### Seed data
- `V11__seed_admin_and_categories.sql` — admin user + default categories.

## Going forward (consolidated)
- `V23__product_discounts.sql` — single, tidy migration for the shop discount feature
  (per-product discount rule on `products` + discount amount columns on cart/order).
- Future features each get one consolidated file following the policy above.
