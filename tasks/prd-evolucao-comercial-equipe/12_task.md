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

