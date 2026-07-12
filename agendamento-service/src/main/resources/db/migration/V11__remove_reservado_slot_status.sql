-- RESERVADO nunca foi produzido por nenhum fluxo do dominio (EStatusSlots so tem
-- DISPONIVEL/OCUPADO/INDISPONIVEL) — removido do CHECK para refletir os status reais.
ALTER TABLE agendamento.slots DROP CONSTRAINT chk_slots_status;
ALTER TABLE agendamento.slots ADD CONSTRAINT chk_slots_status CHECK (
    status IN ('DISPONIVEL', 'OCUPADO', 'INDISPONIVEL')
);
