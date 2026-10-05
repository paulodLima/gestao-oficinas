# Relatório de Bugfix - Abertura de ordem de serviço

## Resumo

- Total de Bugs: 1
- Bugs Corrigidos: 1
- Testes de Regressão Criados: 1

## Detalhes por Bug

| ID | Severidade | Status | Correção | Testes Criados |
|----|------------|--------|----------|----------------|
| BUG-14 | Alta | Corrigido | Aceita pontos de milhar, formata o campo e converte a quilometragem para inteiro antes do envio. | `aceita quilometragem com ponto de milhar e envia o número normalizado` |

## Testes

- Testes unitários: 114 testes do frontend passando, incluindo a regressão de quilometragem formatada.
- Testes de integração: 28 conjuntos de testes do backend concluídos sem falhas.
- Testes E2E: não executado nesta correção; o runner Playwright não estava disponível e a ferramenta visual não iniciou nesta sessão.
- Tipagem: compilação Angular concluída com sucesso após sincronizar as dependências locais com Angular 20 declarado pelo projeto.
