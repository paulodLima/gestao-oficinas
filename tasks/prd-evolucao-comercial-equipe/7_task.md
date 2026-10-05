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

