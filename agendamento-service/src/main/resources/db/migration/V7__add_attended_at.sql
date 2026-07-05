ALTER TABLE agendamento.appointments
ADD COLUMN IF NOT EXISTS attended_at TIMESTAMP;
