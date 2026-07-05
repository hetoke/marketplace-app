ALTER TABLE products ADD COLUMN average_rating DOUBLE PRECISION;
ALTER TABLE products ADD COLUMN review_count INTEGER NOT NULL DEFAULT 0;

UPDATE products SET average_rating = 0.0;
