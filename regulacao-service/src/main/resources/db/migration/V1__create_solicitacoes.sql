CREATE TABLE IF NOT EXISTS regulacao.solicitacoes (
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    paciente_id             UUID            NOT NULL,
    procedure_id            UUID            NOT NULL,
    unidade_solicitante_id  UUID            NOT NULL,
    unidade_executante_id   UUID,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'PENDENTE',
    risco_solicitado        VARCHAR(20)     NOT NULL,
    justificativa_negacao   TEXT,
    solicitado_por          UUID            NOT NULL,
    created_at              TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT now(),

    CONSTRAINT pk_solicitacoes PRIMARY KEY (id),
    CONSTRAINT chk_solicitacoes_status CHECK (
        status IN ('PENDENTE', 'EM_ANALISE', 'APROVADA', 'NEGADA', 'CANCELADA')
    ),
    CONSTRAINT chk_solicitacoes_risco CHECK (
        risco_solicitado IN ('VERMELHO', 'AMARELO', 'VERDE', 'AZUL')
    )
);

CREATE INDEX idx_solicitacoes_status
    ON regulacao.solicitacoes (status);

CREATE INDEX idx_solicitacoes_paciente
    ON regulacao.solicitacoes (paciente_id);

CREATE INDEX idx_solicitacoes_unidade
    ON regulacao.solicitacoes (unidade_solicitante_id);
