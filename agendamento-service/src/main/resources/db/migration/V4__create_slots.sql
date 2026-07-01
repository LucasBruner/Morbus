CREATE TABLE IF NOT EXISTS agendamento.slots (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    schedule_id UUID        NOT NULL,
    data_hora   TIMESTAMP   NOT NULL,
    capacidade  INTEGER     NOT NULL,
    reservados  INTEGER     NOT NULL DEFAULT 0,
    status      VARCHAR(20) NOT NULL DEFAULT 'DISPONIVEL',

    CONSTRAINT pk_slots PRIMARY KEY (id),
    CONSTRAINT fk_slots_schedule
        FOREIGN KEY (schedule_id)
        REFERENCES agendamento.schedules (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_slots_status CHECK (
        status IN ('DISPONIVEL', 'RESERVADO', 'OCUPADO', 'INDISPONIVEL')
    ),
    CONSTRAINT uq_slots_schedule_data_hora UNIQUE (schedule_id, data_hora)
);

CREATE INDEX idx_slots_availability
    ON agendamento.slots (status, data_hora)
    WHERE status = 'DISPONIVEL';
