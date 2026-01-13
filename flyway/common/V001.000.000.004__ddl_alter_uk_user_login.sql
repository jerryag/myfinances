ALTER TABLE "user" DROP CONSTRAINT uk_user_login;
ALTER TABLE "user" ADD CONSTRAINT uk_user_login UNIQUE (login, status);
