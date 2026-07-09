-- V7: Adiciona tipo_fila à tabela de entradas da fila
-- 0 = FILA_ESPERA, 1 = FILA_REGULADA (ordinal do enum EDestino)
-- Entradas existentes são migradas para FILA_REGULADA (1) como padrão
ALTER TABLE queue_entries
    ADD COLUMN tipo_fila SMALLINT NOT NULL DEFAULT 1;
