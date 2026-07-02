CREATE TABLE IF NOT EXISTS regulacao.unidades_solicitantes (
    id        UUID         NOT NULL DEFAULT gen_random_uuid(),
    cnes      VARCHAR(7)   NOT NULL,
    nome      VARCHAR(255) NOT NULL,
    endereco  VARCHAR(255),
    telefone  VARCHAR(20),

    CONSTRAINT pk_unidades_solicitantes PRIMARY KEY (id),
    CONSTRAINT uq_unidades_solicitantes_cnes UNIQUE (cnes)
);

ALTER TABLE regulacao.solicitacoes
    ADD CONSTRAINT fk_solicitacoes_unidade_solicitante
        FOREIGN KEY (unidade_solicitante_id)
        REFERENCES regulacao.unidades_solicitantes (id);