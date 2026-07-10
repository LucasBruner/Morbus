-- V3: Expande o CHECK de role para incluir os perfis dos novos serviços
-- (regulacao-service e agendamento-service), que já exigem essas roles
-- em seus endpoints mas não podiam ser emitidas pelo auth-service.
ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users
    ADD CONSTRAINT users_role_check
    CHECK (role IN ('MEDICO', 'PACIENTE', 'SOLICITANTE', 'REGULADOR', 'EXECUTANTE'));
