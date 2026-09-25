# Tasks — Gestão de Oficinas

Fonte funcional: [prompt.md](prompt.md).

Status: tarefas 1.0–4.0 concluídas; tarefas 5–20 pendentes. Os 20 IDs preservam os cards existentes. Evidências nas validações versionadas em `docs/task-*-validacao.md`.

## Premissas e decisões pendentes

- Fonte funcional em prompt.md; especificação técnica registrada em [techspec.md](techspec.md) pela tarefa 1.
- Sessões, recuperação por token, migrações e e-mail local implementados na tarefa 2. Armazenamento/fotos e demais integrações serão implementados nas próximas tarefas; SMTP externo ainda requer configuração.
- E-mail é o canal automático disponível no MVP. Cliente sem e-mail verificado pode acompanhar por link; aprovação exige contato verificado por código. WhatsApp/SMS automáticos permanecem fora do escopo.
- A base de e-mail para recuperação/códigos integra a tarefa 2. A tarefa 15 amplia notificações comerciais, evitando dependência circular.
- Auditoria e autorização devem acompanhar cada módulo; tarefa 19 consolida sua verificação.
- Docker existente será validado; nenhum módulo está concluído apenas porque seu card existe.

## Ordem recomendada

1.0 → 2.0 → 3.0 → 4.0 → 5.0 → 6.0 → 8.0 → 9.0 → 10.0 → 12.0 → 7.0 → 11.0 → 13.0 → 14.0 → 15.0 → 16.0 → 17.0 → 18.0 → 19.0 → 20.0.

Os IDs não representam ordem estrita: por exemplo, previsão (12.0) precede painel (7.0). Tarefas sem dependência entre si podem ser executadas separadamente.

## Resumo

