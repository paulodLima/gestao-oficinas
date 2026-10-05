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

