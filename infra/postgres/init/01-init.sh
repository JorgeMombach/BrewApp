#!/usr/bin/env bash
# Inicialização do banco local. Roda uma única vez, quando o volume do Postgres está vazio.
# Schemas, grants por schema e RLS ficam com as migrations do Flyway.
set -euo pipefail

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  -v owner_user="$BREWAPP_DB_OWNER_USER" \
  -v owner_password="$BREWAPP_DB_OWNER_PASSWORD" \
  -v app_user="$BREWAPP_DB_APP_USER" \
  -v app_password="$BREWAPP_DB_APP_PASSWORD" \
  -v keycloak_user="$KC_DB_USERNAME" \
  -v keycloak_password="$KC_DB_PASSWORD" \
  -v brewapp_db="$POSTGRES_DB" <<'SQL'
-- Monitoramento de consultas desde o início (spec 5.6)
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Só as roles do BrewApp conectam no banco da aplicação
REVOKE ALL ON DATABASE :"brewapp_db" FROM PUBLIC;

-- Dona das tabelas: usada pelo Flyway, cria os schemas dos módulos
CREATE ROLE :"owner_user" LOGIN PASSWORD :'owner_password';
GRANT CONNECT, CREATE ON DATABASE :"brewapp_db" TO :"owner_user";

-- Role da aplicação: não é dona das tabelas, para que o RLS seja aplicado (spec 9.3)
CREATE ROLE :"app_user" LOGIN PASSWORD :'app_password';
GRANT CONNECT ON DATABASE :"brewapp_db" TO :"app_user";

-- Keycloak em banco próprio, isolado do banco da aplicação
CREATE ROLE :"keycloak_user" LOGIN PASSWORD :'keycloak_password';
CREATE DATABASE keycloak OWNER :"keycloak_user";
REVOKE ALL ON DATABASE keycloak FROM PUBLIC;
SQL
