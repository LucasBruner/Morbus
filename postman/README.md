# Postman — Coleções

| Coleção | Escopo |
|---|---|
| `auth-service.postman_collection.json` | Endpoints isolados do auth-service |
| `queue-service.postman_collection.json` | Endpoints isolados do queue-service |
| `regulacao-service.postman_collection.json` | Endpoints isolados do regulacao-service |
| `e2e-flows.postman_collection.json` | **Fluxos ponta-a-ponta** entre todos os serviços, reproduzindo `docs/sequencias-diagrama.md` (1 a 13) |

As três primeiras testam um serviço por vez. A `e2e-flows` testa jornadas completas — inclui a propagação assíncrona via RabbitMQ entre auth-service, queue-service, regulacao-service, agendamento-service e notification-service.

## Como rodar a `e2e-flows`

```bash
docker-compose up -d
./postman/seed-agendamento.sh          # obrigatório antes da Flow 10 — ver "Limitações conhecidas" #2
newman run postman/e2e-flows.postman_collection.json --delay-request 300
```

Ou importe `e2e-flows.postman_collection.json` no Postman e rode com o Collection Runner, na ordem em que as pastas aparecem (a coleção é stateful: cada pasta depende de variáveis setadas pelas anteriores).

Rode contra um ambiente limpo (`docker-compose down -v && docker-compose up -d`) para resultados determinísticos — alguns testes (`call-next`) assumem que não há entradas `AGUARDANDO` deixadas por execuções anteriores.

## Limitações conhecidas (encontradas no código ao montar esta coleção)

Estas não são falhas da coleção — são comportamentos reais do código atual, confirmados lendo `JwtAuthenticationFilter`, `ScheduleController`, `AppointmentController` e os use cases de regulacao-service/agendamento-service. Ficam documentadas aqui porque não constavam em `docs/api-contract.md` nem em `docs/sequencias-diagrama.md`.

**1. `username` precisa ser um UUID para logins usados em regulacao-service ou agendamento-service.**
Ambos os serviços fazem `UUID userId = UUID.fromString(username)` incondicionalmente dentro do `JwtAuthenticationFilter`, para qualquer role — sem try/catch. O auth-service não impõe formato de UUID para `username` (só 3–100 caracteres), então um username "normal" (ex: `dr.joao`) autentica no auth-service e no queue-service, mas quebra com exceção não tratada no primeiro request a regulacao-service ou agendamento-service. Por isso a coleção registra `SOLICITANTE`, `REGULADOR`, `EXECUTANTE` e o paciente com `{{$randomUUID}}` como username. Para o paciente especificamente, o username **precisa ser exatamente** o mesmo UUID do `Patient` cadastrado no queue-service — é esse valor que `AppointmentController.confirmar()` compara para autorizar a confirmação de presença.

**2. `ROLE_EXECUTANTE` nunca consegue chamar endpoints restritos por unidade — sempre 403.**
`POST /api/v1/schedules`, `.../block`, `.../unblock`, `PATCH .../attend` e `POST .../falta` comparam `principal.unitId()` (extraído do JWT) com a unidade do recurso. O auth-service (`JwtService.generateToken`) nunca adiciona um claim `unitId` ao token — só `sub` e `role`. Então `principal.unitId()` é sempre `null`, a comparação falha, e o resultado é `403 AccessDeniedException` para qualquer EXECUTANTE, não importa qual unidade ele deveria representar.

Consequência prática: **não existe caminho público via API para cadastrar `Schedule`/`Slot` no agendamento-service**. Por isso a Flow 10 da coleção depende de `postman/seed-agendamento.sh`, que insere os dados diretamente no Postgres antes de rodar a coleção. A Flow 13 ("Paciente Faltou") documenta esse 403 explicitamente em vez de tentar contorná-lo.

Corrigir isso exigiria o auth-service emitir um claim `unitId` (e alguma forma de associar um usuário EXECUTANTE a uma unidade no cadastro) — fora do escopo desta coleção de testes.

**3. `POST /api/v1/queue/call-next` é `hasRole('MEDICO')`, mesmo nos fluxos onde o diagrama mostra o Regulador chamando.**
`docs/sequencias-diagrama.md` (Flow 10) mostra `Regulador->>QS: POST /api/v1/queue/call-next`, mas `QueueController.callNext()` está anotado `@PreAuthorize("hasRole('MEDICO')")` — um token REGULADOR recebe 403 aqui. A coleção usa `{{medicoToken}}` nesse passo para refletir o comportamento real do código, não o diagrama.

## Seed do agendamento-service (`seed-agendamento.sh`)

Insere um `HealthUnit` + `Provider` + `Schedule` (segunda-feira, 08h–12h, slots de 30min) + 2 `Slot`s `DISPONIVEL` daqui a 7 dias, usando o `procedureId` do código SIGTAP passado como argumento (padrão: `0301010072`, o mesmo usado por padrão na coleção). O `unitId` usado (`aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa`) é o mesmo valor default da variável de coleção `agendamentoUnitId` — se você alterar um, altere o outro.
