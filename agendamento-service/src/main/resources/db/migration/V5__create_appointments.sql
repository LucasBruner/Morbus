CREATE TABLE IF NOT EXISTS agendamento.appointments (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    queue_entry_id      UUID         NOT NULL,
    slot_id             UUID         NOT NULL REFERENCES agendamento.slots(id),
    patient_id          UUID         NOT NULL,
    status              VARCHAR(30)  NOT NULL DEFAULT 'AGUARDANDO_CONFIRMACAO'
                            CHECK (status IN ('AGUARDANDO_CONFIRMACAO', 'CONFIRMADO', 'CANCELADO', 'ATENDIDO', 'FALTOU')),
    expires_at          TIMESTAMP    NOT NULL,
    cancellation_reason TEXT,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP
);

CREATE INDEX idx_appointments_expiration
    ON agendamento.appointments (expires_at, status)
    WHERE status = 'AGUARDANDO_CONFIRMACAO';
