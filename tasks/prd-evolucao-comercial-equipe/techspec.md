# Especificação Técnica — Evolução comercial e equipe

## Resumo Executivo

Versão 1.0 · 29/09/2026 · Fonte: [prd.md](prd.md).
Manter monólito modular Spring/Java, Angular e PostgreSQL. Reutilizar sessão JDBC, versionamento, eventos, validação de fotos e outbox; não criar outro backend para a equipe. Os contratos abaixo são propostas para implementação, não endpoints já entregues. Decisão confirmada: alcance configurável, padrão somente OS atribuídas.

## Arquitetura do Sistema

### Visão Geral dos Componentes

- Identidade/equipe: novo vínculo de funcionário, convite, papel e alcance; adaptar autenticação/revalidação hoje centrada no proprietário.
- Política de acesso: oficina + usuário ativo + função + atribuição; reaplicada em leitura, escrita, download e replay.
- Ordens/fluxo: modelos versionados e cópia por OS; reutilizar eventos, versões e travas existentes.
- Fotos/conclusão: upload privado em preparação; finalização transacional associa fotos à passagem terminada, publica somente quando autorizado e avança a OS.
- Angular equipe: rota lazy /equipe, detalhe enxuto, rascunho em memória, câmera/galeria e erros recuperáveis.
- Portal/painel: projeções próprias; nunca reutilizar DTO administrativo no portal.
- Comunicação: ampliar observabilidade da outbox existente, não substituir por disparo direto.
- Storage: adaptador privado local/objeto, migração por manifesto e reconciliação.
- SaaS: onboarding, importação, planos, assinatura e console administrativo segregado.

## Design de Implementação

### Interfaces Principais

Serviços conceituais: EquipeService convidar/bloquear; AcessoOsPolicy exigirLeitura/exigirAtualizacao/exigirPublicacao; FluxoService instanciar/revisar; ConclusaoEtapaService concluir; PhotoStorage gravar/abrir/remover; AssinaturaService reconciliarEvento.
Cada serviço recebe identidade autenticada, não oficina confiada ao payload. Não ampliar todas as rotas de proprietário para técnico.

Conclusão: validar sessão/alcance → carregar OS sob trava → conferir expectedVersion/fluxo/estado/aprovações → validar fotos prontas do mesmo lote/oficina/OS/passagem → gravar conclusão, visibilidade, avanço, auditoria, outbox e resposta idempotente na mesma transação. Bytes ficam privados antes do commit. Falha limpa órfãos posteriormente, não muda etapa.
Revalidar autorização antes do replay. Mesmo Idempotency-Key e corpo retornam resultado anterior; corpo diferente retorna 409. A UI mantém chave na tentativa incerta; após conflito e revisão gera outra operação. Fechamento e conclusão usam ordem de locks consistente: identidade/vínculo quando necessário → OS → registros dependentes. Testar deadlocks e revogação concorrente.

### Modelos de Dados

- equipe_usuario: oficina, identificador normalizado único por oficina, nome, celular, papel, alcance ATRIBUIDAS/TODAS, podePublicar, ativo, hashSenha, authVersion, versão.
- equipe_convite: hash do token, validade, uso/revogação, vínculo; nunca guardar token em logs. Proposta inicial: 48h, uso único.
- os_responsavel: oficina/OS/funcionário, período e autor; primeiro ciclo com um responsável técnico ativo por OS.
- fluxo_modelo/versao/etapa: ordenação, tipo EXECUCAO/ESPERA/PRONTO, política de evidência; plano da OS preserva snapshot.
- os_etapa_passagem: etapa, entrada/saída, estado PREVISTA/ATUAL/CONCLUIDA/PULADA, motivo e ator. Retorno cria nova passagem; não sobrescrever anterior.
- conclusao_etapa: idempotência, versão esperada, passagem, próximo passo, comentário público/interno e fotos associadas.
- orçamento inicial: versões imutáveis após envio/aceite; usar os padrões de decimal e aprovação do módulo adicional sem dar poderes de aprovação ao funcionário.
- consumo/plano/assinatura/importacao: escopo por oficina; identificadores de evento externo únicos, estados e auditoria.

