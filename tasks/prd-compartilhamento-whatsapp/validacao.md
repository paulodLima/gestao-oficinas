# Validação da tarefa 16

Data: 25/09/2026. Branch `codex/tarefa-16-whatsapp`, criada da master `aec0561` atualizada com origin.

## Resultados

| Camada | Comando | Resultado |
|---|---|---|
| Backend/PostgreSQL 17 | `mvn -Dtest.database=embedded test` | 94 testes, zero falhas/erros/skips |
| Angular/Chrome Headless | `npm test -- --watch=false --browsers=ChromeHeadless` | 67 testes aprovados |
| TypeScript | `tsc --noEmit -p tsconfig.app.json` e `tsc --noEmit -p tsconfig.spec.json` | Ambos aprovados |
| Build | `npm run build` | Aprovado; dois avisos de CSS preexistentes (portal e detalhe da OS) |
| Navegador | `playwright test e2e/manual-share.spec.ts e2e/portal.spec.ts e2e/notifications.spec.ts` | 18 cenários aprovados: 9 desktop e 9 mobile |

Playwright executado com Node 24 do runtime local. PostgreSQL efêmero em loopback; Docker não é necessário neste modo. Não houve uso de banco de produção.

## Cobertura da entrega

- Mensagem/codificação: acentos, quebras de linha, fragmento codificado e ausência de dados pessoais/telefone de destino.
- Compartilhar: preparação por clique, abertura em nova aba com `noopener noreferrer`, cópia real em navegador de teste, fallback por permissão negada e seleção do campo.
- Estado: erro ao gerar/revogar, ausência de falsa confirmação, prevenção de cliques duplicados, limpeza ao trocar OS/encerrar/destruir e timer de expiração.
- Portal: fragmento removido antes da troca, compatibilidade de query antiga, reload com sessão HTTP sem token e formulário quando não há sessão válida.
- Regressão C1 da revisão: navegação para links diferentes/repetidos na mesma aba, da tela de login e de outra OS, sem reload. Eventos finais/ignorados do roteador consomem o token da URL atual; contexto anterior é limpo imediatamente. Trocas de concessão são serializadas e respostas de carga/autenticação antigas são descartadas.
- API real/PostgreSQL: token/hash, prazo, escopo de oficina/OS, leitura apenas, expiração/revogação de links e sessões, reemissão, encerramento e duas emissões concorrentes com somente um link não revogado.
- Regressão: tentativa de restaurar portal sem concessão não encerra sessão autenticada do proprietário.
- Interface inspecionada nas capturas de teste desktop/mobile; fluxo responsivo e verificação de ausência de overflow horizontal em 320px.

## Limites e segurança

O destino `wa.me` é interceptado no navegador de teste, com token fictício. Nada foi enviado ao WhatsApp nem a contatos reais. A confirmação no aplicativo nativo/dispositivo físico não foi testada; depende do usuário e da disponibilidade do serviço externo.

Os 18 E2E usam APIs simuladas; os testes de integração HTTP/PostgreSQL validam os contratos reais separadamente. Não foi executada toda a suíte E2E dependente de API/Mailpit local. Não houve deploy de produção.

Uma execução intermediária teve o primeiro cenário bloqueado pelo overlay de erro da recompilação HMR. Após encerrar a recompilação, a suíte completa foi repetida: 18/18 aprovados, sem ignorar cenário ou forçar clique sobre overlay.

Imagens locais reproduzíveis: `oficinas-app/test-results/compartilhamento-desktop.png` e `compartilhamento-mobile.png` (artefatos ignorados pelo Git). Evidência durável: testes versionados e este relatório.

Revisão independente: `16_task_review.md`, parecer final **APROVADO** após correções C1/M1. A reprodução independente de navegação na mesma aba passou em desktop/mobile, assim como compilação Angular e typecheck.
