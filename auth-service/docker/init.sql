-- Executado automaticamente como superuser (postgres) na criação do container.
-- Cria o usuário da aplicação e concede todos os privilégios necessários
-- para que o Flyway consiga criar e gerenciar as tabelas.

CREATE USER sus_user WITH PASSWORD 'sus_pass';

GRANT ALL PRIVILEGES ON DATABASE sus_queue_db TO sus_user;
GRANT ALL ON SCHEMA public TO sus_user;
ALTER SCHEMA public OWNER TO sus_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO sus_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO sus_user;