FKs compostas com oficina_id, migrações Flyway incrementais, índices de escopo/status/responsável/data. Não renumerar migrações existentes. Datas com offset, apresentação America/Sao_Paulo; dinheiro decimal, nunca float. Manter legado por migração explícita, sem inferir que etapas anteriores ocorreram.

### Endpoints de API

Contratos propostos; nomes finais devem ser confrontados com rotas existentes:
- /api/equipe/usuarios GET/POST, /{id} PATCH; convite/revogação/bloqueio exclusivos do proprietário.
- /api/equipe/auth/login, /convite/aceitar, /recuperacao, /logout: CSRF e limites conforme identidade.
- GET /api/equipe/ordens-servico?q=&page=&size=: escopo servidor, 20 por página, máximo 100.
- GET /api/equipe/ordens-servico/{id}: DTO mínimo, sem CPF ou financeiro.
- PUT /api/ordens-servico/{id}/responsavel: versão esperada, usuário da mesma oficina.
- /api/fluxos GET/POST, versões e aplicação explícita na OS.
- POST /api/equipe/ordens-servico/{id}/conclusoes: expectedVersion, passagemId, proximaEtapaId, fotoIds, comentário e visibilidade; Idempotency-Key obrigatório.
- Reutilizar upload/leitura atuais por adaptador com política; não copiar a regra em dois serviços.
- GET /api/painel/historico?inicio=&fim=: métricas com definição, cobertura e período.
- /api/importacoes, /api/assinatura e /api/admin/oficinas: contratos segregados; operador SaaS não herda sessão de cliente/oficina.

401 sessão inválida; 403 ação vedada em recurso acessível; 404 recurso fora de alcance; 409 versão/transição/cota concorrente; 422 conteúdo inválido. Erros com código estável e mensagem útil.

## Pontos de Integração

SMTP existente + Mailpit local; preservar outbox comercial e tratar códigos síncronos separadamente. Falha não deve aparentar entrega. WhatsApp continua manual.
Armazenamento S3 compatível privado, fornecedor pendente; manter download autorizado pela API para revogação imediata de novas leituras. Não tornar bucket público nem cache compartilhado de fotos privadas.
Cobrança por adaptador, sandbox antes de produção; verificar assinatura do webhook, replay, ordem de eventos, conciliação e estados. Fornecedor/preços/retenção comercial exigem decisão antes da ativação, não bloqueiam documentação e testes com adaptador fake.

## Abordagem de Testes

### Testes Unidade

Matriz de permissões, normalização, convite/expiração, próxima etapa, evidência obrigatória, valores e fórmulas do painel. Relógio injetado. Estados Angular, reenvio, foco e formulários.

### Testes de Integração

PostgreSQL real/Testcontainers: duas oficinas, dois técnicos, proprietário, atendimento e cliente. Testar bloqueio com sessão aberta, alteração de atribuição, IDs cruzados de fotos, conclusão/encerramento concorrentes, duas conclusões e idempotência após timeout. Não usar H2 para provar locks e constraints. Storage/SMTP/cobrança externos substituídos por ambientes de teste controlados.

### Testes de E2E

Playwright UI+API: convite → login → placa → fotos → concluir → cliente vê etapa e fotos corretas. Desktop/320/390px; teste manual em Safari/iOS e Chrome/Android para câmera/galeria, permissões e conexão. Screenshots com massa sintética. Falhas devem preservar dados possíveis e explicar quando selecionar arquivos novamente.

## Sequenciamento de Desenvolvimento

### Ordem de Construção

