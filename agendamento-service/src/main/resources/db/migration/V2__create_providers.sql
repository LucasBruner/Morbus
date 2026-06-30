CREATE TABLE IF NOT EXISTS agendamento.providers (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    nome            VARCHAR(255)    NOT NULL,
    crm             VARCHAR(30)     NOT NULL,
    especialidade   VARCHAR(120)    NOT NULL,
    unit_id         UUID            NOT NULL,
    ativo           BOOLEAN         NOT NULL DEFAULT true,

    CONSTRAINT pk_providers PRIMARY KEY (id),
    CONSTRAINT fk_providers_health_unit
        FOREIGN KEY (unit_id)
        REFERENCES agendamento.health_units (id)
        ON DELETE RESTRICT
);
