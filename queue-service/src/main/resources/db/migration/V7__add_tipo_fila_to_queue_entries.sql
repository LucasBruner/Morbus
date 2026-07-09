-- V7: Adiciona tipo_fila à tabela de entradas da fila
-- Persistido como texto (nome do enum EDestino) para não acoplar a coluna à ordem
-- de declaração do enum, ao contrário de um mapeamento por ordinal.
-- Entradas existentes são migradas para FILA_REGULADA como padrão
ALTER TABLE queue_entries
    ADD COLUMN tipo_fila VARCHAR(20) NOT NULL DEFAULT 'FILA_REGULADA';
