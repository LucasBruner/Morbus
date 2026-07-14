-- A entidade JPA mapeia "uf" como String simples (VARCHAR), mas a V1 criou a
-- coluna como CHAR(2) (bpchar) — Hibernate schema-validation falha nesse
-- descompasso de tipo. Convertido para VARCHAR(2) para bater com o mapeamento.
ALTER TABLE agendamento.health_units
    ALTER COLUMN uf TYPE VARCHAR(2);
