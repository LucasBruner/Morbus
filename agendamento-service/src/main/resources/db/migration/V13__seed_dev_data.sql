-- Seed fixo de health_unit + provider usado pelo fluxo de testes manuais/Postman
-- (postman/e2e-flows.postman_collection.json, variaveis {{agendamentoUnitId}} e
-- {{agendamentoProviderId}}). Substitui a necessidade de rodar
-- postman/seed-agendamento.sh manualmente: agora os dados ja existem assim que o
-- agendamento-service sobe. Schedule/slots continuam sendo criados via API
-- (request "Create Schedule" da collection), de proposito, para exercitar aquele
-- endpoint no fluxo de teste.
INSERT INTO agendamento.health_units (id, nome, cnes, municipio, uf, ativo, endereco, telefone)
VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'UPA Norte', '2077485', 'Sao Paulo', 'SP', true, 'Rua das Flores, 100', '(11) 4002-8922')
ON CONFLICT (id) DO NOTHING;

INSERT INTO agendamento.providers (id, nome, crm, especialidade, unit_id, ativo)
VALUES ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Dr. Carlos Melo', 'CRM/SP 98765', 'Clinica Geral', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', true)
ON CONFLICT (id) DO NOTHING;
