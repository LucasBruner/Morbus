CREATE TABLE IF NOT EXISTS regulacao.pareceres (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    solicitacao_id  UUID        NOT NULL,
    regulador_id    UUID        NOT NULL,
    decisao         VARCHAR(20) NOT NULL,
    justificativa   TEXT,
    emitido_em      TIMESTAMP   NOT NULL DEFAULT now(),

    CONSTRAINT pk_pareceres PRIMARY KEY (id),
    CONSTRAINT fk_pareceres_solicitacao
        FOREIGN KEY (solicitacao_id)
        REFERENCES regulacao.solicitacoes (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_pareceres_decisao CHECK (
        decisao IN ('APROVADO', 'NEGADO')
    )
);
