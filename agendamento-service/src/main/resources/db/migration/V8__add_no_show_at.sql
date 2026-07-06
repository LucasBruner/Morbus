ALTER TABLE agendamento.appointments
    ADD COLUMN IF NOT EXISTS no_show_at TIMESTAMP;
