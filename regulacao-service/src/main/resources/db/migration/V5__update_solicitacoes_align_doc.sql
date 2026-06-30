UPDATE regulacao.solicitacoes SET status = 'AGUARDANDO' WHERE status = 'PENDENTE';

ALTER TABLE regulacao.solicitacoes DROP CONSTRAINT chk_solicitacoes_status;
ALTER TABLE regulacao.solicitacoes ADD CONSTRAINT chk_solicitacoes_status CHECK (
    status IN ('AGUARDANDO', 'APROVADA', 'NEGADA', 'CANCELADA', 'DEVOLVIDA', 'PENDENTE', 'AGENDADA', 'ATENDIDA', 'FALTOU')
);
ALTER TABLE regulacao.solicitacoes ALTER COLUMN status SET DEFAULT 'AGUARDANDO';

ALTER TABLE regulacao.solicitacoes ALTER COLUMN risco_solicitado SET DEFAULT 'AZUL';

ALTER TABLE regulacao.solicitacoes ADD COLUMN IF NOT EXISTS cid VARCHAR(20);
ALTER TABLE regulacao.solicitacoes ADD COLUMN IF NOT EXISTS justificativa_clinica TEXT;
ALTER TABLE regulacao.solicitacoes ADD COLUMN IF NOT EXISTS profissional_solicitante VARCHAR(200);
ALTER TABLE regulacao.solicitacoes ADD COLUMN IF NOT EXISTS crm_profissional VARCHAR(50);
ALTER TABLE regulacao.solicitacoes ADD COLUMN IF NOT EXISTS destino VARCHAR(20);

ALTER TABLE regulacao.solicitacoes ADD CONSTRAINT chk_solicitacoes_destino CHECK (
    destino IS NULL OR destino IN ('FILA_REGULADA', 'FILA_ESPERA')
);

CREATE INDEX IF NOT EXISTS idx_solicitacoes_destino ON regulacao.solicitacoes (destino);
