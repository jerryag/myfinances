ALTER TABLE transaction_type ADD COLUMN status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE transaction_type DROP CONSTRAINT uk_transaction_type_description_user;

ALTER TABLE transaction_type ADD CONSTRAINT uk_transaction_type_description_user UNIQUE (description, user_id, status);
