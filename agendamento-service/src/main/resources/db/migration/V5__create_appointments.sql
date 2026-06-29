CREATE TABLE IF NOT EXISTS agendamento.appointments (
    id               UUID        NOT NULL DEFAULT gen_random_uuid(),
    slot_id          UUID        NOT NULL,
    paciente_id      UUID        NOT NULL,
    solicitacao_id   UUID,
    status           VARCHAR(30) NOT NULL DEFAULT 'AGUARDANDO_CONFIRMACAO',
    expires_at       TIMESTAMP,
    confirmed_at     TIMESTAMP,
    attended_at      TIMESTAMP,
    created_at       TIMESTAMP   NOT NULL DEFAULT now(),

    CONSTRAINT pk_appointments PRIMARY KEY (id),
    CONSTRAINT fk_appointments_slot
        FOREIGN KEY (slot_id)
        REFERENCES agendamento.slots (id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_appointments_expiration
    ON agendamento.appointments (status, expires_at);

CREATE INDEX idx_appointments_paciente
    ON agendamento.appointments (paciente_id);
