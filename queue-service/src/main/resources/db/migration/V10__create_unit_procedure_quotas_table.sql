-- V10: Cota diária de FILA_ESPERA por unidade + procedimento.
-- FILA_REGULADA nunca é limitada por cota; a ausência de linha aqui
-- para uma combinação unit_id+procedure_id significa "sem cota" (livre).
CREATE TABLE unit_procedure_quotas (
    id            UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id       UUID      NOT NULL,
    procedure_id  UUID      NOT NULL REFERENCES procedures(id),
    max_per_day   INT       NOT NULL CHECK (max_per_day > 0),
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_unit_procedure_quotas_unit_procedure UNIQUE (unit_id, procedure_id)
);
