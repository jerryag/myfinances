ALTER TABLE transaction ADD COLUMN transaction_month_id INTEGER;

ALTER TABLE transaction ADD CONSTRAINT fk_transaction_month_id FOREIGN KEY (transaction_month_id) REFERENCES transaction_month (id);
