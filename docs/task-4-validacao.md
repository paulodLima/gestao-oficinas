# Validação — tarefa 4: cadastro de clientes e veículos

Data: 19/09/2026. Escopo: seção 4.0 de [tasks.md](tasks.md). Revisão: [4_task_review.md](4_task_review.md).

## Entrega

Cadastro, listagem, busca e edição de clientes e veículos com isolamento por oficina. CPF,
placas antigas e Mercosul são validados e normalizados; duplicidades são restritas à oficina.
Um cliente pode ter vários veículos e cada veículo mantém somente um responsável atual.

A transferência usa versão otimista, encerra o vínculo atual e cria um vínculo novo sem apagar
o anterior. A resposta expõe somente o responsável atual. O e-mail do cliente pode ser
confirmado por código de uso único com HMAC, expiração, limite de tentativas, intervalo de
reenvio e limites por cliente/IP.

O Angular oferece rota autenticada, formulários reativos, busca por CPF/placa com ou sem
máscara, estados de carregamento/erro/sucesso e fluxo responsivo de transferência.

## Evidências executadas

| Verificação | Resultado |
| --- | --- |
| `mvn test` | 33 testes aprovados, zero falhas; PostgreSQL 17 via Testcontainers |
| `ng test --watch=false --browsers=ChromeHeadless` | 13 testes aprovados |
| `ng build` | Build de produção aprovado sem estouro de orçamento |
| `docker compose up -d --build` | API e frontend reconstruídos; PostgreSQL saudável |
| `playwright test` | 6 jornadas aprovadas em desktop e mobile |

## Cobertura

- CPF válido/inválido e placas antiga/Mercosul válidas/inválidas.
- Cliente com vários veículos e busca com máscara de CPF/placa.
- Duplicidade dentro da oficina e mesmos dados permitidos em outra oficina.
- Recursos de outra oficina retornam 404 em leitura e escrita.
- CSRF, autenticação, paginação e conflito de versão.
- Código de e-mail de uso único e remoção da verificação após troca do endereço.
- Transferência preserva os dois vínculos e recusa versão antiga.
- Jornada completa em desktop/mobile e ausência de overflow a 320 px.

## Limites

A entidade de ordem de serviço será criada na tarefa 5. Nessa tarefa será ligada ao cliente
responsável no momento da abertura e a transferência passará a ser bloqueada enquanto houver
OS ativa. O E2E móvel usa Chromium emulado; Safari/iOS e aparelhos físicos ficam para a
validação final do MVP.
