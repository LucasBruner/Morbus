ALTER TABLE queue_entries
    ADD COLUMN solicitacao_id UUID,
    ADD COLUMN preferred_unit_id UUID,
    ADD COLUMN priority_group SMALLINT;
