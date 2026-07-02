CREATE TABLE IF NOT EXISTS regulacao.quotas (
    id              UUID    NOT NULL DEFAULT gen_random_uuid(),
    unit_id         UUID    NOT NULL,
    procedure_id    UUID    NOT NULL,
    max_per_period  INTEGER NOT NULL CHECK (max_per_period > 0),
    current_count   INTEGER NOT NULL DEFAULT 0 CHECK (current_count >= 0),
    period_start    DATE    NOT NULL,

    CONSTRAINT pk_quotas PRIMARY KEY (id),
    CONSTRAINT fk_quotas_unidade_solicitante
        FOREIGN KEY (unit_id)
        REFERENCES regulacao.unidades_solicitantes (id)
        ON DELETE CASCADE,
    CONSTRAINT uq_quotas_unit_procedure_period
        UNIQUE (unit_id, procedure_id, period_start)
);
