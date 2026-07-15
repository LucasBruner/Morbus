#!/usr/bin/env bash
set -euo pipefail

# Seed minimo (health_unit + provider) para permitir que a "Flow 10 - Solicitacao
# Aprovada -> Fila -> Agendamento" da colecao postman/e2e-flows.postman_collection.json
# crie a schedule/slots via API (request "Create Schedule", POST /api/v1/schedules)
# (ver docs/sequencias-diagrama.md, item 10).
#
# Por que isso ainda e necessario:
# Nao existe API publica em agendamento-service para cadastrar health_units/providers,
# entao esses dois continuam sendo inseridos direto no banco. A schedule e os slots,
# porem, ja sao criados via API pela propria collection — o gap que bloqueava
# ROLE_EXECUTANTE (JWT sem claim unit_id) foi corrigido no auth-service
# (JwtService.generateToken agora inclui "unit_id" quando o usuario tem unitId
# cadastrado). Ver postman/README.md > "Limitacoes Conhecidas" para o que ainda falta.
#
# Uso:
#   ./postman/seed-agendamento.sh [codigo_sigtap]
#
# Requisitos: psql no PATH, docker-compose up (postgres publicado em localhost:5432).

CODIGO="${1:-0301010072}"
UNIT_ID="aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
PROVIDER_ID="bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"

export PGPASSWORD=sus_pass

PROCEDURE_ID=$(psql -h localhost -U sus_user -d sus_queue_db -t -A \
  -c "SELECT id FROM queue.procedures WHERE co_procedimento = '${CODIGO}' LIMIT 1;")

if [ -z "${PROCEDURE_ID}" ]; then
  echo "Procedimento com codigo ${CODIGO} nao encontrado em queue.procedures. O banco subiu corretamente?" >&2
  exit 1
fi

echo "Procedimento ${CODIGO} -> ${PROCEDURE_ID} (usado pela request 'Create Schedule' via {{procedureId}})"
echo "Unidade/provider fixos (devem bater com {{agendamentoUnitId}} e {{agendamentoProviderId}} na colecao Postman): ${UNIT_ID} / ${PROVIDER_ID}"

psql -h localhost -U sus_user -d agendamento_db -v ON_ERROR_STOP=1 <<SQL
INSERT INTO agendamento.health_units (id, nome, cnes, municipio, uf, ativo, endereco, telefone)
VALUES ('${UNIT_ID}', 'UPA Norte', '2077485', 'Sao Paulo', 'SP', true, 'Rua das Flores, 100', '(11) 4002-8922')
ON CONFLICT (id) DO NOTHING;

INSERT INTO agendamento.providers (id, nome, crm, especialidade, unit_id, ativo)
VALUES ('${PROVIDER_ID}', 'Dr. Carlos Melo', 'CRM/SP 98765', 'Clinica Geral', '${UNIT_ID}', true)
ON CONFLICT (id) DO NOTHING;
SQL

echo "Seed concluido (health_unit + provider). Schedule/slots sao criados pela propria collection (Flow 10 / Create Schedule)."
