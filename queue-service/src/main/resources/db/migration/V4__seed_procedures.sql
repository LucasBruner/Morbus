-- V4: Seed de procedimentos representativos do SUS (tabela SIGTAP)
-- Fonte: Tabela SIGTAP do Ministério da Saúde
INSERT INTO procedures (id, co_procedimento, no_procedimento, idade_minima, idade_maxima, grupo) VALUES

-- Consultas e atendimentos
(gen_random_uuid(), '0301010064', 'CONSULTA MEDICA EM ATENCAO BASICA',                   0, 120, 'CONSULTAS / ATENDIMENTOS / ACOMPANHAMENTOS'),
(gen_random_uuid(), '0301010072', 'CONSULTA MEDICA EM ATENCAO ESPECIALIZADA',             0, 120, 'CONSULTAS / ATENDIMENTOS / ACOMPANHAMENTOS'),
(gen_random_uuid(), '0301020010', 'CONSULTA PRE-NATAL',                                   12, 55,  'CONSULTAS / ATENDIMENTOS / ACOMPANHAMENTOS'),
(gen_random_uuid(), '0301040010', 'CONSULTA DE PROFISSIONAL DE NIVEL SUPERIOR NA APS',    0, 120, 'CONSULTAS / ATENDIMENTOS / ACOMPANHAMENTOS'),

-- Exames laboratoriais
(gen_random_uuid(), '0202010011', 'COLETA DE SANGUE A VACUO / SERINGA',                   0, 120, 'COLETA / EXAMES LABORATORIAIS'),
(gen_random_uuid(), '0202010163', 'DOSAGEM DE GLICOSE',                                   0, 120, 'COLETA / EXAMES LABORATORIAIS'),
(gen_random_uuid(), '0202010481', 'HEMOGRAMA COMPLETO',                                   0, 120, 'COLETA / EXAMES LABORATORIAIS'),
(gen_random_uuid(), '0202040035', 'UROCULTURA COM ANTIBIOGRAMA',                          0, 120, 'COLETA / EXAMES LABORATORIAIS'),

-- Diagnóstico por imagem
(gen_random_uuid(), '0205010059', 'RADIOGRAFIA DE TORAX (PA)',                            0, 120, 'DIAGNOSTICO POR IMAGEM'),
(gen_random_uuid(), '0206010024', 'ELETROCARDIOGRAMA',                                    0, 120, 'DIAGNOSTICO POR IMAGEM'),
(gen_random_uuid(), '0211020012', 'ULTRASSONOGRAFIA OBSTETRICA',                          12, 55,  'DIAGNOSTICO POR IMAGEM'),
(gen_random_uuid(), '0211060100', 'ULTRASSONOGRAFIA DE ABDOME TOTAL',                     0, 120, 'DIAGNOSTICO POR IMAGEM'),
(gen_random_uuid(), '0204030030', 'TOMOGRAFIA COMPUTADORIZADA DE CRANIO',                 0, 120, 'DIAGNOSTICO POR IMAGEM'),
(gen_random_uuid(), '0204030080', 'TOMOGRAFIA COMPUTADORIZADA DE TORAX',                  0, 120, 'DIAGNOSTICO POR IMAGEM'),

-- Procedimentos clínicos
(gen_random_uuid(), '0301060134', 'ATENDIMENTO DE URGENCIA COM OBSERVACAO',               0, 120, 'PROCEDIMENTOS CLINICOS'),
(gen_random_uuid(), '0303010010', 'APENDICECTOMIA',                                       5, 120, 'PROCEDIMENTOS CIRURGICOS'),
(gen_random_uuid(), '0303050031', 'CATETERISMO CARDIACO',                                 0, 120, 'PROCEDIMENTOS CIRURGICOS'),
(gen_random_uuid(), '0302060032', 'COLONOSCOPIA',                                         18, 120, 'PROCEDIMENTOS COM FINALIDADE DIAGNOSTICA'),

-- Terapias especializadas
(gen_random_uuid(), '0407010086', 'HEMODIALISE',                                          0, 120, 'TERAPIAS ESPECIALIZADAS'),
(gen_random_uuid(), '0501050044', 'QUIMIOTERAPIA ANTINEOPLASICA SISTEMICA',               0, 120, 'TERAPIAS ESPECIALIZADAS');
