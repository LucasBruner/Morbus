#!/usr/bin/env bash
set -euo pipefail

# Seed minimo (health_unit + provider + schedule + slots) para permitir que o
# agendamento-service aloque um slot durante a "Flow 10 - Solicitacao Aprovada ->
# Fila -> Agendamento" da colecao postman/e2e-flows.postman_collection.json
# (ver docs/sequencias-diagrama.md, item 10).
#
# Por que isso e necessario:
# POST /api/v1/schedules exige ROLE_EXECUTANTE e compara principal.unitId() (extraido
# do JWT) com o unitId do corpo da requisicao. O auth-service, porem, nunca inclui um
# claim "unitId" no JWT (JwtService.generateToken so adiciona "sub" e "role") — logo
# principal.unitId() e sempre null e ScheduleController rejeita a chamada com 403 para
# qualquer usuario EXECUTANTE. Não existe hoje um caminho publico para popular
# schedules/slots no agendamento-service. Ver postman/README.md > "Limitacoes
# Conhecidas" para mais detalhes (o mesmo gap bloqueia block/unblock, attend e falta).
#
# Uso:
#   ./postman/seed-agendamento.sh [codigo_sigtap]
#
# Requisitos: psql no PATH, docker-compose up (postgres publicado em localhost:5432).

CODIGO="${1:-0301010072}"
UNIT_ID="aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
PROVIDER_ID="bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
SCHEDULE_ID="cccccccc-cccc-cccc-cccc-cccccccccccc"

export PGPASSWORD=sus_pass

PROCEDURE_ID=$(psql -h localhost -U sus_user -d sus_queue_db -t -A \
  -c "SELECT id FROM queue.procedures WHERE co_procedimento = '${CODIGO}' LIMIT 1;")

if [ -z "${PROCEDURE_ID}" ]; then
  echo "Procedimento com codigo ${CODIGO} nao encontrado em queue.procedures. O banco subiu corretamente?" >&2
  exit 1
fi

echo "Procedimento ${CODIGO} -> ${PROCEDURE_ID}"
echo "Unidade executante fixa (deve bater com {{agendamentoUnitId}} na colecao Postman): ${UNIT_ID}"

psql -h localhost -U sus_user -d agendamento_db -v ON_ERROR_STOP=1 <<SQL
INSERT INTO agendamento.health_units (id, nome, cnes, municipio, uf, ativo, endereco, telefone)
VALUES ('${UNIT_ID}', 'UPA Norte', '2077485', 'Sao Paulo', 'SP', true, 'Rua das Flores, 100', '(11) 4002-8922')
ON CONFLICT (id) DO NOTHING;

INSERT INTO agendamento.providers (id, nome, crm, especialidade, unit_id, ativo)
VALUES ('${PROVIDER_ID}', 'Dr. Carlos Melo', 'CRM/SP 98765', 'Clinica Geral', '${UNIT_ID}', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO agendamento.schedules (id, unit_id, provider_id, procedure_id, dia_da_semana, horario_inicio, horario_fim, slot_duracao_minutos, capacidade, ativo)
VALUES ('${SCHEDULE_ID}', '${UNIT_ID}', '${PROVIDER_ID}', '${PROCEDURE_ID}', 'SEGUNDA', '08:00', '12:00', 30, 2, true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO agendamento.slots (id, schedule_id, data_hora, capacidade, reservados, status)
VALUES
  (gen_random_uuid(), '${SCHEDULE_ID}', (CURRENT_DATE + INTERVAL '7 days' + TIME '08:00')::timestamp, 2, 0, 'DISPONIVEL'),
  (gen_random_uuid(), '${SCHEDULE_ID}', (CURRENT_DATE + INTERVAL '7 days' + TIME '08:30')::timestamp, 2, 0, 'DISPONIVEL')
ON CONFLICT (schedule_id, data_hora) DO NOTHING;
SQL

echo "Seed concluido."
