ALTER TABLE regulacao.pareceres DROP CONSTRAINT chk_pareceres_decisao;
ALTER TABLE regulacao.pareceres ADD CONSTRAINT chk_pareceres_decisao CHECK (
    decisao IN ('AUTORIZAR', 'FILA_ESPERA', 'NEGAR', 'DEVOLVER', 'PENDENTE')
);
