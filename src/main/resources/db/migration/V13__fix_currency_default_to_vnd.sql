ALTER TABLE orders ALTER COLUMN currency SET DEFAULT 'VND';
UPDATE orders SET currency = 'VND' WHERE currency = 'USD';

ALTER TABLE payments ALTER COLUMN currency SET DEFAULT 'VND';
UPDATE payments SET currency = 'VND' WHERE currency = 'USD';
