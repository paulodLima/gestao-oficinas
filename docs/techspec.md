# Especificação técnica — Gestão de Oficinas

Versão 1.1 · Fonte funcional: [prompt.md](prompt.md) · Planejamento: [tasks.md](tasks.md).

## 1. Escopo e estado atual

Esta especificação define a implementação das tarefas 2–20. A tarefa 2 implementa identidade do proprietário; os demais módulos permanecem contratos a implementar.

Inventário atualizado: API Maven com Spring Boot 4.1.1, Java 21, Spring Security, Spring Session JDBC, Flyway, Spring Mail e PostgreSQL. Identidade usa JdbcTemplate com SQL parametrizado e transações explícitas para consumir tokens e invalidar sessões atomicamente; JPA continua disponível para próximos módulos. Angular 19/TypeScript 5.6/Express 4 com cadastro, login, recuperação, redefinição e página autenticada mínima. Docker usa PostgreSQL 17, Java 21 e Node 22; não exige Java/Node no host. Fotos e módulos de negócio ainda não foram implementados.

Compose possui postgres, api, app e Mailpit. PostgreSQL tem volume e healthcheck; API aguarda banco saudável; app aguarda início da API, não prontidão. API_URL agora define o destino privado do proxy Express; /api mantém mesma origem no navegador. Evidências: [task-1-validacao.md](task-1-validacao.md) e [task-2-validacao.md](task-2-validacao.md).

### Decisões implementadas na tarefa 2

- Flyway V1 cria oficina/proprietário, hashes de recuperação, limites de tentativas, auditoria de cadastro/troca de senha e tabelas Spring Session. Não há conta padrão.
- Rotas atuais usam renderização cliente, inclusive acesso/recuperação; não há SSR/prerender de identidade. SSR público fica reservado a futuras páginas que o necessitem.
- Serviço TransactionalEmail é reutilizável para os códigos da tarefa 10. SMTP é síncrono nesta base; falha provoca rollback do token e aviso operacional sem PII. A recuperação sempre responde 202 para conta conhecida/desconhecida, inclusive falha SMTP, para não revelar cadastro pelo código HTTP. Monitorar `PASSWORD_RECOVERY_EMAIL_UNAVAILABLE`; envio confiável com outbox/retries será ampliado na tarefa 15. Diferença temporal residual deve ser tratada nessa evolução.
- Rate limit fixo de 15 minutos no PostgreSQL: cadastro 20/IP; login 100/IP e 15/e-mail; recuperação 30/IP e 5/e-mail; redefinição 30/IP. IP encaminhado só é aceito quando a conexão vem do host `APP_AUTH_TRUSTED_PROXY_HOST`; Express sobrescreve o header com IP do socket. Proxies adicionais exigem configuração específica.
- GET /api/auth/csrf fornece token/headerName antes de qualquer escrita. GET /api/oficina retorna somente id/nome da oficina autenticada, sem edição (tarefa 3).
- Produção exige perfil prod, HTTPS e SMTP autenticado/TLS; Compose é apenas desenvolvimento. Dependências Angular 19 têm alertas pendentes de atualização antes de publicação.

## 2. Arquitetura e decisões

### Implementação da tarefa 3 — perfil da oficina

`GET /api/oficina` retorna id, slug imutável, nome, telefone, emailContato, endereco,
horario (texto livre de até 1000 caracteres), fuso IANA, perfilPublico, temLogo e versao.
`PATCH /api/oficina` aceita somente esses campos editáveis e exige versao; campos omitidos
são preservados, texto vazio limpa contatos/endereço/horário, null e campos desconhecidos
são rejeitados. Nome é obrigatório, até 120 caracteres. Todas as escritas derivam a oficina
da sessão, com CSRF, atualização condicional de versão (409 em conflito) e auditoria na
mesma transação. E-mail de contato não altera credenciais do proprietário.

