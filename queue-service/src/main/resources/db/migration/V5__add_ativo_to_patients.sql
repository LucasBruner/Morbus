-- V5: Adiciona campo ativo a tabela de pacientes
ALTER TABLE patients ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
