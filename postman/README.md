# Postman — Coleções

| Coleção | Escopo |
|---|---|
| `auth-service.postman_collection.json` | Endpoints isolados do auth-service |
| `queue-service.postman_collection.json` | Endpoints isolados do queue-service |
| `regulacao-service.postman_collection.json` | Endpoints isolados do regulacao-service |
| `e2e-flows.postman_collection.json` | **Fluxos ponta-a-ponta** entre todos os serviços, reproduzindo `docs/sequencias-diagrama.md` (1 a 13) + Flow 14 extra |

As três primeiras testam um serviço por vez. A `e2e-flows` testa jornadas completas — inclui a propagação assíncrona via RabbitMQ entre auth-service, queue-service, regulacao-service, agendamento-service e notification-service.

## Como rodar a `e2e-flows`

```bash
docker-compose up -d
./postman/seed-agendamento.sh          # obrigatório antes da Flow 10 — ver seção "Seed do agendamento-service"
newman run postman/e2e-flows.postman_collection.json --delay-request 300
```

Ou importe `e2e-flows.postman_collection.json` no Postman e rode com o Collection Runner, na ordem em que as pastas aparecem (a coleção é stateful: cada pasta depende de variáveis setadas pelas anteriores).

Rode contra um ambiente limpo (`docker-compose down -v && docker-compose up -d`) para resultados determinísticos — alguns testes (`call-next`) assumem que não há entradas `AGUARDANDO` deixadas por execuções anteriores.

## Limitações conhecidas (encontradas no código ao montar esta coleção)

Estas não são falhas da coleção — são comportamentos reais do código atual, confirmados lendo `JwtAuthenticationFilter`, `ScheduleController`, `AppointmentController` e os use cases de regulacao-service/agendamento-service. Ficam documentadas aqui porque não constavam em `docs/api-contract.md` nem em `docs/sequencias-diagrama.md`.

**1. `username` precisa ser um UUID para logins usados em regulacao-service ou agendamento-service.**
Ambos os serviços fazem `UUID userId = UUID.fromString(username)` incondicionalmente dentro do `JwtAuthenticationFilter`, para qualquer role — sem try/catch. O auth-service não impõe formato de UUID para `username` (só 3–100 caracteres), então um username "normal" (ex: `dr.joao`) autentica no auth-service e no queue-service, mas quebra com exceção não tratada no primeiro request a regulacao-service ou agendamento-service. Por isso a coleção registra `SOLICITANTE`, `REGULADOR`, `EXECUTANTE` e o paciente com `{{$randomUUID}}` como username. Para o paciente especificamente, o username **precisa ser exatamente** o mesmo UUID do `Patient` cadastrado no queue-service — é esse valor que `AppointmentController.confirmar()` compara para autorizar a confirmação de presença.

## Seed do agendamento-service (`seed-agendamento.sh`)

Insere apenas um `HealthUnit` + `Provider` direto no Postgres — não existe API pública em agendamento-service para cadastrar nenhum dos dois. O `unitId` (`aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa`) e o `providerId` (`bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb`) usados são os mesmos valores default das variáveis de coleção `agendamentoUnitId` e `agendamentoProviderId` — se alterar um lado, altere o outro. O script também confere que o `procedureId` do código SIGTAP passado como argumento (padrão: `0301010072`, o mesmo usado por padrão na coleção) existe em `queue.procedures`.

A `Schedule` e os `Slot`s **não são mais inseridos pelo script** — a própria coleção os cria via API real (`POST /api/v1/schedules`, request "Create Schedule" na Flow 10), agora que o gap de `unit_id` no JWT foi corrigido (ver Limitações Conhecidas #2).
