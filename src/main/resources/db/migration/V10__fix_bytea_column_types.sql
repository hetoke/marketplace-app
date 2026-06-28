-- Fix columns that Hibernate 7.x ddl-auto:create incorrectly mapped to bytea
-- V10 fixes all String columns that should be varchar/text but are bytea

-- users table
ALTER TABLE users ALTER COLUMN email TYPE VARCHAR(255) USING email::text;
ALTER TABLE users ALTER COLUMN password_hash TYPE VARCHAR(255) USING password_hash::text;
ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(20) USING role::text;
ALTER TABLE users ALTER COLUMN authentication_type TYPE VARCHAR(20) USING authentication_type::text;
ALTER TABLE users ALTER COLUMN display_name TYPE VARCHAR(255) USING display_name::text;
ALTER TABLE users ALTER COLUMN profile_picture_url TYPE VARCHAR(512) USING profile_picture_url::text;

-- refresh_tokens table
ALTER TABLE refresh_tokens ALTER COLUMN token TYPE VARCHAR(512) USING token::text;

-- verification_tokens table
ALTER TABLE verification_tokens ALTER COLUMN token TYPE VARCHAR(512) USING token::text;

-- categories table
ALTER TABLE categories ALTER COLUMN name TYPE VARCHAR(255) USING name::text;
ALTER TABLE categories ALTER COLUMN slug TYPE VARCHAR(255) USING slug::text;
ALTER TABLE categories ALTER COLUMN description TYPE TEXT USING description::text;

-- products table
ALTER TABLE products ALTER COLUMN name TYPE VARCHAR(255) USING name::text;
ALTER TABLE products ALTER COLUMN slug TYPE VARCHAR(255) USING slug::text;
ALTER TABLE products ALTER COLUMN description TYPE TEXT USING description::text;

-- product_images table
ALTER TABLE product_images ALTER COLUMN url TYPE VARCHAR(512) USING url::text;
ALTER TABLE product_images ALTER COLUMN alt_text TYPE VARCHAR(255) USING alt_text::text;

-- images table
ALTER TABLE images ALTER COLUMN file_url TYPE VARCHAR(1024) USING file_url::text;
ALTER TABLE images ALTER COLUMN file_name TYPE VARCHAR(255) USING file_name::text;
ALTER TABLE images ALTER COLUMN content_type TYPE VARCHAR(100) USING content_type::text;
ALTER TABLE images ALTER COLUMN entity_type TYPE VARCHAR(20) USING entity_type::text;

-- carts table
ALTER TABLE carts ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

-- orders table
ALTER TABLE orders ALTER COLUMN status TYPE VARCHAR(20) USING status::text;
ALTER TABLE orders ALTER COLUMN payment_status TYPE VARCHAR(20) USING payment_status::text;
ALTER TABLE orders ALTER COLUMN currency TYPE VARCHAR(3) USING currency::text;
ALTER TABLE orders ALTER COLUMN cancel_reason TYPE TEXT USING cancel_reason::text;
ALTER TABLE orders ALTER COLUMN return_reason TYPE TEXT USING return_reason::text;

-- order_items table
ALTER TABLE order_items ALTER COLUMN product_name TYPE VARCHAR(255) USING product_name::text;
ALTER TABLE order_items ALTER COLUMN product_image_url TYPE VARCHAR(500) USING product_image_url::text;

-- upload_sessions table
ALTER TABLE upload_sessions ALTER COLUMN entity_type TYPE VARCHAR(20) USING entity_type::text;
ALTER TABLE upload_sessions ALTER COLUMN file_name TYPE VARCHAR(255) USING file_name::text;
ALTER TABLE upload_sessions ALTER COLUMN content_type TYPE VARCHAR(100) USING content_type::text;
ALTER TABLE upload_sessions ALTER COLUMN storage_path TYPE VARCHAR(1024) USING storage_path::text;
ALTER TABLE upload_sessions ALTER COLUMN supabase_token TYPE VARCHAR(512) USING supabase_token::text;
ALTER TABLE upload_sessions ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

-- payments table
ALTER TABLE payments ALTER COLUMN currency TYPE VARCHAR(3) USING currency::text;
ALTER TABLE payments ALTER COLUMN method TYPE VARCHAR(20) USING method::text;
ALTER TABLE payments ALTER COLUMN status TYPE VARCHAR(20) USING status::text;
ALTER TABLE payments ALTER COLUMN card_last_four TYPE VARCHAR(4) USING card_last_four::text;
ALTER TABLE payments ALTER COLUMN card_brand TYPE VARCHAR(20) USING card_brand::text;
ALTER TABLE payments ALTER COLUMN provider_ref TYPE VARCHAR(255) USING provider_ref::text;
ALTER TABLE payments ALTER COLUMN failure_reason TYPE TEXT USING failure_reason::text;
