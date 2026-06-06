-- V2: Criação da tabela de pacientes
-- Nota: nome e sobrenome são armazenados em colunas separadas para corresponder ao domínio (Patient.nome + Patient.sobrenome)
CREATE TABLE patients (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    cpf              VARCHAR(14)  NOT NULL UNIQUE,
    cns              VARCHAR(15)  UNIQUE,
    nome             VARCHAR(100) NOT NULL,
    sobrenome        VARCHAR(100) NOT NULL,
    data_nascimento  DATE         NOT NULL,
    sexo             VARCHAR(10)  CHECK (sexo IN ('MASCULINO', 'FEMININO', 'OUTROS')),
    contato          VARCHAR(255),
    grupo_legal      VARCHAR(20)  NOT NULL CHECK (grupo_legal IN ('IDOSO', 'GESTANTE', 'DEFICIENTE', 'LACTANTE', 'OBESO', 'GERAL'))
);
