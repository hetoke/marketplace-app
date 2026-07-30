ALTER TABLE cart_items ADD COLUMN variant_id UUID;
ALTER TABLE cart_items ADD COLUMN sku VARCHAR(100);
ALTER TABLE cart_items DROP CONSTRAINT IF EXISTS uk_cart_items_cart_product;
ALTER TABLE cart_items ADD CONSTRAINT uk_cart_items_cart_product_variant UNIQUE (cart_id, product_id, variant_id);

ALTER TABLE order_items ADD COLUMN variant_id UUID;
ALTER TABLE order_items ADD COLUMN sku VARCHAR(100);
