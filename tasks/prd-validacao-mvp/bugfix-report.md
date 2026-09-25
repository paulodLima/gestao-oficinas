# Relatório de correções · Validação do MVP

## Resumo

- Treze problemas tratados: nove encontrados no QA inicial, três na revisão independente e a base Angular 19 vulnerável.
- Correções e regressões detalhadas por ID em `bugs.md`; R20-01/R20-02/R20-03 rechecados pelo revisor independente.
- Cinco alertas moderados de ferramentas de desenvolvimento permanecem documentados como risco residual; não se declara correção desses cinco alertas.

## Detalhes

| IDs | Severidade | Status / correção | Regressão |
|---|---|---|---|
| QA-01–09 | Alta/média | Corrigidos: volume, orientação, retry, publicação, erro de adicionais, contraste, toque, labels e logout | Jornada SMTP/DB real, 4 configurações Playwright e testes de componentes |
| QA-10 / R20-01 | Alta | Corrigido: normalização sem EXIF/GPS em imagens novas e resposta de imagens legadas | JPEG sintético com GPS/câmera, orientação, WebP, limite 40MP e download HTTP autenticado |
| QA-11 / R20-02 | Média | Corrigido: três vagas compartilhadas por página | Quatro retries + nova seleção; sucesso e falha liberam vagas |
| QA-12 / R20-03 | Média | Corrigido: recarga de histórico fora do fieldset de mutações | DOM e E2E em OS encerrada, falha/recarga real, upload/remover bloqueados |
| QA-13 | Alta | Corrigido: Angular 20.3 autorizado, migrado e retestado | Audit produção 0; SSR, tipos e suítes completas |

## Testes finais

144 Java (unitários/integração), 111 Angular, 44 E2E de regressão e 8 cenários QA aprovados. TypeScript E2E sem erros; build SSR e imagens Docker aprovados. Ensaio de restauração sintética aprovado para 36 tabelas e 22 arquivos. Revisão aprovada com observações, descritas em `20_task_review.md`.

Não equivale a certificação WCAG, validação em aparelho físico, auditoria completa de segurança ou deploy de produção. Limites e preparação operacional constam em `qa-report.md` e `docker/QA.md`.
