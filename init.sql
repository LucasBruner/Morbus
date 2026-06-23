-- Executado automaticamente como superuser (postgres) na criação do container.
-- Cria o usuário da aplicação, os bancos isolados por serviço,
-- e concede todas as permissões necessárias para Flyway e Hibernate.

-- ─────────────────────────────────────────────
-- Usuário da aplicação
-- ─────────────────────────────────────────────
CREATE USER sus_user WITH PASSWORD 'sus_pass';

-- ─────────────────────────────────────────────
-- sus_queue_db  →  queue-service + auth-service
-- (banco padrão criado pelo POSTGRES_DB no compose)
-- ─────────────────────────────────────────────
GRANT ALL PRIVILEGES ON DATABASE sus_queue_db TO sus_user;

\connect sus_queue_db

-- Schema do queue-service
CREATE SCHEMA IF NOT EXISTS queue AUTHORIZATION sus_user;
GRANT ALL ON SCHEMA queue TO sus_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA queue GRANT ALL ON TABLES TO sus_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA queue GRANT ALL ON SEQUENCES TO sus_user;

-- Schema do auth-service
CREATE SCHEMA IF NOT EXISTS auth AUTHORIZATION sus_user;
GRANT ALL ON SCHEMA auth TO sus_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA auth GRANT ALL ON TABLES TO sus_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA auth GRANT ALL ON SEQUENCES TO sus_user;

-- ─────────────────────────────────────────────
-- regulacao_db  →  regulacao-service
-- ─────────────────────────────────────────────
\connect postgres

CREATE DATABASE regulacao_db OWNER sus_user;

\connect regulacao_db

CREATE SCHEMA IF NOT EXISTS regulacao AUTHORIZATION sus_user;
GRANT ALL ON SCHEMA regulacao TO sus_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA regulacao GRANT ALL ON TABLES TO sus_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA regulacao GRANT ALL ON SEQUENCES TO sus_user;

-- ─────────────────────────────────────────────
-- agendamento_db  →  agendamento-service
-- ─────────────────────────────────────────────
\connect postgres

CREATE DATABASE agendamento_db OWNER sus_user;

\connect agendamento_db

CREATE SCHEMA IF NOT EXISTS agendamento AUTHORIZATION sus_user;
GRANT ALL ON SCHEMA agendamento TO sus_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA agendamento GRANT ALL ON TABLES TO sus_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA agendamento GRANT ALL ON SEQUENCES TO sus_user;
