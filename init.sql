-- Executado automaticamente como superuser (postgres) na criação do container.
-- Cria o usuário da aplicação, os schemas isolados por serviço,
-- e concede todas as permissões necessárias para Flyway e Hibernate.

CREATE USER sus_user WITH PASSWORD 'sus_pass';

GRANT ALL PRIVILEGES ON DATABASE sus_queue_db TO sus_user;

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
