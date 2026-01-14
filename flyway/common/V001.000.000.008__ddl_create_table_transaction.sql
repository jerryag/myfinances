CREATE TABLE transaction (
    id SERIAL NOT NULL,
    user_id INTEGER NOT NULL,
    transaction_type_id INTEGER NOT NULL,
    transaction_date DATE NOT NULL,
    description VARCHAR(50) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    remark VARCHAR(100),
    created_at TIMESTAMP NOT NULL
);

ALTER TABLE transaction ADD CONSTRAINT pk_transaction PRIMARY KEY (id);

ALTER TABLE transaction ADD CONSTRAINT fk_transaction_user FOREIGN KEY (user_id) REFERENCES "user" (id);

ALTER TABLE transaction ADD CONSTRAINT fk_transaction_type FOREIGN KEY (transaction_type_id) REFERENCES transaction_type (id);

CREATE INDEX idx_transaction_user_date ON transaction (user_id, transaction_date);
