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

