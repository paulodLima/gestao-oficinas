# Validação — tarefa 17

Data: 25/09/2026. Branch: `codex/tarefa-17-encerramento`, baseada na master `80d36e4`.
Cartão: https://trello.com/c/5A1vsurk.

## Implementação

- Encerramento transacional e definitivo de OS por entrega/cancelamento, confirmação explícita, versão otimista e motivo obrigatório no cancelamento.
- Pendências RASCUNHO/ENVIADA/PARCIALMENTE_DECIDIDA somente canceladas com consentimento; versões, itens e decisões anteriores não são removidos.
- Ordem de bloqueios OS → solicitações/versões/desafios; fechamento e decisão concorrentes são serializados. Fotos, remoção de fotos e rascunhos/correções de vistoria também revalidam sob a trava.
- Auditoria, evento de status, encerramento imutável, revogação de links/códigos e notificação na mesma transação. Falha não deixa efeitos parciais.
- Sessão de link perde autorização na próxima requisição. Identidade preservada consulta serviço atual, sem acessar histórico encerrado; retorno cria OS diferente e respeita vínculo atual.
- Interface mantém histórico e desabilita escrita após encerramento. Formulário de retorno não copia relato, quilometragem, previsão ou credenciais anteriores.

## Evidências executadas

- Maven `-Dtest.database=embedded test`: **103 testes**, zero falhas/erros/ignorados. PostgreSQL 17.6 real temporário em localhost, sem Docker.
- Testes focados `OrderClosurePolicyTest,OrderClosureIntegrationTest,PortalAccessIntegrationTest,AdditionalRequestServiceTest`: **20 aprovados**.
- Angular `npm test -- --watch=false --browsers=ChromeHeadless`: **81 aprovados**, incluindo retorno com responsável atual, vínculo indisponível, responsável inativo, troca de seleção e cadastros fora da primeira página (101 registros).
- `npm run build`: aprovado; somente os dois avisos CSS preexistentes em portal e página de OS (7,35/7,00 KB, limite 6 KB).
- Maven `-DskipTests package`: pacote executável Spring Boot gerado com sucesso após a suíte completa.
- Playwright `closure.spec.ts manual-share.spec.ts portal.spec.ts notifications.spec.ts`: **22 aprovados** (11 desktop, 11 Pixel 7), API simulada. Novos cenários: pronto → entrega → vazio no portal → nova OS → consulta por identidade; link antigo rejeitado; cancelamento com motivo; voltar sem encerrar; confirmação separada de pendências; ações desabilitadas após fechar.
- Capturas locais da confirmação desktop/mobile em `oficinas-app/test-results/encerramento-confirmacao-*.png`, examinadas visualmente; layout responsivo e sem rolagem horizontal. Captura de elemento longo inclui o cabeçalho fixo do aplicativo, não representa tela inteira.
- `git diff --check`: sem erros de whitespace.

## Segurança e limitações

Dados exclusivamente sintéticos; transporte de e-mail simulado, nenhum WhatsApp/e-mail real disparado. Não houve deploy de produção ou validação de SMTP externo. Encerrar atendimento não é apagar histórico. Conteúdo já exibido no navegador não pode ser retroativamente removido; toda requisição posterior revalida autorização.

O primeiro ensaio Java apontou mocks antigos da trava da OS; os mocks foram atualizados e a suíte completa passou posteriormente. Revisão independente em `17_task_review.md` antes da publicação.

A revisão identificou dependência indevida da primeira página de clientes/veículos no retorno. Corrigido com consultas individuais por ID e inclusão dos registros nas opções, sem confundir paginação com cadastro indisponível. Após o ajuste: 81 testes Angular, build e 22 E2E novamente aprovados. Backend sem alteração após seus 103 testes aprovados.

Revisão independente final: **APROVADO**, P2 resolvido, sem novos achados no delta.
