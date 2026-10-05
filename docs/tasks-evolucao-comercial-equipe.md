# Resumo de Tarefas de Implementação — Evolução comercial e equipe

## Tarefas

- [x] 1.0 **E01 — Estabilizar jornadas essenciais e consolidar baseline** — P0 — [Detalhes](../tasks/prd-evolucao-comercial-equipe/1_task.md) — [Validação](../tasks/prd-evolucao-comercial-equipe/validacao-e01.md) — [Trello](https://trello.com/c/KDeVBHBM)
- [ ] 2.0 **E02 — Cadastrar equipe e implementar acesso individual** — P0 — [Detalhes](../tasks/prd-evolucao-comercial-equipe/2_task.md) — [Trello](https://trello.com/c/WBnIIlS1)
- [ ] 3.0 **E03 — Aplicar permissões e atribuir responsáveis às OS** — P0 — [Detalhes](../tasks/prd-evolucao-comercial-equipe/3_task.md) — [Trello](https://trello.com/c/ELD6iTk8)
- [ ] 4.0 **E04 — Criar área mobile da equipe com busca por placa** — P0 — [Detalhes](../tasks/prd-evolucao-comercial-equipe/4_task.md) — [Trello](https://trello.com/c/OVx4UWtv)
- [ ] 5.0 **E05 — Configurar modelos de fluxo e regras de evidência** — P0 — [Detalhes](../tasks/prd-evolucao-comercial-equipe/5_task.md) — [Trello](https://trello.com/c/hJ6n7PFK)
- [ ] 6.0 **E06 — Concluir etapa com fotos e publicação confirmada** — P0 — [Detalhes](../tasks/prd-evolucao-comercial-equipe/6_task.md) — [Trello](https://trello.com/c/l2uOae8E)
- [ ] 7.0 **E07 — Tratar rede instável, reenvio e conflitos de atualização** — P0 — [Detalhes](../tasks/prd-evolucao-comercial-equipe/7_task.md) — [Trello](https://trello.com/c/xqsH7wey)
- [ ] 8.0 **E08 — Atualizar portal com progresso real e comunicação clara** — P1 — [Detalhes](../tasks/prd-evolucao-comercial-equipe/8_task.md) — [Trello](https://trello.com/c/r218P6eN)
- [ ] 9.0 **E09 — Consolidar orçamento autorizado e resumo de entrega** — P1 — [Detalhes](../tasks/prd-evolucao-comercial-equipe/9_task.md) — [Trello](https://trello.com/c/EkHuZQ6w)
- [ ] 10.0 **E10 — Criar indicadores históricos e painel de gargalos** — P1 — [Detalhes](../tasks/prd-evolucao-comercial-equipe/10_task.md) — [Trello](https://trello.com/c/pjFEIPpo)
- [ ] 11.0 **E11 — Confiabilizar notificações e visibilidade das falhas** — P0 — [Detalhes](../tasks/prd-evolucao-comercial-equipe/11_task.md) — [Trello](https://trello.com/c/87e9sqwj)
- [ ] 12.0 **E12 — Preparar armazenamento privado, cotas e recuperação** — P0 — [Detalhes](../tasks/prd-evolucao-comercial-equipe/12_task.md) — [Trello](https://trello.com/c/JNeNEuLq)
- [ ] 13.0 **E13 — Preparar produção, segurança e monitoramento** — P0 — [Detalhes](../tasks/prd-evolucao-comercial-equipe/13_task.md) — [Trello](https://trello.com/c/4DclLIDs)
- [ ] 14.0 **E14 — Entregar onboarding, importação e operação de assinaturas** — P1 — [Detalhes](../tasks/prd-evolucao-comercial-equipe/14_task.md) — [Trello](https://trello.com/c/0rBkMTQH)
- [ ] 15.0 **E15 — Executar piloto comercial e validar lançamento** — P1 — [Detalhes](../tasks/prd-evolucao-comercial-equipe/15_task.md) — [Trello](https://trello.com/c/Tv8O1INQ)

## Contexto e sequência

Escopo e lista aprovados pelo usuário. Fonte: [PRD](../tasks/prd-evolucao-comercial-equipe/prd.md) e [Tech Spec](../tasks/prd-evolucao-comercial-equipe/techspec.md).
IDs E01–E15 identificam esta fase, sem renumerar o MVP. E01 está concluída; E02–E15 permanecem pendentes.
Sequência linear válida: E01 → E02 → E03 → E04 → E05 → E06 → E07 → E08 → E09 → E10 → E11 → E12 → E13 → E14 → E15.
Prioridade não elimina dependências. E09 pode iniciar após E03; E12 após E07. E14 possui subentregas verificáveis.
A área mobile utilizável está em E04–E07, mas produção/piloto depende dos gates posteriores de segurança.

## Dependências e rastreabilidade

| ID | Prioridade | Dependências | Requisitos |
| --- | --- | --- | --- |
| E01 | P0 | Base MVP | RF01 |
| E02 | P0 | E01 | RF02 |
| E03 | P0 | E02 | RF03 |
| E04 | P0 | E03 | RF04 |
| E05 | P0 | E01, E03 | RF05, RF06 |
| E06 | P0 | E04, E05 | RF06, RF08 |
| E07 | P0 | E06 | RF07 |
| E08 | P1 | E05, E07 | RF08 |
| E09 | P1 | E01, E03 | RF09 |
| E10 | P1 | E05, E08, E09 | RF10 |
| E11 | P0 | E02, E06, E09 | RF11 |
| E12 | P0 | E01, E07 | RF12 |
| E13 | P0 | E03, E07, E11, E12 | RF13 |
| E14 | P1 | E03, E12, E13 | RF14 |
| E15 | P1 | E08, E09, E10, E11, E12, E13, E14 | RF15 |

## Definição de concluído

- Critérios atendidos, testes unitários/integração/E2E pertinentes executados e evidências registradas.
- Isolamento, privacidade, concorrência e revogação verificados nos módulos afetados.
- Documentos e card sincronizados; sem marcar conclusão apenas por existir interface.
- Sem credenciais/dados pessoais em evidências.
- Serviços externos e validações manuais não disponíveis permanecem como pendência explícita.

## Decisões e limites

- Funcionário: alcance configurável pelo dono, padrão somente OS atribuídas.
- Publicação é permissão separada e confirmação explícita; foto vinculada à etapa concluída.
- Fotos obrigatórias conforme tipo de etapa; esperas exigem motivo.
- Login individual com convite e senha; sessão inicial de 12h, sem promessa de offline.
- Manter MVP e avaliações ocultas; não recriar módulos existentes.
- Plano comercial/preço/provedores definitivos e contato com oficinas exigem decisão antes da ativação.
- Não provisionar recursos pagos, executar piloto, commitar ou fazer push nesta entrega.

Documento consolidado para leitura: [docs/tasks-evolucao-comercial-equipe.md](tasks-evolucao-comercial-equipe.md).

## Detalhamento completo

# Tarefa 1.0 (E01): Estabilizar jornadas essenciais e consolidar baseline

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Comprovar o estado real do MVP e corrigir regressões antes de ampliar permissões e fluxos.

Status: concluída em 29/09/2026. Prioridade: P0. ID estável: E01 (não altera IDs 01–20 do MVP).
Dependências: nenhuma tarefa nova; base atual do MVP.
Requisitos: RF01.
Trello: [Abrir card](https://trello.com/c/KDeVBHBM).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF01 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [x] 1.1 Mapear requisitos existentes para código, testes e evidências; registrar divergências entre docs/tasks.md e Trello sem modificar status antigo automaticamente.
- [x] 1.2 Executar abertura/edição da OS com datas locais e quilometragem, busca normalizada, upload/publicação, link/código do cliente, aprovação e entrega/retorno.
- [x] 1.3 Documentar defeitos reproduzíveis com massa sintética, prioridade e passos; corrigir apenas regressões confirmadas do fluxo essencial.
- [x] 1.4 Adicionar regressão para cada correção, sem tratar configuração externa ausente como envio bem-sucedido.
- [x] 1.5 Registrar comandos, resultados, limitações e baseline em validacao-e01.md.
- [x] 1.6 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e01.md.

## Detalhes de Implementação

Consultar techspec.md: Arquitetura; Testes; Riscos conhecidos. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Jornada entrada → fotos → adicional → decisão → entrega → nova OS comprovada com UI e API.
- Nenhum defeito bloqueador de perda de dados, isolamento ou acesso permanece aberto para liberar o piloto.
- Recursos já existentes são reutilizados; lacunas e pendências externas ficam explícitas.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [x] Unidade: Conversão de datas com offset, normalização de placa, validação de campos e regressões identificadas.
- [x] Integração: Duas oficinas, concorrência de abertura, fotos privadas e revogação ao encerrar.
- [x] E2E/validação operacional: Proprietário e cliente percorrem atendimento completo em desktop e celular; link antigo não acessa novo atendimento.
- [x] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

Evidências: [validacao-e01.md](../tasks/prd-evolucao-comercial-equipe/validacao-e01.md).

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

docs/tasks.md; docs/techspec.md; oficinas-api/src/test; oficinas-app/e2e; módulos ordem/portal/adicional.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.

---

# Tarefa 2.0 (E02): Cadastrar equipe e implementar acesso individual

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Entregar um ciclo funcional de cadastro, convite, senha, login, recuperação e bloqueio de funcionário.

Status: pendente. Prioridade: P0. ID estável: E02 (não altera IDs 01–20 do MVP).
Dependências: E01.
Requisitos: RF02.
Trello: [Abrir card](https://trello.com/c/WBnIIlS1).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF02 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [ ] 2.1 Criar migrações de equipe e convite com oficina, login normalizado, papel, ativo, versão e hash; preservar contas de proprietários.
- [ ] 2.2 Criar tela do dono para cadastrar nome/celular/função e identificador; emitir convite de uso único, validade proposta de 48h, copiável para envio manual.
- [ ] 2.3 Permitir aceitar convite e definir senha, login no contexto da oficina, logout e recuperação segura; não armazenar senha em texto nem compartilhar credenciais.
- [ ] 2.4 Adaptar principal/revalidação atual do proprietário para distinguir equipe e revogar sessões após bloqueio, redefinição ou mudança de authVersion.
- [ ] 2.5 Aplicar CSRF, limite de tentativas e mensagens que não enumerem contas; manter limite inicial de sessão de 12h.
- [ ] 2.6 Documentar fluxo para quem não tem e-mail: dono emite convite de redefinição auditado, sem conhecer a nova senha.
- [ ] 2.7 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e02.md.

## Detalhes de Implementação

Consultar techspec.md: Componentes identidade/equipe; Modelos; Endpoints; Dependências técnicas. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Funcionário tem identidade própria; convite expirado, revogado ou reutilizado não funciona.
- Bloqueio impede novas operações de sessão aberta; proprietário existente continua acessando.
- Não há acesso administrativo por ter apenas um login de equipe; suporte a permissões é integrado na E03.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [ ] Unidade: Normalização, senha, validade/uso único, rate limit e versão da identidade.
- [ ] Integração: Convite e bloqueio transacionais, login duplicado por contexto, sessão e CSRF, isolamento de oficinas.
- [ ] E2E/validação operacional: Dono convida → funcionário define senha/entra → dono bloqueia → ação seguinte é negada.
- [ ] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

identidade/SecurityConfig.java; identidade/Identidade.java; novos módulos equipe; Angular auth/equipe; db/migration.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.

---

# Tarefa 3.0 (E03): Aplicar permissões e atribuir responsáveis às OS

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Permitir delegar serviços com privilégio mínimo e configurar alcance de atuação.

Status: pendente. Prioridade: P0. ID estável: E03 (não altera IDs 01–20 do MVP).
Dependências: E02.
Requisitos: RF03.
Trello: [Abrir card](https://trello.com/c/ELD6iTk8).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF03 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [ ] 3.1 Documentar matriz proprietário/atendimento/técnico por ação: consultar, atualizar, publicar, atribuir, encerrar, cancelar e acessar valores.
- [ ] 3.2 Implementar padrão ATRIBUIDAS e opção TODAS da mesma oficina por funcionário, editáveis só pelo proprietário.
- [ ] 3.3 Criar atribuição/reatribuição do responsável técnico com versão esperada; manter histórico e impedir vínculo de outra oficina/inativo.
- [ ] 3.4 Aplicar política central em busca, detalhe, foto, download, update e idempotência; não confiar no filtro do frontend.
- [ ] 3.5 Separar permissão de publicação: padrão negada para técnico até habilitação explícita; dono/atendimento libera evidências internas.
- [ ] 3.6 Revalidar bloqueio, função e atribuição em sessão ativa e na confirmação da operação.
- [ ] 3.7 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e03.md.

## Detalhes de Implementação

Consultar techspec.md: Política de acesso; Interfaces; Modelos. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Técnico não vê CPF, contatos completos, valores, configurações nem OS fora do alcance.
- TODAS nunca significa todas as oficinas; busca não revela existência de recurso não autorizado.
- Troca de atribuição/permissão passa a valer nas operações seguintes e não apaga autoria anterior.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [ ] Unidade: Tabela de decisões de política para todos os papéis, alcances e publicação.
- [ ] Integração: IDs cruzados em todos os endpoints/fotos; reatribuição/bloqueio concorrente com escrita.
- [ ] E2E/validação operacional: Dois técnicos com OS diferentes; dono amplia/restringe alcance e UI/API refletem a alteração.
- [ ] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

equipe e política nova; ordem/ServiceOrderController.java; ServicePhotoController.java; identidade; tela administrativa equipe.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.

---

# Tarefa 4.0 (E04): Criar área mobile da equipe com busca por placa

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Entregar entrada enxuta de trabalho no celular, sem os menus administrativos.

Status: pendente. Prioridade: P0. ID estável: E04 (não altera IDs 01–20 do MVP).
Dependências: E03.
Requisitos: RF04.
Trello: [Abrir card](https://trello.com/c/OVx4UWtv).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF04 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [ ] 4.1 Criar rota /equipe protegida e navegação por função, com nome/logo da oficina, funcionário e sair.
- [ ] 4.2 Exibir meus serviços ativos paginados e busca explícita por placa, normalizando maiúsculas/minúsculas e hífen.
- [ ] 4.3 Criar detalhe mínimo: placa/modelo, etapa, orientação operacional e fotos permitidas; evitar expor cadastro completo do cliente.
- [ ] 4.4 Reutilizar ações permitidas de adicionar fotos/observação sem avanço; integrar botão de conclusão quando E06 estiver disponível.
- [ ] 4.5 Cobrir carregando, sem atribuições, não encontrado, expirado, acesso negado e retorno à busca preservada.
- [ ] 4.6 Validar 320/390px, toque 44px, teclado, foco, labels, contraste e orientação do aparelho.
- [ ] 4.7 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e04.md.

## Detalhes de Implementação

Consultar techspec.md: Componentes Angular equipe; Endpoints; E2E. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Funcionário encontra serviço atribuído sem navegar pela administração.
- Busca com placa minúscula/mascarada encontra o mesmo veículo; resultados respeitam política e paginação.
- Tela não mostra funcionalidades bloqueadas e API permanece protegida independentemente da UI.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [ ] Unidade: Busca/normalização, estados vazios, guards e renderização por capacidade.
- [ ] Integração: Consulta paginada real com alcance e DTO mínimo; ausência de campos privados.
- [ ] E2E/validação operacional: Login → busca por placa → detalhe → foto/observação autorizada em mobile e desktop.
- [ ] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

oficinas-app/src/app/equipe (novo); app.routes.ts; auth; ordem; API equipe.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.

---

# Tarefa 5.0 (E05): Configurar modelos de fluxo e regras de evidência

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Adaptar as etapas ao serviço sem adulterar o histórico nem exigir fotos sem sentido.

Status: pendente. Prioridade: P0. ID estável: E05 (não altera IDs 01–20 do MVP).
Dependências: E01, E03.
Requisitos: RF05, RF06.
Trello: [Abrir card](https://trello.com/c/hJ6n7PFK).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF05, RF06 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [ ] 5.1 Criar modelos iniciais de mecânica, funilaria/pintura e rápido com execução, espera e pronto distintos.
- [ ] 5.2 Permitir ao dono ordenar etapas, marcar opcionais e exigir quantidade mínima de fotos por conclusão; começar com mínimo 1 nas etapas configuradas.
- [ ] 5.3 Instanciar snapshot do fluxo ao abrir OS e definir migração explícita para OS legada; não inferir conclusões a partir da posição atual.
- [ ] 5.4 Preservar snapshot em edições do modelo; mudança de fluxo da OS exige motivo, versão e manutenção de eventos antigos.
- [ ] 5.5 Modelar passagem por etapa para retorno/retrabalho, estado pulado com motivo e próximo passo permitido.
- [ ] 5.6 Aplicar política em todas as interfaces, inclusive atualização legada do dono; qualquer exceção administrativa exige autorização específica, motivo e auditoria.
- [ ] 5.7 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e05.md.

## Detalhes de Implementação

Consultar techspec.md: Modelos de fluxo/passagem; Decisões principais. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Editar modelo não altera OS em andamento silenciosamente.
- Espera exige motivo; pronto/entregue não são confundidos; retorno gera nova passagem.
- API rejeita conclusão que não atende política, mesmo por rota legada.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [ ] Unidade: Próxima etapa, opcionais, retornos, espera, política de fotos e contagem até pronto.
- [ ] Integração: Snapshot/versionamento, mudança concorrente, migração de legado e tentativa de bypass.
- [ ] E2E/validação operacional: Dono configura modelo, abre OS e confirma que tela técnica/portal usam o fluxo escolhido.
- [ ] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

ordem/ServiceOrderStatus.java; eventos/serviço de OS; novo fluxo; migrações; Angular ordem/oficina.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.

---

# Tarefa 6.0 (E06): Concluir etapa com fotos e publicação confirmada

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Salvar evidências da etapa terminada e avançar somente após confirmação íntegra do servidor.

Status: pendente. Prioridade: P0. ID estável: E06 (não altera IDs 01–20 do MVP).
Dependências: E04, E05.
Requisitos: RF06, RF08.
Trello: [Abrir card](https://trello.com/c/l2uOae8E).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF06, RF08 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [ ] 6.1 Construir formulário com câmera/galeria, miniaturas, remoção antes do envio, observação e próxima etapa sugerida.
- [ ] 6.2 Exibir claramente etapa concluída, etapa seguinte, quantidade e destino das fotos; não publicar na seleção.
- [ ] 6.3 Preparar upload privado e validar formato/conteúdo/tamanho/pertencimento; exigir fotos novas do lote vinculadas à passagem atual quando obrigatório.
- [ ] 6.4 Finalizar com expectedVersion e chave idempotente, associando evidências à etapa anterior e avançando com auditoria/outbox na transação.
- [ ] 6.5 Sem permissão pública, salvar evidências internas para liberação posterior; sem foto exigida, não mudar a etapa.
- [ ] 6.6 Respeitar autorização de trabalho adicional, OS encerrada, espera e retorno; não permitir funcionário aprovar pelo cliente.
- [ ] 6.7 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e06.md.

## Detalhes de Implementação

Consultar techspec.md: Interfaces: transação de conclusão; Endpoints; Modelos. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Falha de qualquer foto obrigatória impede avanço; arquivos já preparados não ficam públicos por acidente.
- Cliente vê apenas o conteúdo explicitamente liberado na etapa correta.
- Confirmação exibe sucesso só após persistência; etapa atual/próxima são inequívocas.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [ ] Unidade: Quantidade mínima, visibilidade, botão salvar, próxima etapa e lote válido.
- [ ] Integração: Commit/rollback, foto de outra OS, arquivo incompleto, chave repetida, autorização de adicional.
- [ ] E2E/validação operacional: Capturar 2 fotos → prévia → confirmar → avançar → conferir fotos na etapa concluída pelo cliente.
- [ ] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

ordem/ServicePhoto*; ServiceOrderService.java; equipe/ConclusaoEtapaService (novo); Angular equipe/ordem.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.

---

# Tarefa 7.0 (E07): Tratar rede instável, reenvio e conflitos de atualização

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Impedir perda silenciosa, duplicação e avanço incorreto em celular com conexão instável.

Status: pendente. Prioridade: P0. ID estável: E07 (não altera IDs 01–20 do MVP).
Dependências: E06.
Requisitos: RF07.
Trello: [Abrir card](https://trello.com/c/xqsH7wey).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF07 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [ ] 7.1 Manter rascunho em memória e explicar que reload/fechar pode exigir nova seleção; não persistir fotos/tokens sensíveis no localStorage.
- [ ] 7.2 Implementar progresso por arquivo, tentativas limitadas e reenvio com identificação estável; liberar object URLs ao descartar.
- [ ] 7.3 Para timeout após commit, consultar/repetir operação com mesma chave/corpo e exibir resultado confirmado.
- [ ] 7.4 Tratar 409 recarregando estado atual sem aplicar automaticamente etapa obsoleta; exigir revisão do usuário.
- [ ] 7.5 Cobrir expiração/bloqueio/reatribuição durante upload; não reutilizar autorização antiga.
- [ ] 7.6 Implementar coleta de uploads órfãos com janela documentada e métricas, sem remover fotos referenciadas.
- [ ] 7.7 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e07.md.

## Detalhes de Implementação

Consultar techspec.md: Interfaces: idempotência e locks; Testes; Riscos. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Duplo clique ou resposta perdida produz uma conclusão/evento/publicação, não duas.
- Dois técnicos não sobrescrevem conclusão silenciosamente; conflito exige confirmação revisada.
- Interface explica falha e mantém o que for recuperável, sem prometer offline completo.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [ ] Unidade: Máquina de estados, retry/backoff, chave estável e tratamento de 401/409.
- [ ] Integração: Falha antes/depois do commit, finalizações concorrentes, bloqueio/encerramento durante upload, limpeza de órfãos.
- [ ] E2E/validação operacional: Rede lenta/interrompida, resposta perdida, tentativa posterior e duas sessões na mesma OS.
- [ ] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

Angular equipe/upload; API conclusão/fotos; idempotência; scheduler de limpeza; testes de concorrência.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.

---

# Tarefa 8.0 (E08): Atualizar portal com progresso real e comunicação clara

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Mostrar ao cliente o fluxo específico da OS e sua evolução, sem progresso fictício.

Status: pendente. Prioridade: P1. ID estável: E08 (não altera IDs 01–20 do MVP).
Dependências: E05, E07.
Requisitos: RF08.
Trello: [Abrir card](https://trello.com/c/r218P6eN).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF08 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [ ] 8.1 Adaptar DTO público e linha horizontal existente ao snapshot e passagens reais.
- [ ] 8.2 Mostrar concluídas, atual, previstas e puladas; contar restantes até pronto, separando retirada/entrega.
- [ ] 8.3 Preservar retrabalho no histórico sem duplicar etapas no resumo; informar quando o legado não possui evidência histórica completa.
- [ ] 8.4 Agrupar fotos por passagem/etapa e manter galeria cronológica, legendas, zoom e carregamento paginado.
- [ ] 8.5 Exibir previsão como estimativa, motivo público de atraso e próxima ação; não converter contagem em dias ou percentual de trabalho.
- [ ] 8.6 Manter animação com movimento reduzido e sem barra vertical interna; respeitar revogação e conteúdo privado.
- [ ] 8.7 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e08.md.

## Detalhes de Implementação

Consultar techspec.md: Portal/projeções; Modelos de passagem; Decisões principais. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Não marcar etapas puladas como concluídas nem inferir datas inexistentes.
- Cliente vê fotos antigas liberadas e novas da etapa correta; dados internos continuam ausentes.
- Sem OS ativa, link expirado e falta de previsão têm mensagens úteis.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [ ] Unidade: Contagem, retrabalho, legado, ordenação/filtros e acessibilidade da animação.
- [ ] Integração: Projeção pública sem observações privadas, funcionário/cliente/arquivo de outra oficina e revogação.
- [ ] E2E/validação operacional: Evolução, espera, salto e retorno refletidos no portal em 320px sem overflow indevido.
- [ ] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

oficinas-app/src/app/portal/portal-access.component.ts; API portal/ordem; fotos/eventos.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.

---

# Tarefa 9.0 (E09): Consolidar orçamento autorizado e resumo de entrega

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Apresentar valor inicial e adicionais aceitos com histórico confiável, sem simular recebimento.

Status: pendente. Prioridade: P1. ID estável: E09 (não altera IDs 01–20 do MVP).
Dependências: E01, E03.
Requisitos: RF09.
Trello: [Abrir card](https://trello.com/c/EkHuZQ6w).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF09 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [ ] 9.1 Inventariar adicionais/aprovação/resumo existentes e reaproveitar versionamento/decimais e concessões restritas.
- [ ] 9.2 Adicionar orçamento inicial com itens, quantidades, valores e versões congeladas após envio; confirmação autenticada sem poderes ao técnico.
- [ ] 9.3 Calcular total autorizado pela soma do inicial aceito e adicionais efetivamente aprovados; evitar repetir versões substituídas/grupos.
- [ ] 9.4 Exibir pendente, recusado, parcialmente aprovado e autorizado na empresa e no cliente.
- [ ] 9.5 Ampliar resumo de entrega com trabalhos, orientações e fotos selecionadas, usando acesso específico revogável e validade explícita.
- [ ] 9.6 Preservar política de encerramento/retorno e manter avaliação oculta; não criar contas a receber ou emissão fiscal.
- [ ] 9.7 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e09.md.

## Detalhes de Implementação

Consultar techspec.md: Modelos orçamento; Valores e autorização; Integrações. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Total é rastreável a versões/decisões; arredondamento consistente; nenhum aprovado contado duas vezes.
- Técnico não consulta valores nem aprova em nome do cliente.
- Resumo não reativa acesso operacional encerrado nem mostra informações internas.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [ ] Unidade: Decimal/arredondamento, aprovação parcial, substituição e total consolidado.
- [ ] Integração: Versão concorrente, dupla decisão, encerramento simultâneo e permissão do resumo/fotos.
- [ ] E2E/validação operacional: Orçamento inicial → aceite → adicional parcial → total → entrega/resumo restrito.
- [ ] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

adicional; portal/AdditionalDecision*; ordem/encerramento; avaliacao/resumo; Angular ordem/portal.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.

---

# Tarefa 10.0 (E10): Criar indicadores históricos e painel de gargalos

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Transformar registros operacionais em decisões com métricas definidas e dados reais.

Status: pendente. Prioridade: P1. ID estável: E10 (não altera IDs 01–20 do MVP).
Dependências: E05, E08, E09.
Requisitos: RF10.
Trello: [Abrir card](https://trello.com/c/pjFEIPpo).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF10 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [ ] 10.1 Documentar definição e período de cada métrica antes da consulta: entradas, entregas, tempo por passagem, espera de aprovação e pontualidade.
- [ ] 10.2 Separar OS sem atualização pública de última alteração interna; permitir limiar configurável.
- [ ] 10.3 Definir pontualidade contra previsão vigente e exibir quantidade de revisões; evitar indicador inflado por reprogramação silenciosa.
- [ ] 10.4 Implementar agregações por oficina/período e informar cobertura histórica, denominadores e estado sem dados.
- [ ] 10.5 Criar gráficos acessíveis com tabela alternativa e links para listas filtradas de OS.
- [ ] 10.6 Validar índices e plano de consulta com massa sintética; distinguir valor autorizado de receita recebida.
- [ ] 10.7 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e10.md.

## Detalhes de Implementação

Consultar techspec.md: Monitoramento; Modelos; Endpoints painel. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Totais reconciliam com massa conhecida e listagem detalhada.
- Retornos/esperas não distorcem tempo médio; registros incompletos são identificados.
- Nenhum gráfico usa dados fictícios em produção ou mistura oficinas.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [ ] Unidade: Fórmulas, limites do período, offset, denominadores zero e incompletude.
- [ ] Integração: Agregados SQL vs eventos conhecidos, índices/paginação e isolamento.
- [ ] E2E/validação operacional: Selecionar período → conferir gráfico/tabela → abrir lista correspondente no desktop/mobile.
- [ ] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

API painel; ordem/eventos/previsão; Angular painel/dashboard*; testes de métricas.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.

---

# Tarefa 11.0 (E11): Confiabilizar notificações e visibilidade das falhas

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Tornar problemas de entrega visíveis sem desfazer trabalho salvo nem disparar mensagens em duplicidade.

Status: pendente. Prioridade: P0. ID estável: E11 (não altera IDs 01–20 do MVP).
Dependências: E02, E06, E09.
Requisitos: RF11.
Trello: [Abrir card](https://trello.com/c/87e9sqwj).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF11 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [ ] 11.1 Inventariar outbox/retries existentes e separar e-mails comerciais, convites e códigos de autenticação.
- [ ] 11.2 Exibir estado aguardando/enviado ao provedor/falha/cancelado e última tentativa com informação segura para dono/atendimento.
- [ ] 11.3 Integrar eventos novos de equipe/orçamento/conclusão sem enviar e-mail para cada foto isolada.
- [ ] 11.4 Reenvio com cooldown, deduplicação e auditoria; tratar estado incerto após SMTP aceitar sem prometer exactly-once.
- [ ] 11.5 Manter nome da oficina e URL contextual nos modelos; validar destinatário e escopo, sem tokens em logs.
- [ ] 11.6 Testar indisponibilidade do provedor, alertar fila antiga e preservar envio manual WhatsApp sem fingir confirmação de entrega.
- [ ] 11.7 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e11.md.

## Detalhes de Implementação

Consultar techspec.md: Comunicação; Integrações; Observabilidade. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Falha SMTP não reverte conclusão; UI permite diagnóstico e nova tentativa autorizada.
- Sucesso SMTP não é exibido como leitura do cliente.
- Códigos expirados não são enviados tardiamente por reutilização indiscriminada de fila.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [ ] Unidade: Templates, destinatário, deduplicação, expiração e política de retry.
- [ ] Integração: Mailpit/provedor fake, falha/transiente, concorrência de workers e reenvio.
- [ ] E2E/validação operacional: Atualizar OS com provedor indisponível → ver falha → recuperar → reenviar sem repetir evento de negócio.
- [ ] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

notificacoes/NotificationWorker.java; NotificationService.java; TransactionalEmail.java; portal códigos; Angular notificacoes.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.

---

# Tarefa 12.0 (E12): Preparar armazenamento privado, cotas e recuperação

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Eliminar dependência exclusiva do disco de uma instância e comprovar restauração de dados e fotos.

Status: pendente. Prioridade: P0. ID estável: E12 (não altera IDs 01–20 do MVP).
Dependências: E01, E07.
Requisitos: RF12.
Trello: [Abrir card](https://trello.com/c/JNeNEuLq).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF12 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [ ] 12.1 Inventariar armazenamento atual e criar adaptador local/objeto privado preservando contrato de autorização.
- [ ] 12.2 Preparar ambiente de teste S3 compatível; fornecedor de produção e custos ficam como decisão antes da ativação.
- [ ] 12.3 Migrar por manifesto com IDs/checksums/contagens, leitura compatível e rollback; não apagar origem antes de verificação e janela autorizada.
- [ ] 12.4 Criar miniaturas e métricas de bytes/cota por oficina; reservar cota atomicamente em uploads concorrentes e liberar órfãos.
- [ ] 12.5 Configurar backup de banco e objetos, retenção explícita e procedimento de restauração em ambiente isolado.
- [ ] 12.6 Medir RPO/RTO propostos 24h/4h para piloto; testar restauração com vínculos, decisões e imagens; registrar resultados reais.
- [ ] 12.7 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e12.md.

## Detalhes de Implementação

Consultar techspec.md: Storage; Integrações; Riscos; Observabilidade. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Arquivo não fica público nem revela chave do objeto; revogação bloqueia novas leituras.
- Migração reconciliada sem perda; quota não remove material existente ou contorna limite por concorrência.
- Restauração demonstrada com evidência, não apenas backup criado.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [ ] Unidade: Adaptador, chave segura, tamanho/cota, reserva/liberação e validação de manifesto.
- [ ] Integração: Storage de teste, falha parcial de migração, isolamento, concorrência e restauração.
- [ ] E2E/validação operacional: Upload/visualização privada/pública autorizada após migração e recuperação.
- [ ] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

ordem/Photo* e ServicePhoto*; docker; configurações por ambiente; novos runbooks e testes storage.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.

---

# Tarefa 13.0 (E13): Preparar produção, segurança e monitoramento

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Definir e validar uma operação recuperável, segura e observável antes de ampliar vendas.

Status: pendente. Prioridade: P0. ID estável: E13 (não altera IDs 01–20 do MVP).
Dependências: E03, E07, E11, E12.
Requisitos: RF13.
Trello: [Abrir card](https://trello.com/c/4DclLIDs).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF13 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [ ] 13.1 Criar configuração de produção separada do Compose local: HTTPS, cookies seguros, proxy confiável, portas mínimas, sem senhas default/Mailpit exposto.
- [ ] 13.2 Inventariar segredos e documentar rotação; não copiar credenciais em documentos, logs ou cards.
- [ ] 13.3 Executar matriz de autorização em endpoints/arquivos/exportações, dependências vulneráveis, CSRF, limites e sessão revogada.
- [ ] 13.4 Instrumentar latência, erros, fila, uploads, disco/cota e backups; alertas com responsável e runbook.
- [ ] 13.5 Testar carga de referência proposta 50 oficinas/100 mil OS/25 usuários; registrar hardware, resultados e gargalos, meta inicial p95 busca <1s.
- [ ] 13.6 Documentar retenção, acesso de suporte, exportação, incidentes, restauração e checklist de liberação; não contratar infraestrutura nesta task sem autorização.
- [ ] 13.7 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e13.md.

## Detalhes de Implementação

Consultar techspec.md: Monitoramento; Testes; Dependências técnicas. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Sem falhas críticas de isolamento ou perda de dados; pendências de produção explicitadas.
- Alertas são exercitados; logs não contêm CPF/token/senha ou payload privado.
- Capacidade e disponibilidade não são prometidas além do que foi medido.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [ ] Unidade: Redação de logs, validação de configuração segura e limites.
- [ ] Integração: Matriz de duas oficinas e sessões, headers/proxy, alertas e migração em staging.
- [ ] E2E/validação operacional: Smoke HTTPS e fluxo principal em staging; teste de carga e recuperação documentados.
- [ ] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

identidade/SecurityConfig.java; application*.yaml; Docker/proxy; testes segurança; runbooks produção.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.

---

# Tarefa 14.0 (E14): Entregar onboarding, importação e operação de assinaturas

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Permitir ativação assistida e administração comercial sem depender de alterações manuais no banco.

Status: pendente. Prioridade: P1. ID estável: E14 (não altera IDs 01–20 do MVP).
Dependências: E03, E12, E13.
Requisitos: RF14.
Trello: [Abrir card](https://trello.com/c/0rBkMTQH).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF14 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [ ] 14.1 E14.1a Entregar onboarding: identidade da oficina, equipe, fluxo, primeira OS e compartilhamento; checklist persistente e retomável.
- [ ] 14.2 E14.1b Entregar importação CSV com template, prévia, normalização, duplicidade, erros por linha, limite e rollback/reprocessamento idempotente.
- [ ] 14.3 E14.1c Entregar planos/cotas e estados de teste/ativo/inadimplente/cancelado com regras explícitas; downgrade não apaga dados.
- [ ] 14.4 E14.1d Integrar adaptador de cobrança em sandbox: webhooks autenticados, idempotência, eventos fora de ordem, conciliação e portal de cancelamento.
- [ ] 14.5 E14.1e Criar console SaaS segregado com mínimo privilégio, MFA para operador de produção, auditoria e sem impersonação invisível.
- [ ] 14.6 E14.1f Exportar dados/fotos autorizados e definir retenção pós-cancelamento antes de cobrar; fornecedor/preços/contratos finais requerem decisão explícita de ativação.
- [ ] 14.7 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e14.md.

## Detalhes de Implementação

Consultar techspec.md: Componentes SaaS; Modelos; Endpoints; Integrações. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Cada subentrega tem demonstração e testes próprios; nenhuma cobrança real ocorre em teste.
- Importação não mistura oficinas nem duplica lote ao repetir; registros rejeitados são explicados.
- Assinatura/cota vale no backend; inadimplência não apaga histórico automaticamente; admin não ganha acesso irrestrito.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [ ] Unidade: Validação de CSV/duplicidade, cotas, máquina de estados e idempotência de webhook.
- [ ] Integração: Sandbox/fake de cobrança, replay/ordem de eventos, jobs de importação e exportação isolada.
- [ ] E2E/validação operacional: Nova oficina → onboarding/importação → primeira OS → teste/plano → cancelamento/exportação.
- [ ] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

novos comercial/importacao/admin; Angular onboarding/assinatura; cadastro; políticas de cota; documentação comercial.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.

---

# Tarefa 15.0 (E15): Executar piloto comercial e validar lançamento

<critical>Ler prd.md e techspec.md desta pasta antes de implementar. Não considerar tarefa concluída sem testes e evidências.</critical>

## Visão Geral

Validar uso, valor percebido, custo e disposição de pagamento antes de expandir.

Status: pendente. Prioridade: P1. ID estável: E15 (não altera IDs 01–20 do MVP).
Dependências: E08, E09, E10, E11, E12, E13, E14.
Requisitos: RF15.
Trello: [Abrir card](https://trello.com/c/Tv8O1INQ).

## Conformidade com Skills Padrões

Consultar as skills aplicáveis em .agents/skills antes da execução: executar-task, java-springboot no backend, frontend-design na interface e task-review na validação de conclusão. Usar executar-qa quando solicitado. Não presumir que .claude/skills existe.

## Requisitos obrigatórios

- Atender RF15 do PRD; preservar funcionalidades e histórico existentes.
- Autorizar no servidor, derivando oficina da sessão; distinguir conteúdo privado de público.
- Não enviar comunicações reais, contratar serviços ou cobrar clientes sem autorização operacional.
- Não transformar hipóteses de preço, desempenho ou adoção em resultado comprovado.

## Subtarefas

- [ ] 15.1 Preparar demonstração com dados sintéticos, roteiro de instalação/onboarding e FAQ de suporte; posicionar acompanhamento sem substituir ERP completo.
- [ ] 15.2 Definir cinco oficinas candidatas e obter adesão/autorização separada antes de contato, convite, importação ou cobrança real.
- [ ] 15.3 Instrumentar métricas mínimas agregadas: primeira OS compartilhada, atualização semanal, uso do portal, tempo de aprovação, suporte e custo por oficina.
- [ ] 15.4 Conduzir quatro semanas de piloto autorizado; medir atualização simples com meta de 30s excluindo upload e dificuldades no celular.
- [ ] 15.5 Testar disposição a pagar R$99/R$149 como hipóteses, sem anunciar preço definitivo; contabilizar armazenamento, e-mail, suporte e aquisição.
- [ ] 15.6 Documentar decisão seguir/ajustar/interromper com dados: meta proposta 4/5 ativadas e 3/5 ativas ao final; falhas de segurança ou recuperação impedem lançamento.
- [ ] 15.7 Implementar e executar os testes abaixo; registrar resultados e limitações em validacao-e15.md.

## Detalhes de Implementação

Consultar techspec.md: Observabilidade; Sequenciamento; Dependências. As decisões de interfaces/modelos estão na especificação; não criar contratos paralelos no card.
Antes de editar, inventariar mudanças locais, testes existentes e reutilização possível. Novas migrações devem ser incrementais. Bloqueios externos devem ser registrados sem simular sucesso.

## Critérios de Sucesso

- Relatório diferencia entrevistas, resultados medidos e hipóteses; cadastro/receita fictícia não conta como venda.
- Plano de suporte e custo por oficina documentados; feedback vira backlog priorizado sem customização ilimitada.
- Fora de escopo (fiscal/estoque/app nativo/WhatsApp automático) não é prometido na venda.
- Evidências de testes, revisão e limitações anexadas; só então marcar documento e Trello como concluídos.

## Testes da Tarefa

- [ ] Unidade: Eventos/métricas de ativação, denominadores e cálculos de custo sem PII.
- [ ] Integração: Coleta deduplicada de métricas reconciliada com OS, sem mistura de oficinas.
- [ ] E2E/validação operacional: Ensaio de onboarding/demonstração; piloto real somente após autorização e gates de segurança.
- [ ] Regressão: rodar suites dos módulos afetados e build; usar massa sintética, sem dados reais.

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

novos docs/piloto-comercial.md e relatório; telemetria agregada; onboarding; materiais comerciais.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.
