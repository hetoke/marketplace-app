ALTER TABLE payments ADD COLUMN invoice_number VARCHAR(255);
CREATE INDEX idx_payments_invoice ON payments(invoice_number);
