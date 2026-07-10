-- V9: Inclui o status CHAMADO no CHECK de queue_entries.status
-- CallNextPatient passou a setar CHAMADO (aguardando appointment.confirmed
-- do agendamento-service) em vez de pular direto para AGENDADO. Sem esta
-- migration, o INSERT/UPDATE falha por violação do CHECK original (V3).
ALTER TABLE queue_entries
    DROP CONSTRAINT IF EXISTS queue_entries_status_check;
ALTER TABLE queue_entries
    ADD CONSTRAINT queue_entries_status_check
    CHECK (status IN ('AGUARDANDO', 'CHAMADO', 'AGENDADO', 'ATENDIDO', 'FALTOU', 'CANCELADO', 'DEVOLVIDO'));
