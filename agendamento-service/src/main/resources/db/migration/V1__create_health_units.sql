CREATE TABLE IF NOT EXISTS agendamento.health_units (
    id      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    cnes    VARCHAR(7)   NOT NULL UNIQUE,
    name    VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    phone   VARCHAR(20)
);
