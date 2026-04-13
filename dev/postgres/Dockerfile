FROM postgres:16-alpine

ENV POSTGRES_DB=myfinance_db
ENV POSTGRES_USER=admin
ENV POSTGRES_PASSWORD=admin

COPY init.sql /docker-entrypoint-initdb.d/
EXPOSE 5432
