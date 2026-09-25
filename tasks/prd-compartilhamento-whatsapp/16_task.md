# 16.0 Compartilhamento manual pelo WhatsApp

Status: concluída, testes e revisão independente aprovados.

Dependência 10 concluída: emissão e revogação de links, autenticação pública e testes de escopo existentes.

- [x] 16.1 Compor mensagem mínima com link autorizado, sem CPF.
- [x] 16.2 Abrir WhatsApp manualmente e copiar link com alternativa acessível.
- [x] 16.3 Explicar envio manual sem registrar entrega como confirmada.
- [x] 16.4 Testes unitários, integração PostgreSQL e E2E desktop/mobile.

Riscos: vazamento do token em query/referrer, reutilização de link revogado, resposta atrasada de outra OS e falsa confirmação de envio. Mitigar com fragmento removido, sessão revalidada, estado por OS e texto explícito.

Plano: preparar componente e contrato de mensagem; proteger emissão e consumo; testar as camadas; revisar; publicar branch e merge na master; concluir cartão no Trello.

Evidências: `validacao.md`. Resultado: 94 Java, 67 Angular, 18 E2E e build aprovados. Revisão identificou navegação por fragmento na mesma aba; corrigida com regressões e descarte de respostas atrasadas.
