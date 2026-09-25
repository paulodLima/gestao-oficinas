# PRD · Auditoria e autorização

Detalhamento da tarefa 19 já aprovada em `docs/prompt.md`, `docs/techspec.md` e `docs/tasks.md`. Não amplia o MVP.

## Objetivo

Garantir isolamento entre oficinas e clientes, acesso privado a fotografias e revogação efetiva das credenciais do portal. Proteger dados pessoais e segredos em URLs, respostas e logs.

## Critérios de aceite

- Matriz negativa com duas oficinas e clientes distintos, incluindo download privado de fotografias.
- Códigos expirados, reutilizados ou bloqueados não autenticam; emissão tem limites e resposta não enumerável.
- Transferência do veículo, mudança de contato e revogação/renovação do link não reativam acessos antigos.
- Autenticação renova o identificador de sessão; link inválido não conserva a autorização operacional anterior.
- CPF pesquisado não aparece na URL gerada pelo aplicativo; auditoria registra ator, recurso e ação sem segredos.
- Regressões de concorrência de OS e aprovação continuam passando.
- Produção exige HTTPS e segredo apropriado; guia distingue configuração verificada de implantação real.

## Fora do escopo

Deploy em produção, envio a clientes reais, pentest externo e mudança visual do produto.
