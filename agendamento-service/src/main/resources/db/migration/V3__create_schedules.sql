CREATE TABLE IF NOT EXISTS agendamento.schedules (
    id                   UUID        NOT NULL DEFAULT gen_random_uuid(),
    unit_id              UUID        NOT NULL,
    provider_id          UUID,
    procedure_id         UUID        NOT NULL,
    dia_da_semana        VARCHAR(15) NOT NULL,
    horario_inicio       TIME        NOT NULL,
    horario_fim          TIME        NOT NULL,
    slot_duracao_minutos INTEGER     NOT NULL,
    capacidade           INTEGER     NOT NULL,
    ativo                BOOLEAN     NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_schedules PRIMARY KEY (id),
    CONSTRAINT fk_schedules_health_unit
        FOREIGN KEY (unit_id)
        REFERENCES agendamento.health_units (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_schedules_provider
        FOREIGN KEY (provider_id)
        REFERENCES agendamento.providers (id)
        ON DELETE SET NULL,
    CONSTRAINT chk_schedules_dia_semana CHECK (
        dia_da_semana IN ('SEGUNDA','TERCA','QUARTA','QUINTA','SEXTA','SABADO','DOMINGO')
    ),
    CONSTRAINT chk_schedules_horario CHECK (horario_fim > horario_inicio)
);
