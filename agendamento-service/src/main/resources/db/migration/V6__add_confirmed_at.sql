ALTER TABLE agendamento.appointments
ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMP;
