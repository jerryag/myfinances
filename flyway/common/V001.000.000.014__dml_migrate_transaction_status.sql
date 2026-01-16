UPDATE transaction SET status = 'COMPLETED' WHERE status IN ('PAID', 'RECEIVED');
UPDATE transaction SET status = 'PENDING' WHERE status = 'CANCELED';
