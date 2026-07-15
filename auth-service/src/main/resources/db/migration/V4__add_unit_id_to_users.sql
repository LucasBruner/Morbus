-- unit_id nao tem FK porque health_units pertence ao schema do agendamento-service
-- (bancos separados por servico). Preenchido apenas para EXECUTANTE/SOLICITANTE;
-- demais roles ficam com unit_id nulo e simplesmente nao recebem o claim no JWT.
ALTER TABLE users ADD COLUMN unit_id UUID;
