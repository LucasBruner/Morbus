-- Usuários de teste para desenvolvimento e demonstração
-- Senhas geradas com bcrypt (fator 10) — compatível com Spring Security BCryptPasswordEncoder
--
-- medico_teste  / medico123
-- paciente_teste / paciente123

INSERT INTO users (id, username, email, password_hash, role, created_at)
VALUES
    (gen_random_uuid(),
     'medico_teste',
     'medico@sus.gov.br',
     '$2b$10$2HsykHf7Y4Beuf3DwjKlLuZ4eYHV3jDaYQvaKCGTRz0DDDla9ntuO',
     'MEDICO',
     NOW()),

    (gen_random_uuid(),
     'paciente_teste',
     'paciente@sus.gov.br',
     '$2b$10$n01vKZwKrVhbfpdIB9JiHeFL59.KlmeEc0wnDh7E0IVJJVRuook/H6',
     'PACIENTE',
     NOW());
