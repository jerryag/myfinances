CREATE TABLE transaction_type (
    id SERIAL NOT NULL,
    user_id INTEGER NOT NULL,
    type VARCHAR(10) NOT NULL,
    description VARCHAR(40) NOT NULL,
    recurring BOOLEAN NOT NULL
);

ALTER TABLE transaction_type ADD CONSTRAINT pk_transaction_type PRIMARY KEY (id);

ALTER TABLE transaction_type ADD CONSTRAINT fk_transaction_type_user FOREIGN KEY (user_id) REFERENCES "user" (id);

ALTER TABLE transaction_type ADD CONSTRAINT uk_transaction_type_description_user UNIQUE (description, user_id);
