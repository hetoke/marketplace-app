-- Shop discount campaign (local per-product discount, single rule per product).
-- One consolidated migration per feature (no one-liner sprawl).
-- All statements are idempotent so a re-run is safe.

-- 1. Per-product discount rule on the products table.
ALTER TABLE products ADD COLUMN IF NOT EXISTS discount_type VARCHAR(20);
ALTER TABLE products ADD COLUMN IF NOT EXISTS discount_value DECIMAL(10,2);
ALTER TABLE products ADD COLUMN IF NOT EXISTS discount_start TIMESTAMP;
ALTER TABLE products ADD COLUMN IF NOT EXISTS discount_end TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_products_discount
    ON products(seller_id, discount_type);

-- 2. Discount audit/display columns on cart and order lines + totals.
ALTER TABLE cart_items ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(10,2);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(10,2);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(10,2);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS original_amount DECIMAL(10,2);

CREATE INDEX IF NOT EXISTS idx_cart_items_discount
    ON cart_items(discount_amount);
CREATE INDEX IF NOT EXISTS idx_orders_discount
    ON orders(discount_amount);
