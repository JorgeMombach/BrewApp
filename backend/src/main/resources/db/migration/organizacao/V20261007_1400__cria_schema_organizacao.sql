-- Schema do módulo organizacao, dono: role de migração (spec 5.1).
-- Role da aplicação: usa o schema e manipula dados, sem DDL (spec 9.3).
-- DELETE não entra no padrão: exclusão física é exceção (spec 5.5) e é concedida por tabela.
CREATE SCHEMA organizacao;

GRANT USAGE ON SCHEMA organizacao TO ${appRole};

ALTER DEFAULT PRIVILEGES IN SCHEMA organizacao
    GRANT SELECT, INSERT, UPDATE ON TABLES TO ${appRole};

ALTER DEFAULT PRIVILEGES IN SCHEMA organizacao
    GRANT USAGE, SELECT ON SEQUENCES TO ${appRole};
