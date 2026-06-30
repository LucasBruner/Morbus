CREATE TABLE IF NOT EXISTS agendamento.appointments (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    queue_entry_id      UUID        NOT NULL,
    slot_id             UUID        NOT NULL,
    paciente_id         UUID        NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'AGUARDANDO_CONFIRMACAO',
    expires_at          TIMESTAMP   NOT NULL,
    cancellation_reason TEXT,
    created_at          TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP,

    CONSTRAINT pk_appointments PRIMARY KEY (id),
    CONSTRAINT fk_appointments_slot
        FOREIGN KEY (slot_id)
        REFERENCES agendamento.slots (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_appointments_status CHECK (
        status IN ('AGUARDANDO_CONFIRMACAO','CONFIRMADO','CANCELADO','ATENDIDO','FALTOU')
    )
);

CREATE INDEX idx_appointments_expiration
    ON agendamento.appointments (status, expires_at)
    WHERE status = 'AGUARDANDO_CONFIRMACAO';

CREATE INDEX idx_appointments_paciente
    ON agendamento.appointments (paciente_id);
