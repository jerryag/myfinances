CREATE TABLE transaction_detail (
    id SERIAL NOT NULL,
    transaction_id INTEGER NOT NULL,
    detail_date TIMESTAMP NOT NULL,
    description VARCHAR(50) NOT NULL,
    amount DECIMAL(10,2) NOT NULL
);

ALTER TABLE transaction_detail ADD CONSTRAINT pk_transaction_detail PRIMARY KEY (id);

ALTER TABLE transaction_detail ADD CONSTRAINT fk_transaction_detail_transaction FOREIGN KEY (transaction_id) REFERENCES transaction (id);
