# Especificação técnica — tarefa 15

Spring Boot/JDBC/PostgreSQL e Angular 19 existentes. Migração V14 cria notificações
da oficina, outbox por cliente e histórico de tentativas. A chave única é oficina,
evento e referência imutável (OS, versão adicional, operação de decisão ou OS/versão
de previsão). Central compartilhada pelo proprietário da oficina; todas as consultas
e mutações usam a oficina autenticada, nunca um tenant fornecido pelo navegador.

Gravar aviso e outbox na transação de negócio, sem SMTP. Worker agendado processa
uma linha por transação independente usando `FOR UPDATE SKIP LOCKED`. Revalidar
cliente ativo, e-mail e instante de verificação antes de enviar; contato alterado
cancela a entrega, não migra mensagens para outro endereço. Transportador existente
`TransactionalEmail` falha explicitamente sem MAIL_FROM/provedor. Registrar somente
códigos de erro, não mensagens do provedor que possam expor PII.

Tentativa inicial + retentativas em 1, 5, 15 e 60 minutos; após isso FALHOU.
Reenvio manual somente de FALHOU, preservando ID e histórico, com cooldown de um
minuto. ENVIO SMTP é at-least-once: queda após aceitação remota e antes do commit
pode duplicar entrega; deduplicação evita novos registros/reenvios comuns, não
promete exactly-once que SMTP não oferece.

GET `/api/notificacoes?page=0&size=20&naoLidas=false`, PATCH `/{id}` com
`{"lida":true}`, POST `/{id}/reenvio`. DTO sem endereço do cliente nem payload interno.
UI `/notificacoes` com leitura, paginação, estado de envio e erro/reenvio explícitos.

Testes: PostgreSQL real para rollback, dedup, worker concorrente, contato revogado,
tentativas, isolamento e eventos; unitários para políticas e transporte; Angular
para integração HTTP/estados; Playwright desktop e mobile. Banco efêmero isolado,
SMTP mockado. Sem alterações em banco de usuário nem e-mails reais.

Referências: https://www.postgresql.org/docs/17/sql-select.html,
https://docs.spring.io/spring-framework/reference/integration/scheduling.html.
Context7 indisponível; documentação oficial consultada.
