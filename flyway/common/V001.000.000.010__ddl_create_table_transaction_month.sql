CREATE TABLE transaction_month (
    id SERIAL NOT NULL,
    user_id INTEGER NOT NULL,
    month INTEGER NOT NULL,
    year INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    initial_balance DECIMAL(10,2) NOT NULL DEFAULT 0.00
);

ALTER TABLE transaction_month ADD CONSTRAINT pk_transaction_month PRIMARY KEY (id);

ALTER TABLE transaction_month ADD CONSTRAINT fk_transaction_month_user FOREIGN KEY (user_id) REFERENCES "user" (id);

ALTER TABLE transaction_month ADD CONSTRAINT uk_transaction_month_user_month_year UNIQUE (user_id, month, year);
