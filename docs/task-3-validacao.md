# Validação — tarefa 3: configuração e identidade da oficina

Data: 19/09/2026. Escopo: seção 3.0 de [tasks.md](tasks.md). Revisão: [3_task_review.md](3_task_review.md).

## Entrega

Consulta e edição autenticadas de nome, contatos comerciais, endereço, horário e fuso da oficina.
Todas as operações derivam a oficina da sessão, usam CSRF, auditoria e controle otimista por versão.
O perfil público é opt-in e expõe somente os dados comerciais liberados.

Logo PNG/JPEG validada por conteúdo, limitada a 2 MiB e 4 megapixels, normalizada para PNG de até
512 px e armazenada sem metadados. Há alternativa visual pela inicial da oficina e remoção da logo.

O Angular oferece formulário responsivo, prévia, estados de carregamento/erro/sucesso, preservação
dos campos em falha e rota pública. A apresentação foi inspecionada na captura móvel gerada pelo E2E.

## Evidências executadas

| Verificação | Resultado |
| --- | --- |
| `mvnw.cmd -B -ntp test` | 25 testes aprovados, zero falhas; PostgreSQL 17 via Testcontainers |
| `npm test -- --watch=false --browsers=ChromeHeadless` | 9 testes aprovados após as correções finais |
| `npm run build` | Build Angular aprovado; componentes finais compilados novamente pelos testes Angular |
| `docker compose -f docker/docker-compose.yml up --build -d` | API e frontend construídos e ambiente iniciado |
| `playwright test` | 4 jornadas aprovadas: autenticação e tarefa 3 em desktop e Pixel 7 |
| `git diff --check` | Sem erros de whitespace; somente avisos locais de conversão LF/CRLF |

## Cobertura

- Duas oficinas não consultam nem alteram os dados ou a logo uma da outra.
- Escritas concorrentes com a mesma versão produzem um sucesso e um conflito 409.
- A resposta de escrita usa a versão criada pela própria transação.
- A logo pública é consultada atomicamente com o opt-in; despublicação e substituição mantêm 404.
- Formato, MIME, conteúdo, tamanho em bytes e dimensões da logo são validados.
- Perfil não publicado retorna 404 e o DTO público não contém IDs internos, versão ou credenciais.
- Falha de rede preserva os campos; validação inválida remove confirmação anterior de sucesso.
- Fluxo móvel não apresenta overflow horizontal na largura mínima validada de 320 px.

## Limites

O perfil público apresenta a identidade comercial da oficina. O portal operacional, acesso do cliente,
serviços e galeria pertencem às tarefas 10 e 11. O E2E usa Chromium/Pixel 7 emulado e não substitui
validação posterior em Safari/iOS e aparelhos físicos. As vulnerabilidades já registradas da base
Angular continuam como pendência de atualização antes da produção.
