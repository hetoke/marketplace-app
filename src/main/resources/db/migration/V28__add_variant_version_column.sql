-- Fix: V26 was applied without the version column needed by ProductVariant @Version
ALTER TABLE product_variants ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