`PUT /api/oficina/logo?versao=N` recebe multipart `arquivo`; `DELETE` no mesmo caminho
remove a logo. `GET /api/oficina/logo` serve a imagem autenticada. Limites específicos da
logo: PNG/JPEG, 2 MiB, 4 megapixels, conteúdo decodificado e MIME conferidos. Normalização
para PNG de até 512 px remove metadados; SVG/WebP/HEIC não são aceitos para logo. O pequeno
arquivo normalizado fica em bytea no PostgreSQL, com backup e transação junto ao cadastro.
Isso não substitui o adaptador de fotos de OS previsto na tarefa 8.

O proprietário publica explicitamente o perfil (`perfilPublico=false` por padrão).
`GET /api/publico/oficinas/{slug}` e `.../{slug}/logo` retornam somente dados comerciais
liberados, ou 404 se não publicados. Nunca incluem proprietário, e-mail de login, IDs
internos ou versão. Respostas usam no-store; remoção/despublicação bloqueia novas leituras.
O perfil `/oficina/:slug` e o componente compartilhável de identidade entregam a
representação pública desta tarefa. Acompanhamento de OS e portal autenticado continuam
nas tarefas 10–11; não são simulados aqui.

Frontend: `/configuracoes/oficina`, formulário reativo, prévia, upload independente,
alternativa por inicial sem logo, estados de falha e recarga explícita em conflito.
Referências: [multipart Spring](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/multipart-forms.html)
e [formulários reativos Angular](https://angular.dev/guide/forms/reactive-forms).

### Implementação da tarefa 4 — clientes e veículos

A migração V3 cria `cliente`, `veiculo`, `vinculo_cliente_veiculo`,
`verificacao_email_cliente` e `cadastro_auditoria`. CPF e placa são únicos por oficina;
FKs compostas impedem vínculos entre oficinas e um índice parcial garante um único
responsável atual por veículo. A troca encerra o vínculo atual e cria outro na mesma
transação, com controle otimista pela versão do veículo. O histórico não é exposto nas
respostas do novo responsável. A tarefa 5 adicionará a referência imutável do cliente da
OS e o bloqueio de transferência enquanto existir OS ativa.

As rotas privadas `/api/clientes` e `/api/veiculos` aplicam paginação, busca de até 100
caracteres, escopo derivado da sessão, CSRF, validação de payload e auditoria. A busca aceita
CPF e placa com ou sem máscara. Placas antigas e Mercosul são armazenadas sem pontuação e em
maiúsculas. Escritas concorrentes retornam 409 e duplicidades são isoladas por oficina.

A verificação de e-mail emite código aleatório de seis dígitos, guarda somente HMAC SHA-256
com `CODE_SECRET`, expira em 10 minutos, aceita cinco tentativas, exige 60 segundos entre
envios e limita emissões por cliente e IP em janelas de 15 minutos. O uso é único; alterar o
e-mail remove a verificação e revoga desafios pendentes. O Angular entrega `/clientes-veiculos`
com formulários reativos, busca, edição, transferência e layout validado a 320 px.

Monólito modular Spring Boot, pacotes por domínio em br.com.gestao.oficinas_api: identidade, oficina, clientes, veiculos, ordens, vistoria, arquivos, adicionais, portal, notificacoes e auditoria. Controllers recebem DTOs validados; serviços transacionais aplicam regras; repositórios sempre recebem oficinaId autorizado. Entidades JPA não são respostas públicas. Injeção por construtor e propriedades tipadas.

Angular organizado por funcionalidades com rotas lazy, serviços HTTP e formulários reativos. Autorização é aplicada no servidor, não por guards apenas. Portal e administração não compartilham DTOs privados.

### Decisões para as próximas tarefas

| Tema | Decisão | Implementação |
| --- | --- | --- |
| Sessões | Spring Security + Spring Session JDBC no PostgreSQL; cookie HttpOnly; revogação no servidor | 2 e 10 |
| Banco | Flyway com módulo PostgreSQL, migrações versionadas em src/main/resources/db/migration | 2 em diante |
| Hibernate | ddl-auto=validate após primeira migração; não usar update em produção | 2 |
| E-mail | SMTP via Spring Mail; Resend como provedor de produção; Mailpit local para desenvolvimento | base na 2, eventos na 15 |
| Fotos | Adaptador de armazenamento: diretório privado persistente local; Amazon S3 privado em produção | 8 |
| Download | API transmite arquivo após autorização; não devolver URL S3 ao cliente | 8 e 11 |
| Testes | JUnit/Mockito, integração com PostgreSQL via Testcontainers, frontend unitário e E2E com Playwright | por módulo |
| Comunicação | Mesma origem pública; /api encaminhado ao backend; chamadas privadas feitas no navegador | 2 |

Não criar contas pagas, contratar serviços ou provisionar nuvem nesta tarefa. Credenciais e domínio verificado são pré-requisitos operacionais do envio em produção; falta de configuração gera falha explícita. Localmente, Mailpit captura e-mails sem enviá-los a destinatários reais.

Na tarefa 2 configurar proxy /api no desenvolvimento Angular e na entrada HTTP usada pelo Docker/produção, preservando cookies, cabeçalhos CSRF e respostas Set-Cookie. Destino interno fixo em configuração, nunca fornecido pelo usuário. Não usar http://api:8080 no navegador. API_URL serve somente no servidor. SSR permanece para páginas públicas; rotas privadas devem usar renderização no cliente, sem prerender, cache compartilhado ou transferência de respostas privadas para HTML.

## 3. Modelo de dados e invariantes

UUID para IDs. Entidades de negócio incluem oficina_id, created_at/updated_at em timestamptz e autor quando aplicável. CPF normalizado como texto e nunca retornado em DTO público. E-mail normalizado para identidade. Placas normalizadas para letras/números maiúsculos. OS mantém referência ao cliente responsável no atendimento.

| Entidade | Campos/relações essenciais |
| --- | --- |
| Oficina | nome, slug único, contatos, endereço, horário, logo, fuso IANA |
| Proprietario | oficina, nome, e-mail único de login, hash de senha, ativo |
| Cliente | oficina, nome, CPF, telefone, e-mail, verificadoEm, ativo |
| Veiculo | oficina, placa, marca/modelo/ano/cor; unique(oficina, placa) |
| VinculoClienteVeiculo | oficina, cliente, veículo, início/fim; só um responsável atual |
| OrdemServico | oficina, cliente responsável, veículo, numero, relato, kmEntrada, status, previsão, versão, encerradaEm |
| HistoricoStatus/Atualizacao | OS, status anterior/novo, motivo, texto público/interno separados, autor |
| Vistoria/VersaoVistoria | OS, checklist, km, combustível, objetos, avarias, rascunho/confirmada, versão anterior |
| Foto | oficina, OS, atualização/vistoria/etapa opcionais, chave privada, MIME, bytes, legenda, publicada, estado |
| Solicitacao/VersaoAdicional | OS, problema, justificativa, fotoIds, versão, prazo proposto, estados, enviadaEm |
| ItemAdicional | versão, descrição, tipo PECA/MAO_DE_OBRA, quantidade, valor unitário, grupo opcional, execucao NAO_INICIADA/EM_EXECUCAO/CONCLUIDA, iniciadaEm/concluidaEm |
| Decisao | versão e item/grupo, cliente autenticado, decisão, comentário, instante |
| HistoricoPrevisao | OS, valor anterior/novo inclusive null, motivo público, próxima ação, autor |
| CredencialPortal | hash/HMAC, finalidade, oficina/cliente/OS/versão, validade, tentativas, usadaEm/revogadaEm |
| Avaliacao | OS entregue, nota, comentário, consentimento/publicadaEm; unique(OS) |
| Notificacao/Outbox | evento, destinatário, canal, payload mínimo, chave única, tentativas, próximo envio, estado |
| Auditoria | oficina, recurso/ação, ator/tipo, instante, campos alterados permitidos, correlação |

FKs compostas com oficina_id evitam associações entre empresas. OS tem unique(oficina_id, numero) e índice único parcial em (oficina_id, veiculo_id) WHERE encerrada_em IS NULL. CHECK vincula encerradaEm aos estados ENTREGUE/CANCELADO. Abertura concorrente responde 409, sem gerar segunda OS.

OS/versões usam controle otimista por campo version; decisões e encerramento bloqueiam a mesma OS em transação, na ordem OS → solicitação → itens. Alterações exigem expectedVersion; estado desatualizado retorna 409. Auditoria e outbox gravadas na transação de negócio.

Valores unitários e totais numeric(15,2); quantidades numeric(12,3), positivas. Valores não negativos; total por item = quantidade × unitário, arredondado HALF_UP para duas casas; total = soma dos itens arredondados. JSON transmite dinheiro e quantidade como strings decimais. Nunca usar ponto flutuante para autorização de despesa. Instantes ISO 8601 UTC; apresentação no fuso da oficina, padrão America/Sao_Paulo. Previsão exige timestamp com offset.

Transferência de veículo revoga vínculo atual; não altera cliente das OS passadas. Impedir transferência enquanto houver OS ativa; encerrar/cancelar primeiro. Nunca conceder ao novo cliente fotos ou decisões anteriores.

## 4. Identidade, permissões e expiração

Proprietário: sessão com 30 minutos de inatividade e máximo absoluto de 12 horas. Cliente identificado por código: 30 minutos de inatividade e máximo absoluto de 24 horas. Cookie Secure em produção, HttpOnly, SameSite=Lax, Path=/; proteção CSRF nas operações de escrita, inclusive login. Rotação do ID após autenticação; logout invalida sessão. Sem tokens em localStorage. Senhas com BCrypt e política mínima de 12 caracteres, limite compatível com BCrypt validado em bytes.

Código aleatório de seis dígitos: validade 10 minutos, máximo cinco tentativas, intervalo de reenvio de 60 segundos e cinco emissões por 15 minutos por finalidade/contato; limite adicional por IP. Guardar HMAC com segredo de ambiente (código tem baixa entropia). Reenvio invalida anterior da mesma finalidade. Resposta pública genérica 202 mesmo para placa inexistente, sem contato mascarado revelado. Desafio para contato inexistente não emite e-mail e não é validável. Cliente novo verifica posse do e-mail com código, sem condição circular de exigir verificação anterior.

Recuperação de senha: token aleatório de 256 bits, hash armazenado, 30 minutos, uso único; invalida sessões após troca. Link de acompanhamento: token 256 bits, sete dias ou encerramento, o que ocorrer primeiro; reemissão revoga anterior e sessões derivadas. Link abre /acompanhar#token=..., troca por sessão via POST e remove fragmento da barra. Referrer-Policy: no-referrer; não registrar token no servidor nem em analytics.

Sessão de link tem escopo de uma OS, somente leitura. Sessão de cliente verificado tem identidade, mas consulta vínculo e OS ativa a cada requisição. Encerramento revoga grants da OS e sessões derivadas de link; sessão de identidade pode permanecer sem OS ativa e, numa nova visita, receber acesso somente após validar vínculo atual. Isso permite retorno sem conceder acesso futuro por link antigo.

Troca de e-mail do cliente limpa verificadoEm e revoga códigos, grants e sessões associados ao contato anterior, inclusive autorizações de aprovação pendentes. Verificar posse do novo endereço antes de emitir novas autorizações de identidade. Decisões já registradas preservam a identidade e a evidência do momento do aceite. A alteração é auditada.

Aprovação exige código de finalidade APROVAR_ADICIONAL ligado ao cliente, OS e versão. Consumir código junto à decisão em transação. Link sozinho não autoriza. Decisões posteriores exigem novo código; repetição idempotente da mesma requisição não consome outro.

Avaliação usa credencial independente de sete dias após entrega; autoriza apenas resumo público e criação de uma avaliação daquela OS. Não permite galeria operacional nem atendimento novo. Nota 1–5 inteira; consentimento separado e false por padrão. Link Google disponível para todos.

## 5. Convenções e contratos HTTP

Prefixo /api; JSON camelCase; multipart para fotos. IDs UUID. Escritas validam payload, oficina e estado. Criar retorna 201 + Location; consultas/edições 200; ações sem corpo 204; solicitação de código/recuperação 202 genérico.

Listagens: page=0, size=20 (máximo 100), sort=createdAt,desc, desempate id; somente campos permitidos. Resposta: {items:[],page:0,size:20,totalElements:0,totalPages:0}. Busca q limitada a 100 caracteres. Datas from/to em ISO; filtros desconhecidos ou inválidos retornam 400. Conteúdo privado Cache-Control:no-store.

Erro application/problem+json: {type:"about:blank",title:"Conflito",status:409,code:"OS_ATIVA_EXISTENTE",detail:"Já existe atendimento ativo para este veículo.",instance:"/api/ordens-servico",traceId:"...",errors:[]}. Não refletir segredos, SQL ou dados de terceiros. 400 inválido; 401 sem sessão; 403 perfil/CSRF; 404 recurso inexistente ou fora do escopo; 409 estado/versão/duplicidade; 413 tamanho; 415 MIME; 429 tentativas; 503 dependência indisponível. Credenciais públicas inválidas usam erro genérico sem revelar existência.

### Proprietário

| Método e caminho | Entrada/saída principal |
| --- | --- |
| GET /auth/csrf | token CSRF para sessão anônima/autenticada |
| POST /auth/cadastro | nome, email, senha, nomeOficina → proprietário/oficina sem senha |
| POST /auth/login | email, senha → identidade + cookie |
| GET /auth/me; POST /auth/logout | identidade; invalidação |
| POST /auth/recuperacao; POST /auth/redefinicao | email → 202; token,novaSenha → 204 |
| GET/PATCH /oficina | dados/horário/fuso; PATCH campos permitidos |
| PUT /oficina/logo | multipart imagem validada → referência da logo |
| POST/GET /clientes; GET/PATCH /clientes/{id} | nome,cpf,telefone,email; consulta paginada |
| POST /clientes/{id}/verificacao; POST /clientes/{id}/verificacao/confirmacao | emissão; desafioId,codigo |
| POST/GET /veiculos; GET/PATCH /veiculos/{id} | placa,marca,modelo,ano,cor,clienteId |
| POST /veiculos/{id}/transferencias | novoClienteId,expectedVersion; sem OS ativa |
| POST/GET /ordens-servico; GET /ordens-servico/{id} | criar: clienteId,veiculoId,relato,kmEntrada,previsao?; filtrar clienteId/placa/status |
| POST /ordens-servico/{id}/status | status,motivo?,textoPublico?,expectedVersion |
| POST/GET /ordens-servico/{id}/atualizacoes | textoPublico,textoInterno,publicada; lista ordenada |
| PUT/GET /ordens-servico/{id}/vistoria | checklist,observacoes,expectedVersion → rascunho |
| POST /ordens-servico/{id}/vistoria/confirmacoes | expectedVersion → versão confirmada |
| POST /ordens-servico/{id}/vistoria/correcoes | motivo,checklist,expectedVersion → nova versão |
| POST/GET /ordens-servico/{id}/fotos | multipart arquivo + metadata; lista |
| PATCH/DELETE /ordens-servico/{id}/fotos/{fotoId} | legenda/publicada/expectedVersion; remoção auditada |
| GET /ordens-servico/{id}/fotos/{fotoId}/conteudo | bytes autorizados; variante miniatura/original |
| POST /ordens-servico/{id}/previsao | previsao nullable,motivoPublico,proximaAcao,expectedVersion |
| POST/GET /ordens-servico/{id}/adicionais | problema,justificativa,fotoIds[],itens[],previsaoProposta → rascunho/lista |
| PATCH /ordens-servico/{id}/adicionais/{sid} | editar apenas rascunho, expectedVersion |
| POST /ordens-servico/{id}/adicionais/{sid}/envio | expectedVersion → versão congelada |
| POST /ordens-servico/{id}/adicionais/{sid}/substituicoes | nova composição e motivo → nova versão |
| POST /ordens-servico/{id}/adicionais/{sid}/cancelamento | motivo,expectedVersion |
| POST /ordens-servico/{id}/adicionais/{sid}/itens/{itemId}/execucao | versaoId,estado EM_EXECUCAO/CONCLUIDA,expectedVersion; somente proprietário, item aprovado e OS ativa |
| POST /ordens-servico/{id}/encerramento | tipo ENTREGUE/CANCELADO,motivo?,cancelarPendencias,expectedVersion |
| POST/DELETE /ordens-servico/{id}/acesso | emitir/revogar link de acompanhamento |
| GET /painel | contagens operacionais e atrasos, filtradas por oficina |
| GET /notificacoes; PATCH /notificacoes/{id} | avisos paginados; lida=true |
| GET /avaliacoes | avaliações privadas da oficina |

### Cliente

| Método e caminho | Entrada/saída principal |
| --- | --- |
| POST /portal/acesso/codigo | oficinaSlug,placa → 202, desafioId opaco |
| POST /portal/acesso/validacao | desafioId,codigo → sessão de identidade |
| POST /portal/acesso/link | token → sessão só leitura de uma OS |
| POST /portal/logout | invalida sessão |
| GET /portal/veiculos | veículos atualmente autorizados, sem CPF/contatos |
| GET /portal/servico-atual?veiculoId=... | {servico:null} ou resumo público da OS ativa |
| GET /portal/ordens-servico/{id}/atualizacoes | somente atualizações publicadas |
| GET /portal/ordens-servico/{id}/fotos | galeria publicada paginada |
| GET /portal/ordens-servico/{id}/fotos/{fotoId}/conteudo | bytes com autorização atual |
| GET /portal/ordens-servico/{id}/adicionais | versões enviadas e decisões autorizadas |
| POST /portal/ordens-servico/{id}/adicionais/{sid}/codigo | versaoId → 202 genérico |
| POST /portal/ordens-servico/{id}/adicionais/{sid}/decisoes | versaoId,desafioId,codigo,itens[{itemId,decisao}],comentario? |
| POST /portal/avaliacoes/acesso | token de avaliação → sessão restrita |
| GET /portal/avaliacoes/resumo | resumo da OS do grant de avaliação |
| POST /portal/avaliacoes | nota,comentario?,consentimentoPublicacao; OS vem do grant |

Serviço público: id,numero,veiculo{placa,marca,modelo},oficina{nome,logo,contato},status,ultimaAtualizacao,previsao,motivoAtraso,proximaAcao,versao. Nunca incluir CPF, contato do cliente, observações internas, chaves de objetos ou entidades completas.

Ações de abertura, upload e decisão aceitam Idempotency-Key UUID. Armazenar chave, ator, oficina, operação, hash do corpo e resposta por 24 horas. Mesma chave/corpo retorna resposta anterior; corpo divergente 409. Revalidar autorização antes do replay. Decisões guardam unicidade por item/versão e cliente permanentemente, além da janela de idempotência.

## 6. Estados, publicação e concorrência

Status internos: RECEBIDO, EM_DIAGNOSTICO, AGUARDANDO_APROVACAO, AGUARDANDO_PECAS, EM_MANUTENCAO, EM_MONTAGEM, EM_TESTES, PRONTO_PARA_RETIRADA, ENTREGUE, CANCELADO; FUNILARIA/PINTURA opcionais. Saltos permitidos entre não terminais; retornos na ordem do fluxo exigem motivo. Os estados de espera exigem motivo quando substituem uma etapa em execução. ENTREGUE/CANCELADO somente via encerramento. OS terminada não reabre; retorno gera outra OS.

Pronto não é encerrado nem atrasado de execução. Previsão null não gera atraso. Alteração com data conhecida → null mantém motivo/histórico. Fotos publicadas não desaparecem ao mudar status.

Versões enviadas de adicional são imutáveis; decisões por item ou grupo dependente completo. Não permitir decisão contraditória posterior na mesma versão. Substituição marca pendências antigas indisponíveis e exige novo aceite para novos itens; escopo já aprovado/executado não é cobrado outra vez. Quantias aprovadas e executadas devem ser rastreadas à versão original. Encerramento cancela pendências explicitamente, preserva decisões e invalida grants na mesma transação.

Problema e justificativa são obrigatórios no envio de adicionais. fotoIds deve pertencer à mesma oficina e OS; somente fotos publicadas podem acompanhar o envio ao cliente. Não publicar imagem interna implicitamente. Iniciar execução bloqueia a OS e o item e valida aprovação da versão indicada; pendência/recusa retorna 409. Conclusão exige EM_EXECUCAO anterior. Cancelar/substituir uma solicitação não apaga execução existente; nenhum valor executado pode migrar silenciosamente para outra versão.

Vistoria tem uma versão rascunho editável; confirmação congela checklist e referências às fotos. Correção adiciona versão com motivo; a política de visibilidade atual ainda governa fotos, inclusive versões anteriores. Auditoria não é galeria pública.

## 7. Fotos e armazenamento

Limites iniciais configuráveis: 10 MiB por foto, 40 megapixels decodificados, 20 arquivos por seleção; upload individual com concorrência máxima 3. JPEG, PNG e WebP aceitos pelo servidor. HEIC/HEIF não garantidos no MVP: explicar formato incompatível e oferecer captura em formato compatível/seleção de JPEG. Testar essa alternativa em iPhone; não prometer suporte irrestrito.

Normalizar orientação, remover EXIF, produzir miniatura de até 480 px; preservar versão normalizada adequada à visualização, não dados sensíveis de câmera. Chaves aleatórias, fora da pasta pública. Estados PENDENTE, PRONTA, FALHOU e REMOVIDA; publicar somente PRONTA. Cliente gera identificador de upload por arquivo para reenvio seguro. Limpeza periódica de uploads órfãos após 24 horas.

S3 privado com acesso do backend; download transmitido após autorização a cada requisição, Cache-Control:no-store. Não expor URLs pré-assinadas persistentes: revogar a OS deve bloquear novas leituras imediatamente. Arquivo já baixado pelo cliente não pode ser remotamente apagado. Desenvolvimento usa diretório configurado fora do webroot, volume persistente a adicionar na tarefa 8.

## 8. Notificações e operação

Outbox na transação da OS; worker após commit. Chave única evento/versão/destinatário/canal. Tentativas após 1, 5, 15 e 60 minutos, depois falha visível; reenvio manual preserva identificação do evento. Entrega SMTP não prova leitura. Falha externa não altera a operação salva.

MAIL_HOST/PORT/USERNAME/PASSWORD/FROM, STORAGE_DRIVER/BUCKET/REGION e segredo HMAC por ambiente; sem credenciais em repositório. Resend exige domínio remetente configurado. Mailpit somente local. Backups de banco e objetos coordenados, restauração antes do lançamento na tarefa 20; nenhum recurso de produção provisionado aqui.

Migrações Flyway incrementais e imutáveis, inclusive tabelas Spring Session. Não aplicar scripts de teste em produção. Testes integrados usam PostgreSQL, não substituição por H2 para índices parciais/concorrrência.

## 9. Validação e rastreabilidade

[cenarios-validacao.md](cenarios-validacao.md) fornece massa sintética e casos positivos/negativos a implementar por módulo. Tarefa 1 valida apenas scaffold/inicialização e coerência documental; não declara testes de autorização aprovados antes de existirem módulos.

Tarefas 2–4: identidade/cadastro; 5–6: OS/eventos; 8–9: fotos/vistoria; 10–11: portal; 12–14: prazos/adicionais; 15–18: notificações/entrega/avaliação; 19–20: consolidação de testes e liberação.

Referências primárias consultadas: [Spring Session JDBC](https://docs.spring.io/spring-session/reference/guides/boot-jdbc.html), [Flyway PostgreSQL](https://documentation.red-gate.com/flyway/reference/database-driver-reference/postgresql-database), [SMTP Resend](https://resend.com/changelog/smtp-service), [validade de URLs S3](https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-presigned-url.html). Context7 não estava disponível; documentação oficial usada como alternativa.
