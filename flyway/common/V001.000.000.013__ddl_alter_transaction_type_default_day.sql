ALTER TABLE transaction_type ADD COLUMN IF NOT EXISTS default_day INTEGER CHECK (default_day >= 1 AND default_day <= 31);
