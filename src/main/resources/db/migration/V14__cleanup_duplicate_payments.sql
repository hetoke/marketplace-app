-- Remove duplicate PENDING payments, keep only the most recent per order_id
DELETE FROM payments
WHERE id NOT IN (
    SELECT DISTINCT ON (order_id) id
    FROM payments
    WHERE status = 'PENDING'
    ORDER BY order_id, created_at DESC
)
AND status = 'PENDING';
