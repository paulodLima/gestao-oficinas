# 20 · Validação móvel e entrega do MVP

Trello: https://trello.com/c/YphJ3GJG

## Plano

- [x] 20.1 Criar ambiente isolado e executar jornada integrada com notificações SMTP locais.
- [x] 20.2 Validar desktop, Android/Chrome emulado, WebKit/iPhone emulado e 320px.
- [x] 20.3 Testar alternativa à câmera, orientação, falha/rede lenta/reenvio; corrigir regressões.
- [x] 20.4 Verificar teclado, labels, contraste, alvos de toque e estados de interface.
- [x] 20.5 Executar suíte completa, smoke Docker, backup/restauração e registrar evidências/instruções.
- [x] Revisão independente aprovada com observações; R20-01/R20-02/R20-03 resolvidos.
- [x] Publicar branch, merge na master e ajustar Trello.
- [x] Migração Angular 19→20 autorizada em 25/09/2026 e executada; auditoria npm de produção sem alertas, build e testes unitários aprovados.

Em 25/09/2026: Angular atualizado com autorização; três achados da revisão corrigidos com regressões. Validação final aprovada: 144 Java, 111 Angular, 44 E2E de regressão e 8 QA; build/TypeScript/Docker e auditoria npm de produção (0 alertas). Ver `qa-report.md` e revisão independente. Nenhum deploy de produção ou alteração no ambiente principal.

## Publicação confirmada

- Branch `codex/tarefa-20-validacao-mvp`, implementação `797deeccfd5031ad1895cb61703dc751e4456918`, enviada ao GitHub.
- Merge da implementação na `master`: `ab3d1bd4be03e1fdee777485a7e61788f4ded7f7`, push e SHA remoto confirmados.
- Card 20 movido para **Concluído** e marcado concluído no Trello; estado visível confirmado. As listas Backlog MVP, Pronto para iniciar, QA e Bloqueado estavam vazias; a tarefa 20 era a última em desenvolvimento.
- Tarefa 19 também publicada e concluída. Prévia local do código validado em http://localhost:14220; contêineres originais preservados em 4200/8080/5432.
- Este registro administrativo posterior não altera os artefatos de código já testados.

## Critério de conclusão

Todos os RFs verificados no ambiente equivalente autorizado pelo PRD global, correções testadas e nenhum bloqueador aberto. Relatório distingue emulação de dispositivo físico e validação local de produção.
