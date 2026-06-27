CREATE TABLE IF NOT EXISTS agendamento.schedules (
    id            UUID        NOT NULL DEFAULT gen_random_uuid(),
    provider_id   UUID        NOT NULL,
    unit_id       UUID        NOT NULL,
    data_inicio   TIMESTAMP   NOT NULL,
    data_fim      TIMESTAMP   NOT NULL,
    turno         VARCHAR(20) NOT NULL,

    CONSTRAINT pk_schedules PRIMARY KEY (id),
    CONSTRAINT fk_schedules_provider
        FOREIGN KEY (provider_id)
        REFERENCES agendamento.providers (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_schedules_health_unit
        FOREIGN KEY (unit_id)
        REFERENCES agendamento.health_units (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_schedules_turno CHECK (
        turno IN ('MANHA', 'TARDE', 'NOITE')
    ),
    CONSTRAINT chk_schedules_periodo CHECK (data_fim > data_inicio)
);
