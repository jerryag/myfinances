CREATE TABLE "user" (
    id SERIAL NOT NULL,
    login VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    type VARCHAR(20) NOT NULL
);

ALTER TABLE "user" ADD CONSTRAINT pk_user PRIMARY KEY (id);
ALTER TABLE "user" ADD CONSTRAINT uk_user_login UNIQUE (login);

CREATE UNIQUE INDEX idx_user_01 ON "user" (id);
