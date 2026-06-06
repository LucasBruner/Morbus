-- V3: Criação da tabela de entradas na fila
CREATE TABLE queue_entries (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id    UUID        NOT NULL REFERENCES patients(id),
    procedure_id  UUID        NOT NULL REFERENCES procedures(id),
    risk_color    VARCHAR(10) NOT NULL DEFAULT 'AZUL'
                      CHECK (risk_color IN ('VERMELHO', 'AMARELO', 'VERDE', 'AZUL')),
    status        VARCHAR(20) NOT NULL DEFAULT 'AGUARDANDO'
                      CHECK (status IN ('AGUARDANDO', 'AGENDADO', 'ATENDIDO', 'FALTOU', 'CANCELADO', 'DEVOLVIDO')),
    registered_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP
);

-- Índice otimizado para a query crítica de prioridade (evita full table scan ao chamar próximo ou calcular posição)
CREATE INDEX idx_queue_entries_priority
    ON queue_entries (risk_color, status, registered_at)
    WHERE status = 'AGUARDANDO';
