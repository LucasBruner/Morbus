CREATE TABLE IF NOT EXISTS agendamento.health_units (
    id          UUID            NOT NULL DEFAULT gen_random_uuid(),
    nome        VARCHAR(255)    NOT NULL,
    cnes        VARCHAR(20)     NOT NULL,
    municipio   VARCHAR(150)    NOT NULL,
    uf          CHAR(2)         NOT NULL,
    ativo       BOOLEAN         NOT NULL DEFAULT true,

    CONSTRAINT pk_health_units PRIMARY KEY (id),
    CONSTRAINT uq_health_units_cnes UNIQUE (cnes),
    CONSTRAINT chk_health_units_uf CHECK (char_length(trim(uf)) = 2)
);
