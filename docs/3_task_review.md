# Revisão: tarefa 3 — Configuração e identidade da oficina

Revisor: task-reviewer (Codex). Data: 19/09/2026.
Fonte: [tasks.md](tasks.md), seção 3.0; contratos: [techspec.md](techspec.md).
Status: **APROVADO**.

## Resumo

A implementação atende à edição isolada por oficina, logo validada, perfil público opt-in, formulário
responsivo e tratamento de erros. A primeira revisão encontrou duas janelas de concorrência e uma
mensagem contraditória na interface. As três foram corrigidas e receberam testes de regressão.

## Achados resolvidos

1. Os métodos transacionais agora devolvem o perfil e a versão produzidos pela própria escrita antes
   do commit, impedindo que uma resposta adote a versão de outra sessão e sobrescreva dados antigos.
2. A logo pública é lida por uma única consulta que exige simultaneamente slug, publicação ativa e
   imagem existente; uma imagem substituída depois da despublicação não pode ser retornada.
3. Carregamento e salvamento limpam confirmações anteriores antes de validar ou iniciar nova operação.

## Verificações

| Área | Resultado |
| --- | --- |
| Autorização e isolamento | Identidade da sessão em todas as escritas; campos desconhecidos recusados |
| Concorrência | UPDATE por id/versão e regressão concorrente 200/409 |
| Publicação | Desativada por padrão; projeção pública sem IDs, versão ou credenciais |
| Logo | Conteúdo e MIME conferidos, limites de bytes/pixels, recodificação PNG e `nosniff` |
| API | 25 testes aprovados com PostgreSQL 17 |
| Angular | 9 testes aprovados; formulário preserva campos em falha |
| E2E | 4 jornadas aprovadas em desktop e Pixel 7, incluindo contexto público anônimo |
| Mobile | Sem overflow na largura validada de 320 px; captura inspecionada |

## Observações não bloqueantes

A validação móvel usa emulação Chromium. Safari/iOS e dispositivos físicos permanecem na validação
final do MVP. A atualização das dependências Angular com alertas conhecidos continua fora desta tarefa.

## Veredito

APROVADO. Não restam achados críticos ou major no escopo da tarefa 3.
