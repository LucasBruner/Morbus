CREATE TABLE IF NOT EXISTS agendamento.schedules (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id           UUID         NOT NULL REFERENCES agendamento.health_units(id),
    provider_id       UUID         REFERENCES agendamento.providers(id),
    procedure_id      UUID         NOT NULL,
    day_of_week       VARCHAR(15)  NOT NULL,
    start_time        TIME         NOT NULL,
    end_time          TIME         NOT NULL,
    slot_duration_min INTEGER      NOT NULL,
    capacity          INTEGER      NOT NULL,
    active            BOOLEAN      NOT NULL DEFAULT TRUE
);
