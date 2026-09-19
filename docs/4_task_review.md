# Revisão: tarefa 4 — Cadastro de clientes e veículos

Revisor: task-reviewer (Codex). Data: 19/09/2026.
Fonte: [tasks.md](tasks.md), seção 4.0; contratos: [techspec.md](techspec.md).
Status: **APROVADO**.

## Resumo

A implementação atende aos cadastros isolados por oficina, normalização de CPF/placa,
vínculos históricos, verificação de e-mail e interface responsiva. A revisão encontrou duas
lacunas na busca mascarada e no limite de emissão por origem; ambas foram corrigidas e
receberam validação de regressão.

## Achados resolvidos

1. A busca agora remove a máscara de CPF e placa antes de comparar os valores normalizados,
   permitindo consultas como `529.982.247-25` e `BRA-1E23`.
2. A emissão de código passou a aplicar também limite persistente por endereço do cliente,
   usando somente o IP resolvido pela cadeia de proxy confiável já existente.

## Verificações

| Área | Resultado |
| --- | --- |
| Autorização e isolamento | Oficina sempre derivada da sessão; acesso cruzado retorna 404 |
| Integridade | Unicidade por oficina, FKs compostas e vínculo atual único |
| Concorrência | Versão exigida para edição/transferência; conflito retorna 409 |
| Verificação | HMAC, 10 minutos, uso único, cinco tentativas e limites de emissão |
| API | 33 testes aprovados com PostgreSQL 17 |
| Angular | 13 testes aprovados e build de produção aprovado |
| E2E | 6 jornadas aprovadas em desktop/mobile; largura de 320 px validada |

## Observações não bloqueantes

O bloqueio de transferência durante OS ativa depende da entidade da tarefa 5; a decisão está
registrada na especificação e o histórico atual já está modelado para manter o cliente da OS.
Safari/iOS e dispositivos físicos permanecem na validação final do MVP.

## Veredito

APROVADO. Não restam achados críticos ou major no escopo executável da tarefa 4.
