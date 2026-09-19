# Validação — tarefa 5: abertura e consulta de ordens de serviço

Data: 19/09/2026. Escopo: seção 5.0 de [tasks.md](tasks.md). Revisão: [5_task_review.md](5_task_review.md).

## Entrega

Abertura de OS numerada por oficina, com cliente responsável preservado, veículo, relato,
data de entrada, quilometragem, previsão opcional e estado inicial `RECEBIDO`. A API oferece
detalhe e pesquisa paginada por número, cliente e placa, sempre no escopo da sessão.

Uma restrição parcial no PostgreSQL garante uma única OS ativa por veículo/oficina, inclusive
em aberturas concorrentes. A transferência do veículo é recusada enquanto a OS estiver ativa.
Aberturas aceitam `Idempotency-Key` por 24 horas: o mesmo payload reproduz o recurso e um
payload diferente com a mesma chave retorna 409.

O Angular inclui diretório, busca, abertura e detalhe responsivos, com validações e estados
de carregamento, sucesso e erro.

## Evidências executadas

| Verificação | Resultado |
| --- | --- |
| `mvnw.cmd test` | 39 testes aprovados, zero falhas; PostgreSQL 17 via Testcontainers |
| `npm test -- --watch=false --browsers=ChromeHeadless` | 17 testes aprovados |
| `npm run build` | Build de produção aprovado |
| `docker compose up -d --build` | Imagens reconstruídas e serviços iniciados |
| `playwright test` | 8 jornadas aprovadas em desktop e mobile |

## Cobertura

- Numeração sequencial isolada por oficina e estado inicial.
- Busca por cliente, placa formatada e número com prefixo `OS-`.
- Duas aberturas simultâneas para o mesmo veículo: uma 201 e uma 409.
- Repetição idempotente e rejeição de chave reutilizada com payload divergente.
- Bloqueio da transferência com OS ativa e preservação do cliente histórico após encerramento.
- Validação de campos, paginação, autenticação, CSRF e isolamento entre oficinas.
- Jornada completa em desktop/mobile e ausência de overflow a 320 px.

## Limites

Mudanças de estado, linha do tempo e encerramento pela interface pertencem às tarefas 6 e 17.
Nesta etapa o encerramento foi exercitado diretamente no banco apenas para provar a regra de
histórico e liberar a transferência. Safari/iOS e aparelhos físicos ficam para a validação
final do MVP.