E01 baseline → E02 identidade → E03 política → E04 mobile; E05 fluxo depende de E01/E03; E06 conclusão integra E04/E05; E07 robustez → E08 portal. E09 valores depende de E01/E03; E10 métricas depende de E05/E08/E09; E11 comunicação depende de E02/E06/E09. E12 storage após E01/E07; E13 produção após E03/E07/E11/E12; E14 comercial após E03/E12/E13. E15 piloto após E08–E14 e baseline. Entrevistas podem preceder a liberação do produto.

### Dependências Técnicas

Contas, domínio e infraestrutura de produção não são criados nesta documentação. Produção depende de provedor autorizado, segredos, remetente verificado e rotina de recuperação aprovada. Sessão inicial da equipe usa limite de 12h já existente; não introduzir sessão indefinida. Recuperação manual emite convite de redefinição de uso único com auditoria, sem mostrar senha ao dono.

## Monitoramento e Observabilidade

Métricas: latência p95 de busca/conclusão sem transferência de bytes, conflitos, uploads/falhas, atraso da outbox, autenticações negadas, bytes por oficina e falhas de backup. IDs de correlação sem CPF, tokens ou conteúdo de fotos. Não usar placa/cliente como label de métrica.
Carga de referência proposta: 50 oficinas sintéticas, 100 mil OS no total, 25 usuários simultâneos; meta inicial p95 busca <1s em ambiente registrado. RPO proposto 24h/RTO 4h para piloto, só declarar atingidos após restauração ensaiada. Alertas acionáveis com responsável e procedimento; não pressupor Grafana existente.

## Considerações Técnicas

### Decisões Principais

Preservar monólito e banco atuais. Separar foto de passagem de etapa, pois retorno gera nova ocorrência. O status legado permanece compatível; adaptar projeções sem reescrever histórico.
Pendência de adicional bloqueia apenas execução vinculada ao item/grupo pendente; não bloquear todo o veículo automaticamente. Tornar vínculo entre trabalho e adicional explícito.
Etapas restantes são posições previstas até pronto, não tempo estimado. Receita não é total autorizado. Histórico incompleto deve informar cobertura, não inventar eventos.

### Riscos Conhecidos

Documentação antiga contém decisões já alteradas; código e evidências devem ser conferidos em E01. A autenticação atual revalida proprietário no filtro; acrescentar equipe exige adaptar principal, repositório e sessão, não só menu. Visibilidade sem confirmação pode expor dados. Storage e DB não compartilham transação: usar preparação privada e coleta de órfãos. Custos de mensagens/fotos e suporte invalidam preço se ignorados. Escopo E14 deve ser executado em subentregas sem ativar cobrança real.

### Conformidade com Skills Padrões

Não foi localizado AGENTS.md na busca do projeto. Catálogo atual em .agents/skills (não pressupor .claude/skills): java-springboot para backend, frontend-design para interface, executar-task e task-review na implementação, executar-qa quando solicitado. Esta entrega usa cria-prd, cria-techspec e criar-tasks; não declara revisão/testes funcionais concluídos.
Context7 indisponível: fallback em fontes oficiais, consultadas em 29/09/2026: [Spring Security](https://docs.spring.io/spring-security/reference/servlet/authorization/index.html), [Spring Session JDBC](https://docs.spring.io/spring-session/reference/configuration/jdbc.html), [PostgreSQL locks](https://www.postgresql.org/docs/17/explicit-locking.html), [OWASP autorização](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html), [OWASP upload](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html).

### Arquivos relevantes e dependentes

Existentes: oficinas-api/src/main/java/br/com/gestao/oficinas_api/{identidade,ordem,adicional,portal,notificacoes}; identidade/SecurityConfig.java e Identidade.java; src/main/resources/db/migration; oficinas-app/src/app/{auth,ordem,portal,painel,oficina,notificacoes}; app.routes.ts; docker/docker-compose.yml; docs/techspec.md.
Novos módulos previstos: equipe, fluxo e comercial; rotas Angular equipe; documentação e testes ao lado dos módulos existentes. Inventariar nomes concretos antes de cada implementação.

