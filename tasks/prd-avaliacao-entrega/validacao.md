# Validação — tarefa 18

Data: 25/09/2026. Base: master `90ffa0e`; branch `codex/tarefa-18-avaliacao`.
Card: https://trello.com/c/8ZDp53o3.

## Escopo entregue

Resumo restrito após entrega; convite independente de sete dias, outbox e cópia
manual; revogação definitiva; nota inteira 1–5, comentário opcional e uma resposta
por OS; consentimento separado e privado por padrão; central privada paginada;
Google opcional igual para qualquer nota. Não há publicação automática, galeria
de depoimentos, disparo retroativo em massa nem envio de avaliações ao Google.

## Evidências executadas

- API: Maven `-Dtest.database=embedded test` — **121 testes**, sem falhas ou skips.
  PostgreSQL real temporário 17.6; e-mail substituído por mock. Inclui 8 casos de
  política e 10 cenários integrados novos de avaliação.
- Angular: `npm test -- --watch=false --browsers=ChromeHeadless` — **97 testes**.
- E2E: Node 24 + Playwright `test e2e/reviews.spec.ts e2e/closure.spec.ts
  e2e/manual-share.spec.ts e2e/portal.spec.ts e2e/notifications.spec.ts` — **34/34**,
  desktop e Pixel 7. Aplicação Angular real com API simulada; backend validado
  separadamente por HTTP/JDBC e PostgreSQL real. Não confundir com E2E de SMTP.
- `npm run build` aprovado. Persistem apenas dois avisos já existentes de tamanho
  CSS (`portal-access` 7,35 kB e `service-order-page` 7,00 kB; orçamento 6 kB).
- Maven `-DskipTests package` aprovado após a suíte completa; JAR executável gerado.
- Capturas `oficinas-app/test-results/avaliacao-formulario-{desktop,mobile}.png`,
  `avaliacao-confirmada-{desktop,mobile}.png` e `avaliacoes-oficina-{desktop,mobile}.png`.
  Inspeção visual de formulário mobile e central desktop sem sobreposição/cortes;
  asserções de ausência de overflow horizontal nas duas larguras.
- Logs locais ignorados pelo Git em `oficinas-api/target/task18-evidence/`:
  `all-tests.log`, `angular-tests.log`, `e2e.log`. Capturas também são locais.

## Segurança e regressão

Cobertos: entrega cria um único convite/outbox na mesma transação; rollback não
deixa convite; cancelamento e OS ativa rejeitados; expiração sete dias a partir
da entrega; token somente hash persistido; resumo sem relato/custos/CPF/placa ou
contatos do cliente; grant de avaliação não autoriza proprietário, fotos nem
portal operacional; nova OS não redireciona o convite antigo; troca inválida
limpa grant; POST sem CSRF negado; nota fracionada/fora do intervalo negada;
consentimento ausente é false; replay idêntico retorna original e divergente 409;
concorrência produz uma única avaliação e auditoria; revogação/contato alterado
e expiração bloqueiam sessão existente; oficina não acessa convites/resultados de
outra; Google bloqueia destinos arbitrários; worker envia convite dedicado ou
cancela revogado. Token não persiste no navegador e sai da URL antes da troca.

Frontend cobre trocas de convite serializadas, resposta atrasada descartada,
envio em curso antes de nova troca, confirmação de revogação, cópia sem envio,
consentimento privado, nota baixa/alta com mesmo Google link, remoção opcional
do link e paginação. Regressões de encerramento/retorno, compartilhamento,
notificações, galeria e decisão de adicional passaram.

Correções da revisão: o resumo agora fornece `contexto` não autorizador, exigido no
envio e comparado ao grant da sessão. Teste HTTP obtém A, troca para B de outra
oficina com o mesmo cookie e envia A: 409, nenhuma avaliação/auditoria; contexto
ausente também falha, B correto funciona. E2E com duas páginas reproduz esse fluxo,
remove o formulário obsoleto e preserva o consentimento correto. Tokens `#token=`
e `#token` na inicialização ou navegação são rejeitados e limpam grant, enquanto
recarga sem parâmetro restaura a sessão. Regressões em API, Angular e E2E passaram.

## Operação e limites

Migração V16 aditiva. `PUBLIC_URL` HTTPS e SMTP devem estar configurados na
implantação; `CODE_SECRET` deve permanecer protegido e estável. Rotação do segredo
impede reconstrução de convites antigos para reenvio, mas a validade de sessões e
tokens já distribuídos continua controlada por expiração/revogação persistidas.
SMTP mantém semântica at-least-once da fila existente. Não foi efetuado deploy
de produção, SMTP externo, acesso real ao Google ou publicação de depoimentos.

## Revisão independente

Primeira revisão `task-reviewer` solicitou correções de contexto entre abas e
token vazio. Ambas corrigidas e cobertas por regressões. Nova revisão independente
**APROVADA**, sem bloqueios, em `18_task_review.md`, antes do merge.