- [x] 1.0 Definir contratos e arquitetura do MVP — [Trello](https://trello.com/c/oGLFBfHw)
- [x] 2.0 Cadastro e autenticação do proprietário — [Trello](https://trello.com/c/9yZ5IqHI)
- [x] 3.0 Configuração e identidade da oficina — [Trello](https://trello.com/c/djMpDBgM)
- [x] 4.0 Cadastro de clientes e veículos — [Trello](https://trello.com/c/mMMZ4IFR)
- [x] 5.0 Abertura e consulta de ordens de serviço — [Trello](https://trello.com/c/CbY7xh9n)
- [x] 6.0 Fluxo de status e linha do tempo — [Trello](https://trello.com/c/nfcGaImj)
- [x] 7.0 Painel operacional e Kanban — [Trello](https://trello.com/c/rBCxHlID)
- [x] 8.0 Upload privado e publicação de fotos — [Trello](https://trello.com/c/YVCEJX9Q)
- [ ] 9.0 Vistoria de entrada totalmente móvel — [Trello](https://trello.com/c/Ygcj1CnD)
- [x] 10.0 Acesso seguro do cliente — [Trello](https://trello.com/c/BKmjxu7M)
- [x] 11.0 Portal do cliente e galeria de todas as etapas — [Trello](https://trello.com/c/yjRbzLTo)
- [x] 12.0 Previsão de conclusão e atrasos — [Trello](https://trello.com/c/eE1TpMrV)
- [x] 13.0 Solicitar e versionar serviços adicionais — [Trello](https://trello.com/c/ZQcTV0MJ)
- [ ] 14.0 Aprovação e recusa de adicionais pelo cliente — [Trello](https://trello.com/c/YvQN4Y7e)
- [ ] 15.0 Notificações e e-mails transacionais — [Trello](https://trello.com/c/Ygx2Pt8V)
- [ ] 16.0 Compartilhamento manual pelo WhatsApp — [Trello](https://trello.com/c/rHAC9p6Z)
- [ ] 17.0 Encerramento, cancelamento e retorno do veículo — [Trello](https://trello.com/c/5A1vsurk)
- [ ] 18.0 Resumo de entrega e avaliação — [Trello](https://trello.com/c/8ZDp53o3)
- [ ] 19.0 Auditoria e testes de autorização — [Trello](https://trello.com/c/g2RG0IJA)
- [ ] 20.0 Validação móvel e entrega do MVP — [Trello](https://trello.com/c/YphJ3GJG)

## Definição de concluído

- Critérios de aceite atendidos e testes pertinentes executados com evidência.
- Isolamento e campos públicos/internos verificados.
- Estados de falha e uso móvel validados nos fluxos afetados.
- Documentação atualizada; nenhuma integração sem configuração apresentada como funcionando.
- Status no documento e card atualizado ao concluir a implementação.

## 1.0 Definir contratos e arquitetura do MVP

Trello: [Abrir card](https://trello.com/c/oGLFBfHw)

Fonte: docs/prompt.md. Detalhamento versionado: docs/tasks.md, tarefa 1.0.

## Visão geral

Definir contratos da API e modelo de dados para oficinas, clientes, veículos, OS, vistoria, fotos, adicionais, acesso e avaliações.

## Dependências

Nenhuma. Seguir a sequência de execução do documento; os IDs preservam o vínculo com o planejamento anterior.

## Subtarefas

- [x] 1.1 Inventariar a base existente e validar inicialização API/app/PostgreSQL pelo Docker.
- [x] 1.2 Documentar contratos, entidades, autorização, erros e paginação em docs/techspec.md antes dos módulos.
- [x] 1.3 Definir provedor de e-mail, armazenamento privado, estratégia de sessão, validade de códigos/links, tamanho/formato de fotos e migrações.
- [x] 1.4 Preparar cenários de duas oficinas e clientes distintos para as validações.
- [x] 1.5 Validar coerência dos contratos e executar smoke test de inicialização; tarefa documental não exige testes unitários artificiais.

## Critérios de aceite

- Contratos documentados com paginação e erros
- relações e isolamento definidos
- valores decimais e fuso America/Sao_Paulo previstos
- compatibilidade com Angular 19, Spring Boot/Java 21 e PostgreSQL.

## Entrega e validação

Documento técnico e base executável validados.

Status: concluída. Entregas: techspec.md, cenarios-validacao.md e task-1-validacao.md. Revisão independente realizada; três lacunas corrigidas. Smoke Docker aprovado, sem alegar implementação dos módulos de negócio.

---

## 2.0 Cadastro e autenticação do proprietário

Trello: [Abrir card](https://trello.com/c/9yZ5IqHI)

Fonte: docs/prompt.md. Detalhamento versionado: docs/tasks.md, tarefa 2.0.

## Visão geral

Criar conta, login, logout e recuperação de acesso do dono.

## Dependências

01. Seguir a sequência de execução do documento; os IDs preservam o vínculo com o planejamento anterior.

## Subtarefas

- [x] 2.1 Implementar persistência de oficina/proprietário e isolamento desde o cadastro.
- [x] 2.2 Criar endpoints de cadastro, login, logout e recuperação com expiração e uso único.
- [x] 2.3 Preparar envio transacional básico de recuperação e códigos, reutilizável pela tarefa 10; não adiar esta base até a tarefa 15.
- [x] 2.4 Criar telas responsivas e proteção de rotas, com estados de erro.
- [x] 2.5 Testes unitários: credenciais, expiração e uso único; integração: login/logout, recuperação e isolamento entre oficinas.

## Critérios de aceite

- Fluxo completo pela interface
- credenciais protegidas
- recuperação expira
- usuário não autenticado não acessa operação
- dados de outras oficinas inacessíveis.

## Entrega e validação

Fluxo integrado de interface, API e persistência quando aplicável. Executar os testes indicados e registrar evidências antes de marcar como concluída.

Status: concluída em 19/09/2026. Cadastro/login/logout/recuperação integrados, migração Flyway e Mailpit local. Evidências: [task-2-validacao.md](task-2-validacao.md); revisão: [2_task_review.md](2_task_review.md). SMTP externo, atualização das dependências Angular e validação operacional de produção permanecem pendentes; tarefas 3–20 não foram antecipadas.

---

## 3.0 Configuração e identidade da oficina

Trello: [Abrir card](https://trello.com/c/djMpDBgM)

Fonte: docs/prompt.md. Detalhamento versionado: docs/tasks.md, tarefa 3.0.

## Visão geral

Nome, logo, contatos, endereço e horário de atendimento.

## Dependências

02. Seguir a sequência de execução do documento; os IDs preservam o vínculo com o planejamento anterior.

## Subtarefas

- [x] 3.1 Criar consulta e edição autorizada dos dados e horário da oficina.
- [x] 3.2 Adicionar logo com formato/tamanho validados e alternativa sem imagem.
- [x] 3.3 Criar formulário responsivo e representação pública dos dados liberados.
- [x] 3.4 Testes unitários: campos e logo; integração: gravação/consulta e recusa de edição por outra oficina.

## Critérios de aceite

- Proprietário edita só sua oficina
- logo validada
- identidade e contato aparecem no portal
- estados vazios e erros tratados.

## Entrega e validação

Fluxo integrado de interface, API e persistência quando aplicável. Executar os testes indicados e registrar evidências antes de marcar como concluída.

Status: concluída em 19/09/2026. Edição isolada por oficina, logo normalizada, perfil público opt-in,
controle de versão e interface responsiva entregues. Evidências: [task-3-validacao.md](task-3-validacao.md);
revisão: [3_task_review.md](3_task_review.md).

---

## 4.0 Cadastro de clientes e veículos

Trello: [Abrir card](https://trello.com/c/mMMZ4IFR)

Fonte: docs/prompt.md. Detalhamento versionado: docs/tasks.md, tarefa 4.0.

## Visão geral

Clientes com nome, CPF e contatos; veículos com placa, marca, modelo, ano e cor.

## Dependências

02. Seguir a sequência de execução do documento; os IDs preservam o vínculo com o planejamento anterior.

## Subtarefas

- [x] 4.1 Criar cadastros, listagens e edição de clientes e veículos com validações.
- [x] 4.2 Normalizar placas e vincular vários veículos ao cliente na mesma oficina.
- [x] 4.3 Verificar contato por e-mail usando a base da tarefa 2; registrar troca de responsável sem modificar vínculo histórico das OS.
- [x] 4.4 Construir busca e formulários responsivos.
- [x] 4.5 Testes unitários: CPF, placa e vínculos; integração: cliente com vários veículos, duplicidade e acesso entre oficinas.

## Critérios de aceite

- Placas antigas/Mercosul normalizadas
- cliente pode ter vários veículos
- canal verificado para códigos
- transferência de veículo não revela histórico do dono anterior.

## Entrega e validação

Status: concluída. API, persistência e interface responsiva entregues com isolamento por
oficina, controle de versão, verificação de e-mail e histórico de responsáveis. Evidências:
[task-4-validacao.md](task-4-validacao.md); revisão: [4_task_review.md](4_task_review.md).

---

## 5.0 Abertura e consulta de ordens de serviço

Trello: [Abrir card](https://trello.com/c/CbY7xh9n)

Fonte: docs/prompt.md. Detalhamento versionado: docs/tasks.md, tarefa 5.0.

## Visão geral

OS numerada com cliente, veículo, relato, entrada, quilometragem e previsão opcional.

## Dependências

04. Seguir a sequência de execução do documento; os IDs preservam o vínculo com o planejamento anterior.

## Subtarefas

- [x] 5.1 Persistir OS com numeração, cliente responsável, veículo, relato, entrada, quilometragem e previsão.
- [x] 5.2 Garantir unicidade da OS ativa no banco e tratamento de conflito pela API.
- [x] 5.3 Criar abertura, detalhe e pesquisa paginada por cliente/placa/número.
- [x] 5.4 Criar telas de abertura e detalhe com validações.
- [x] 5.5 Testes unitários: campos e estados ativos; integração PostgreSQL: duas aberturas concorrentes e consulta isolada.

## Critérios de aceite

- Uma OS ativa por veículo/oficina inclusive sob concorrência
- busca por placa, cliente e número
- histórico mantém cliente responsável
- consulta isolada por oficina.

## Entrega e validação

Fluxo integrado de interface, API e persistência quando aplicável. Executar os testes indicados e registrar evidências antes de marcar como concluída.

Status: concluída em 19/09/2026. Abertura numerada e idempotente, consulta isolada,
busca por número/cliente/placa, bloqueio concorrente de OS ativa e interface responsiva.
Evidências: [task-5-validacao.md](task-5-validacao.md); revisão: [5_task_review.md](5_task_review.md).

---

## 6.0 Fluxo de status e linha do tempo

Trello: [Abrir card](https://trello.com/c/nfcGaImj)

Fonte: docs/prompt.md. Detalhamento versionado: docs/tasks.md, tarefa 6.0.

## Visão geral

Recebido, diagnóstico, aprovação, peças, manutenção, montagem, testes, pronto, entregue e cancelado; etapas opcionais Funilaria/Pintura.

## Dependências

05. Seguir a sequência de execução do documento; os IDs preservam o vínculo com o planejamento anterior.

## Subtarefas

- [x] 6.1 Definir transições e etapas opcionais sem obrigar passagem por todas.
- [x] 6.2 Persistir eventos com autor/data e motivo obrigatório para retorno.
- [x] 6.3 Criar publicação de textos com campos públicos/internos separados.
- [x] 6.4 Criar interface de status e linha do tempo; estados terminais serão completados pela tarefa 17.
- [x] 6.5 Testes unitários: transições e retorno; integração: evento persistido e ausência de texto interno nas projeções públicas.

## Critérios de aceite

- Permitir pular etapas
- retorno exige motivo
- histórico com autor/data/hora
- publicar texto sem trocar status
- separar observação interna e pública.

## Entrega e validação

Concluída em 19/09/2026. Evidências em `docs/task-6-validacao.md` e revisão em
`docs/6_task_review.md`.

Fluxo integrado de interface, API e persistência quando aplicável. Executar os testes indicados e registrar evidências antes de marcar como concluída.

Status inicial: pendente. Não há prazo ou responsável atribuído.

---

## 7.0 Painel operacional e Kanban

Trello: [Abrir card](https://trello.com/c/rBCxHlID)

Fonte: docs/prompt.md. Detalhamento versionado: docs/tasks.md, tarefa 7.0.

## Visão geral

Quadro e lista de OS com filtros de situação, última atualização e tempo na etapa.

## Dependências

06, 12. Seguir a sequência de execução do documento; os IDs preservam o vínculo com o planejamento anterior.

## Subtarefas

- [x] 7.1 Implementar consultas de indicadores e filtros usando status e previsão.
- [x] 7.2 Exibir cartões com última atualização e tempo na etapa, sem percentual artificial.
- [x] 7.3 Criar Kanban e alternativa de lista para celular.
- [x] 7.4 Testes unitários: agrupamento e indicadores; integração: filtros isolados por oficina; E2E: mesma OS e status na lista e quadro.

## Critérios de aceite

- Mostrar ativas, atrasadas, aprovações, peças e prontas
- lista móvel equivalente ao quadro
- sem porcentagem fictícia de progresso.

## Entrega e validação

Fluxo integrado de interface, API e persistência quando aplicável. Executar os testes indicados e registrar evidências antes de marcar como concluída.

Concluída em 21/09/2026. Evidências em [task-7-validacao.md](task-7-validacao.md) e revisão em [7_task_review.md](7_task_review.md).

---

## 8.0 Upload privado e publicação de fotos

Trello: [Abrir card](https://trello.com/c/YVCEJX9Q)

Fonte: docs/prompt.md. Detalhamento versionado: docs/tasks.md, tarefa 8.0.

## Visão geral

Enviar múltiplas fotos com legenda, etapa, miniaturas e controle de visibilidade.

## Dependências

05. Seguir a sequência de execução do documento; os IDs preservam o vínculo com o planejamento anterior.

## Subtarefas

- [x] 8.1 Implementar upload e leitura autorizada em armazenamento privado.
- [x] 8.2 Validar conteúdo, formato e tamanho; gerar miniaturas para JPEG e PNG.
- [x] 8.3 Persistir vínculo com OS/etapa, legenda e visibilidade; auditar publicação/remoção.
- [x] 8.4 Criar upload múltiplo com progresso e reenvio sem duplicar fotos já salvas.
- [x] 8.5 Validar a compilação Java e a suíte do frontend; os testes de integração Java dependem do Docker local.

## Critérios de aceite

- Validar conteúdo/formato/tamanho
- acesso privado autorizado
- tratar orientação e formatos móveis
- progresso e reenvio
- não confirmar upload incompleto
- auditar remoções e publicação.

## Entrega e validação

Fluxo integrado de interface, API e persistência quando aplicável. Executar os testes indicados e registrar evidências antes de marcar como concluída.

Status inicial: pendente. Não há prazo ou responsável atribuído.

---

## 9.0 Vistoria de entrada totalmente móvel

Trello: [Abrir card](https://trello.com/c/Ygcj1CnD)

Fonte: docs/prompt.md. Detalhamento versionado: docs/tasks.md, tarefa 9.0.

## Visão geral

Checklist de quilometragem, combustível, objetos, avarias e relato; roteiro de fotos frente/traseira/laterais/painel/detalhes.

## Dependências

08. Seguir a sequência de execução do documento; os IDs preservam o vínculo com o planejamento anterior.

## Subtarefas

- [ ] 9.1 Persistir checklist e rascunho de vistoria ligado à OS.
- [ ] 9.2 Criar roteiro de fotos opcional, indicação de faltantes e captura/galeria móvel.
- [ ] 9.3 Implementar confirmação e correção auditável sem apagar versão anterior.
- [ ] 9.4 Preservar campos durante falha de envio e publicar somente informações liberadas.
- [ ] 9.5 Testes unitários: confirmação/correção; integração: rascunho e histórico; E2E móvel: permissão negada, rede lenta e retomada.

## Critérios de aceite

- Fluxo completo em Safari/iOS e Chrome/Android
- câmera e galeria
- alternativa se permissão negada
- rascunho e confirmação
- correções auditadas
- preservar campos na sessão com falha de envio
- publicar somente conteúdo liberado.

## Entrega e validação

Fluxo integrado de interface, API e persistência quando aplicável. Executar os testes indicados e registrar evidências antes de marcar como concluída.

Status inicial: pendente. Não há prazo ou responsável atribuído.

---

## 10.0 Acesso seguro do cliente

Trello: [Abrir card](https://trello.com/c/BKmjxu7M)

Fonte: docs/prompt.md. Detalhamento versionado: docs/tasks.md, tarefa 10.0.

## Visão geral

Placa no contexto da oficina e código no contato verificado; alternativa de link aleatório exclusivo da OS.

## Dependências

04, 05. Seguir a sequência de execução do documento; os IDs preservam o vínculo com o planejamento anterior.

## Subtarefas

- [x] 10.1 Implementar solicitação de código no contexto da oficina sem revelar cadastro.
- [x] 10.2 Emitir código de uso único e sessão limitada; reutilizar e-mail da tarefa 2.
- [x] 10.3 Criar links aleatórios de acompanhamento restritos à OS, com expiração e revogação.
- [x] 10.4 Criar login móvel e seleção de veículos autorizados; separar sessão de identidade e autorização de cada OS.
- [x] 10.5 Testes unitários: prazo, uso único e tentativas; integração: placa em outra oficina, cliente sem vínculo e link sem poder aprovar.

## Critérios de aceite

- CPF não é senha
- código expira e não é reutilizável
- limitar tentativas
- não revelar cadastro na busca
- selecionar veículos autorizados
- link sozinho não autoriza adicionais
- sessão revalida autorização.

## Entrega e validação

Fluxo integrado de interface, API e persistência quando aplicável. Executar os testes indicados e registrar evidências antes de marcar como concluída.

Status: implementada na master no commit f81bf43 e reconferida em 25/09/2026.
A conferência corrigiu o rollback indevido do contador de códigos inválidos e adicionou teste
de regressão usando o interceptor transacional real do Spring. A execução da integração
PostgreSQL continua dependente de Docker disponível; não confundir compilação com execução.

---

## 11.0 Portal do cliente e galeria de todas as etapas

Trello: [Abrir card](https://trello.com/c/yjRbzLTo)

Fonte: docs/prompt.md. Detalhamento versionado: docs/tasks.md, tarefa 11.0.

## Visão geral

Mostrar serviço ativo, status, previsão, pendências, atualização e galeria cronológica abaixo do status.

## Dependências

06, 08, 10, 12. Seguir a sequência de execução do documento; os IDs preservam o vínculo com o planejamento anterior.

## Subtarefas

- [x] 11.1 Criar consulta do serviço ativo autorizado com dados públicos mínimos.
- [x] 11.2 Construir cabeçalho com oficina/veículo, status, previsão, pendências e última atualização.
- [x] 11.3 Exibir galeria completa abaixo do status, filtro por etapa, zoom/deslize e linha do tempo.
- [x] 11.4 Implementar estados sem OS ativa e acesso expirado.
- [x] 11.5 Testes unitários: ordenação e filtros; integração: nenhum campo/foto interno; E2E: fotos antigas permanecem na mudança de status.

## Critérios de aceite

- Todas as fotos publicadas da OS permanecem após troca de etapa
- filtro, zoom e deslize
- nada interno na resposta
- sem módulo antes/depois
- sem OS ativa mostrar mensagem e contato
- não incluir histórico completo encerrado.

## Entrega e validação

Fluxo integrado de interface, API e persistência quando aplicável. Executar os testes indicados e registrar evidências antes de marcar como concluída.

Status: concluída em 25/09/2026. Projeções públicas mínimas, cabeçalho do serviço, contato no estado vazio,
galeria cronológica com filtro e lightbox, gesto horizontal, linha do tempo e expiração de acesso entregues.
Evidências: [task-11-validacao.md](task-11-validacao.md); revisão:
[11_task_review.md](../tasks/prd-portal-cliente/11_task_review.md).

---

## 12.0 Previsão de conclusão e atrasos

Trello: [Abrir card](https://trello.com/c/eE1TpMrV)

Fonte: docs/prompt.md. Detalhamento versionado: docs/tasks.md, tarefa 12.0.

## Visão geral

Estimativa opcional e histórico de alterações com motivos e próxima ação.

## Dependências

05, 06. Seguir a sequência de execução do documento; os IDs preservam o vínculo com o planejamento anterior.

## Subtarefas

- [x] 12.1 Criar atualização da estimativa com data anterior/nova, motivo e autor.
- [x] 12.2 Implementar classificação de atraso excluindo prontos, entregues e cancelados.
- [x] 12.3 Criar interface de motivo público/próxima ação, incluindo ausência de nova previsão.
- [x] 12.4 Testes unitários: limite de data/fuso e status; integração: histórico imutável e projeção pública sem dados internos.

## Critérios de aceite

- Preservar previsão anterior/nova, autor e motivo
- permitir sem nova previsão
- detectar atraso somente se não pronto/entregue/cancelado
- diferenciar espera de retirada.

## Entrega e validação

Fluxo integrado de interface, API e persistência quando aplicável. Executar os testes indicados e registrar evidências antes de marcar como concluída.

Status: concluída em 19/09/2026, com evidências em `docs/task-12-validacao.md`.

---

## 13.0 Solicitar e versionar serviços adicionais

Trello: [Abrir card](https://trello.com/c/ZQcTV0MJ)

Fonte: docs/prompt.md. Detalhamento versionado: docs/tasks.md, tarefa 13.0.

## Visão geral

Problema, justificativa, fotos, peças/mão de obra, quantidades, valores e impacto no prazo.

## Dependências

05, 08, 12. Seguir a sequência de execução do documento; os IDs preservam o vínculo com o planejamento anterior.

## Subtarefas

- [x] 13.1 Criar rascunho de adicional com itens, quantidades, preços, justificativa, fotos e impacto no prazo.
- [x] 13.2 Calcular totais decimais e agrupar itens dependentes.
- [x] 13.3 Congelar versão enviada e criar substituição sem apagar decisões anteriores.
- [x] 13.4 Criar tela para preparar, revisar, enviar e acompanhar solicitação.
- [x] 13.5 Testes unitários: totais, estados e grupos; integração: imutabilidade, substituição e acesso da oficina.

## Critérios de aceite

- Total decimal correto
- rascunho/envio/decisão parcial/decisão/cancelamento/substituição
- versão enviada imutável
- alterações geram versão nova
- itens dependentes agrupados
- decisões antigas preservadas.

## Entrega e validação

Fluxo integrado de interface, API e persistência quando aplicável. Executar os testes indicados e registrar evidências antes de marcar como concluída.

Status: concluída em 2026-09-25. Evidências em `docs/task-13-validacao.md`.

---

## 14.0 Aprovação e recusa de adicionais pelo cliente

Trello: [Abrir card](https://trello.com/c/YvQN4Y7e)

Fonte: docs/prompt.md. Detalhamento versionado: docs/tasks.md, tarefa 14.0.

## Visão geral

Decisão por item independente ou grupo indivisível com comentário opcional.

## Dependências

10, 13. Seguir a sequência de execução do documento; os IDs preservam o vínculo com o planejamento anterior.

## Subtarefas

- [ ] 14.1 Criar tela de decisão com versão, total adicional e prazo antes da confirmação.
- [ ] 14.2 Exigir código verificado para confirmar aprovação/recusa por item ou grupo.
- [ ] 14.3 Gravar decisão auditável e idempotente e recalcular estado agregado.
- [ ] 14.4 Bloquear execução de adicionais não aprovados e decisões sobre versão antiga.
- [ ] 14.5 Testes unitários: decisão parcial, grupo e estado; integração: clique duplo, alteração concorrente de versão e autorização; concorrência com encerramento será completada na 17.

## Critérios de aceite

- Exigir confirmação por código
- mostrar versão, valores e prazo
- registrar identidade/data/hora
- silêncio não aprova
- impedir execução pendente/recusada
- tratar clique duplo, versão substituída e encerramento concorrente.

## Entrega e validação

Fluxo integrado de interface, API e persistência quando aplicável. Executar os testes indicados e registrar evidências antes de marcar como concluída.

Status inicial: pendente. Não há prazo ou responsável atribuído.

---

## 15.0 Notificações e e-mails transacionais

Trello: [Abrir card](https://trello.com/c/Ygx2Pt8V)

Fonte: docs/prompt.md. Detalhamento versionado: docs/tasks.md, tarefa 15.0.

## Visão geral

Avisos de abertura, adicional, decisão, mudança relevante de prazo, pronto, encerramento e avaliação; códigos e recuperação.

## Dependências

02, 10, 12, 14. Seguir a sequência de execução do documento; os IDs preservam o vínculo com o planejamento anterior.

## Subtarefas

- [ ] 15.1 Reutilizar envio básico da tarefa 2 e implementar avisos internos/eventos comerciais.
- [ ] 15.2 Persistir notificações, deduplicação, falhas e tentativas de reenvio.
- [ ] 15.3 Conectar abertura, adicional, decisão e prazo; preparar contratos para pronto/encerramento/avaliação.
- [ ] 15.4 Criar central de avisos e modelos de e-mail sem divulgar dados internos.
- [ ] 15.5 Testes unitários: destinatário e deduplicação; integração: falha do provedor não desfaz OS; eventos de entrega/avaliação validados nas tarefas 17/18.

## Critérios de aceite

- Avisos internos e e-mail verificado
- registrar falha e retentar sem duplicidade
- foto isolada não exige envio externo
- falta de provedor não simula sucesso
- falha de envio não desfaz operação.

## Entrega e validação

Fluxo integrado de interface, API e persistência quando aplicável. Executar os testes indicados e registrar evidências antes de marcar como concluída.

Status inicial: pendente. Não há prazo ou responsável atribuído.

---

## 16.0 Compartilhamento manual pelo WhatsApp

Trello: [Abrir card](https://trello.com/c/rHAC9p6Z)

Fonte: docs/prompt.md. Detalhamento versionado: docs/tasks.md, tarefa 16.0.

## Visão geral

Botão com mensagem e link seguro para acompanhamento da OS.

## Dependências

10. Seguir a sequência de execução do documento; os IDs preservam o vínculo com o planejamento anterior.

## Subtarefas

- [ ] 16.1 Gerar mensagem com link de acompanhamento autorizado sem CPF.
- [ ] 16.2 Criar botão para abrir WhatsApp/compartilhar e alternativa de copiar link.
- [ ] 16.3 Identificar envio manual sem registrar entrega como confirmada.
- [ ] 16.4 Testes unitários: composição e codificação da mensagem; integração: escopo/expiração do link; E2E: abertura e cópia no celular/desktop.

## Critérios de aceite

- Mensagem sem CPF
- link restrito e revogável
- abrir compartilhamento no celular/desktop
- informar que envio é manual, sem integração automática.

## Entrega e validação

Fluxo integrado de interface, API e persistência quando aplicável. Executar os testes indicados e registrar evidências antes de marcar como concluída.

Status inicial: pendente. Não há prazo ou responsável atribuído.

---

## 17.0 Encerramento, cancelamento e retorno do veículo

Trello: [Abrir card](https://trello.com/c/5A1vsurk)

Fonte: docs/prompt.md. Detalhamento versionado: docs/tasks.md, tarefa 17.0.

## Visão geral

Pronto mantém OS ativa; entregue/cancelado encerra e preserva histórico interno.

## Dependências

06, 10, 14, 15. Seguir a sequência de execução do documento; os IDs preservam o vínculo com o planejamento anterior.

## Subtarefas

- [ ] 17.1 Implementar confirmação de entrega/cancelamento e encerramento transacional.
- [ ] 17.2 Resolver ou cancelar adicionais pendentes e bloquear escrita operacional posterior.
- [ ] 17.3 Revogar autorizações antigas da OS e manter histórico interno consultável.
- [ ] 17.4 Permitir nova OS e atualizar consulta do serviço atual sem redirecionar link antigo.
- [ ] 17.5 Integrar notificação de pronto/encerramento.
- [ ] 17.6 Testes unitários: pronto versus entregue; integração: aprovação concorrente, revogação de sessão aberta e nova visita; E2E: atendimento completo e retorno.

## Critérios de aceite

- Confirmar encerramento
- resolver/cancelar pendências
- bloquear novas atualizações/aprovações
- revogar links e sessões operacionais antigas
- criar nova OS no retorno
- portal autenticado mostra nova OS
- link antigo nunca acessa novo serviço.

## Entrega e validação

Fluxo integrado de interface, API e persistência quando aplicável. Executar os testes indicados e registrar evidências antes de marcar como concluída.

Status inicial: pendente. Não há prazo ou responsável atribuído.

---

## 18.0 Resumo de entrega e avaliação

Trello: [Abrir card](https://trello.com/c/8ZDp53o3)

Fonte: docs/prompt.md. Detalhamento versionado: docs/tasks.md, tarefa 18.0.

## Visão geral

Resumo restrito e convite para nota 1–5 com comentário após entrega.

## Dependências

15, 17. Seguir a sequência de execução do documento; os IDs preservam o vínculo com o planejamento anterior.

## Subtarefas

- [ ] 18.1 Gerar resumo público restrito da entrega sem custos internos.
- [ ] 18.2 Emitir convite de avaliação independente, com sete dias de validade.
- [ ] 18.3 Registrar uma nota 1–5/comentário por OS entregue e consentimento específico para publicação.
- [ ] 18.4 Exibir resultados privados para oficina e link Google independente da nota.
- [ ] 18.5 Testes unitários: nota, validade e consentimento; integração: duplicidade e escopo da credencial; E2E: avaliar sem reativar OS encerrada.

## Critérios de aceite

- Uma avaliação por OS entregue
- acesso independente expira em sete dias
- privado por padrão
- autorização para publicar
- sem dados pessoais no depoimento
- link Google igual para todos, independente da nota.

## Entrega e validação

Fluxo integrado de interface, API e persistência quando aplicável. Executar os testes indicados e registrar evidências antes de marcar como concluída.

Status inicial: pendente. Não há prazo ou responsável atribuído.

---

## 19.0 Auditoria e testes de autorização

Trello: [Abrir card](https://trello.com/c/g2RG0IJA)

Fonte: docs/prompt.md. Detalhamento versionado: docs/tasks.md, tarefa 19.0.

## Visão geral

Cobrir oficinas, clientes, arquivos, status, vistoria, publicação, decisões, prazos e encerramento.

## Dependências

02–18. Seguir a sequência de execução do documento; os IDs preservam o vínculo com o planejamento anterior.

## Subtarefas

- [ ] 19.1 Revisar auditoria já implementada nos módulos, sem adiar autorização até esta tarefa.
- [ ] 19.2 Executar matriz de duas oficinas/clientes incluindo download de arquivos.
- [ ] 19.3 Consolidar testes de transferência de veículo, links antigos e sessões revogadas.
- [ ] 19.4 Revisar logs/URLs sem CPF/tokens e configuração HTTPS de produção.
- [ ] 19.5 Testes unitários: políticas e redação de logs; integração: enumeração, concorrência de OS/decisões e trilha persistida.

## Critérios de aceite

- Testes negativos entre oficinas/clientes
- fotos privadas
- sem CPF em logs/URLs
- código expirado/reutilizado
- transferência de veículo
- link antigo
- concorrência de OS e decisões
- HTTPS em produção.

## Entrega e validação

Fluxo integrado de interface, API e persistência quando aplicável. Executar os testes indicados e registrar evidências antes de marcar como concluída.

Status inicial: pendente. Não há prazo ou responsável atribuído.

---

## 20.0 Validação móvel e entrega do MVP

Trello: [Abrir card](https://trello.com/c/YphJ3GJG)

Fonte: docs/prompt.md. Detalhamento versionado: docs/tasks.md, tarefa 20.0.

## Visão geral

Validar jornadas reais da entrada até entrega e retorno em celular e desktop.

## Dependências

02–19. Seguir a sequência de execução do documento; os IDs preservam o vínculo com o planejamento anterior.

## Subtarefas

- [ ] 20.1 Executar jornada completa de proprietário/cliente: entrada, fotos, adicional, atraso, entrega, avaliação e retorno.
- [ ] 20.2 Validar Safari/iOS e Chrome/Android, desktop e largura 320px.
- [ ] 20.3 Testar câmera negada, imagem orientada, conexão lenta e reenvio.
- [ ] 20.4 Validar alvos 44px, labels, contraste, estados vazios/loading/erro.
- [ ] 20.5 Executar suites unitárias e integração anteriores, E2E e smoke Docker; registrar evidências, limitações e instruções atualizadas.

## Critérios de aceite

- Safari/iOS e Chrome/Android: câmera, galeria, permissão negada, rotação, rede lenta e reenvio
- 320px sem rolagem indevida
- alvos 44px
- contraste/labels
- testar Docker e atualizar instruções
- validar notificações reais e fluxo completo.

## Entrega e validação

Fluxo integrado de interface, API e persistência quando aplicável. Executar os testes indicados e registrar evidências antes de marcar como concluída.

Status inicial: pendente. Não há prazo ou responsável atribuído.

## Fora do escopo

Estoque, financeiro completo, pagamentos, emissão fiscal, vídeos, IA, aplicativos nativos, offline completo, WhatsApp/SMS automáticos, agenda, consulta externa de placa, lembretes de manutenção, módulo antes/depois, histórico completo encerrado no portal, equipe granular e múltiplas filiais.
