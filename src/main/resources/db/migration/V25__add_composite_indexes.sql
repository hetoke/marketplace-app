-- Composite indexes for high-traffic query patterns

-- Products: category browsing filtered by active status
CREATE INDEX idx_products_category_active ON products(category_id, is_active);

-- Products: seller dashboard listing active products
CREATE INDEX idx_products_seller_active ON products(seller_id, is_active);

-- Orders: buyer order history sorted by date (covers findByBuyerIdOrderByCreatedAtDesc)
CREATE INDEX idx_orders_buyer_created ON orders(buyer_id, created_at DESC);

-- Orders: buyer orders filtered by status
CREATE INDEX idx_orders_buyer_status ON orders(buyer_id, status);

-- Reviews: product reviews sorted by date
CREATE INDEX idx_reviews_product_created ON reviews(product_id, created_at DESC);

-- Reviews: buyer review history sorted by date
CREATE INDEX idx_reviews_buyer_created ON reviews(buyer_id, created_at DESC);

-- Notifications: user notifications with unread filter and date sort
CREATE INDEX idx_notifications_user_read_created ON notifications(user_id, is_read, created_at DESC);

-- Payments: lookup payment by order and status
CREATE INDEX idx_payments_order_status ON payments(order_id, status);
