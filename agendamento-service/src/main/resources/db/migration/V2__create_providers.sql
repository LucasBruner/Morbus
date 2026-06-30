CREATE TABLE IF NOT EXISTS agendamento.providers (
    id        UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id   UUID         NOT NULL REFERENCES agendamento.health_units(id),
    name      VARCHAR(255) NOT NULL,
    crm       VARCHAR(20),
    specialty VARCHAR(100)
);
