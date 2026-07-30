ALTER TABLE reviews ADD COLUMN variant_id UUID REFERENCES product_variants(id) ON DELETE SET NULL;
CREATE INDEX idx_reviews_variant ON reviews(variant_id);
