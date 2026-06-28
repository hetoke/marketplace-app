INSERT INTO categories (name, slug, description, is_active, created_at, updated_at)
SELECT 'Electronics', 'electronics', 'Smartphones, laptops, gadgets, and accessories', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'electronics');

INSERT INTO categories (name, slug, description, is_active, created_at, updated_at)
SELECT 'Clothing', 'clothing', 'Men''s, women''s, and children''s apparel', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'clothing');

INSERT INTO categories (name, slug, description, is_active, created_at, updated_at)
SELECT 'Home & Garden', 'home-garden', 'Furniture, decor, kitchenware, and garden tools', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'home-garden');

INSERT INTO categories (name, slug, description, is_active, created_at, updated_at)
SELECT 'Books', 'books', 'Fiction, non-fiction, textbooks, and e-books', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'books');

INSERT INTO categories (name, slug, description, is_active, created_at, updated_at)
SELECT 'Sports', 'sports', 'Fitness equipment, sportswear, and outdoor gear', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'sports');

INSERT INTO categories (name, slug, description, is_active, created_at, updated_at)
SELECT 'Beauty', 'beauty', 'Skincare, makeup, haircare, and personal care', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'beauty');

INSERT INTO categories (name, slug, description, is_active, created_at, updated_at)
SELECT 'Toys', 'toys', 'Action figures, board games, puzzles, and educational toys', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'toys');

INSERT INTO categories (name, slug, description, is_active, created_at, updated_at)
SELECT 'Automotive', 'automotive', 'Car parts, accessories, and maintenance supplies', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'automotive');
