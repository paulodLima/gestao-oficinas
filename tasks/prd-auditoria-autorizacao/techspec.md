# Tech Spec · Tarefa 19

Base: arquitetura Angular 19 + Spring Boot/JDBC + PostgreSQL/Flyway e sessão JDBC existente. Dependências 2–18 implementadas na master 1085f46.

## Decisões

- Manter isolamento por `oficina_id`, vínculo vigente e OS ativa em cada autorização; UUID não é autorização.
- Usar versão de acesso do cliente persistida para invalidar desafios/sessões após alteração de e-mail ou estado ativo. Preservar sessões independentes do proprietário e da avaliação ao expirar o acesso operacional.
- A versão de acesso também acompanha desafios de aprovação de adicionais; confirmação exige contato atualmente verificado, inclusive em sessão de link. Lock do cliente serializa emissão/confirmação com alteração de contato.
- Limites persistentes por origem e destinatário, cooldown de 60 segundos e invalidação de desafios anteriores. Resposta de emissão permanece genérica inclusive quando suprimida por limite/destinatário inexistente.
- Em adicionais, o grant já autoriza a consulta da solicitação: emissão usa limite de 5 pedidos/contato e 30/origem a cada 15 minutos, cooldown 60 segundos, sem invalidar o código vigente quando suprimido. Confirmação limitada a 60/origem. A emissão anônima de login mantém 202 genérico; a emissão autorizada de adicionais informa 429 para permitir recuperação na interface.
- Rotacionar sessão via Servlet `changeSessionId()` somente após autenticação válida.
- Pesquisa de clientes com CPF via POST e CSRF, não query string. Listagem GET sem termo permanece disponível.
- Auditar emissão e revogação de links com identificadores internos, sem token/código/CPF/conteúdo de e-mail.
- Configuração de produção falha fechada para origem não HTTPS ou segredo de desenvolvimento. TLS termina em proxy confiável; não publicar API/banco diretamente na internet.

## Validação

JUnit e integração com PostgreSQL real; testes Angular e Playwright de regressão. Matriz de evidências em `validacao.md`. Revisão independente obrigatória antes do merge.

## Referências oficiais

- https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html
- https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html
- https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html

Context7 indisponível nesta sessão; documentação oficial consultada diretamente.
