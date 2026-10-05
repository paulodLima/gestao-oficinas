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

Evidências: [validacao-e01.md](validacao-e01.md).

<critical>SEMPRE CRIAR E EXECUTAR OS TESTES ANTES DE CONSIDERAR A TAREFA FINALIZADA. Esta documentação não é evidência de implementação.</critical>

## Arquivos relevantes

docs/tasks.md; docs/techspec.md; oficinas-api/src/test; oficinas-app/e2e; módulos ordem/portal/adicional.

Caminhos resumidos de backend são relativos a oficinas-api/src/main/java/br/com/gestao/oficinas_api. Conferir existência antes de implementar; itens marcados novos são propostas.
