# Revisão: Tarefa 1.0 (E01) — Estabilizar jornadas essenciais e consolidar baseline

**Revisor**: AI Code Reviewer  
**Data**: 2026-09-29  
**Arquivo da task**: `1_task.md`  
**Status**: APROVADO COM OBSERVAÇÕES

## Resumo

A E01 estabiliza a baseline sem antecipar a área de funcionários. As correções atribuídas à tarefa estão consistentes com RF01: a seleção da oficina normaliza nome/slug sem diferenciar caixa, acentos ou espaços externos; os horários do Perfil passam a se reorganizar em telas estreitas; fixtures foram alinhadas aos contratos atuais; e os E2E acompanham a navegação por busca, criação explícita e abas da OS. A suíte unitária do frontend foi reexecutada nesta revisão com 116/116 testes aprovados. As evidências registram ainda 144/144 testes no backend, 46/46 jornadas E2E e build concluído.

Não encontrei defeito crítico nem regressão funcional bloqueante nas mudanças específicas da E01. Há uma lacuna documental não bloqueante e um ponto de fidelidade dos testes E2E a melhorar.

## Arquivos Revisados

| Arquivo | Status | Problemas |
|---------|--------|-----------|
| `tasks/prd-evolucao-comercial-equipe/1_task.md` | ✅ OK | 0 |
| `tasks/prd-evolucao-comercial-equipe/prd.md` | ✅ OK | 0 |
| `tasks/prd-evolucao-comercial-equipe/techspec.md` | ✅ OK | 0 |
| `tasks/prd-evolucao-comercial-equipe/validacao-e01.md` | ⚠️ Problemas | 1 |
| `oficinas-app/src/app/portal/portal-access.component.ts` | ✅ OK | 0 |
| `oficinas-app/src/app/portal/portal-access.component.spec.ts` | ✅ OK | 0 |
| `oficinas-app/src/app/oficina/shop-page.component.css` | ✅ OK | 0 |
| `oficinas-app/src/app/ordem/additional-request.component.spec.ts` | ✅ OK | 0 |
| `oficinas-app/e2e/service-order.spec.ts` | ⚠️ Problemas | 1 |
| `oficinas-app/e2e/customer-vehicle.spec.ts` | ⚠️ Problemas | 1 |
| Demais E2E atualizados (`closure`, `manual-share`, `portal`, `shop`) | ✅ OK | 0 |

## Problemas Encontrados

### 🔴 Problemas Críticos

Nenhum problema crítico encontrado.

### 🟡 Problemas Major

1. **A evidência não registra prioridade e passos de reprodução para cada regressão confirmada.**  
   Arquivo: `tasks/prd-evolucao-comercial-equipe/validacao-e01.md:23-28`.  
   A subtask 1.3 exige massa sintética, prioridade e passos reproduzíveis. O documento lista causa e correção, mas não informa prioridade, pré-condições, passos e resultado anterior. Isso não invalida as correções — que possuem testes verdes —, porém deixa a trilha de auditoria incompleta. Recomenda-se transformar os quatro itens em uma tabela com `prioridade`, `massa/pré-condição`, `passos`, `resultado anterior`, `correção` e `teste de regressão`.

### 🟢 Problemas Minor

1. **Dois E2E disparam o evento diretamente em vez de reproduzir o clique do usuário.**  
   Arquivos: `oficinas-app/e2e/service-order.spec.ts:27` e `oficinas-app/e2e/customer-vehicle.spec.ts:38`.  
   `dispatchEvent('click')` pode passar mesmo quando o controle está encoberto, desabilitado ou fora de uma condição real de interação. Prefira `click()`; caso exista uma razão técnica para o disparo direto, documente-a no teste e acrescente uma asserção de visibilidade/habilitação antes da ação.

## ✅ Destaques Positivos

- A resolução da oficina usa o slug interno somente depois de localizar a opção pública por nome/slug normalizado, sem confiar no texto livre como identificador do servidor.
- O teste unitário cobre explicitamente caixa alta e espaços externos em `OFICINA CENTRAL`, evitando regressão do caso corrigido.
- A correção responsiva é localizada ao componente de Perfil e mantém os campos de abertura/fechamento legíveis em largura móvel.
- As fixtures passaram a refletir o contrato real de adicionais (`blocos`) e o catálogo público de oficinas, sem duplicar interfaces.
- Os E2E agora selecionam a OS e a aba correta antes de operar, reproduzindo o comportamento atual da interface.
- A validação separa limitações externas de SMTP e não transforma ausência de integração real em sucesso simulado.

## Conformidade com Padrões

| Padrão | Status |
|--------|--------|
| Padrões de Código | ✅ |
| TypeScript/Node.js | ✅ — compilação Angular exercitada pela suíte; 116/116 testes |
| REST/HTTP | ✅ |
| Logging | ✅ — sem alteração relevante nesta E01 |
| Angular | ✅ |
| Testes | ⚠️ — suites verdes; duas ações E2E usam `dispatchEvent` |

## Validação Executada

- `npm.cmd test -- --watch=false --browsers=ChromeHeadless`: **116/116 aprovados** nesta revisão.
- O projeto não possui script `typecheck` nem usa Bun; a verificação equivalente ocorre na compilação Angular da suíte e no `npm run build` registrado na evidência.
- `mvn test`: **144 testes, 0 falhas e 0 erros**, conforme execução registrada em `validacao-e01.md`.
- `npm run test:e2e`: **46/46 jornadas**, conforme execução registrada em `validacao-e01.md`.
- `npm run build`: concluído, com apenas avisos de orçamento CSS já documentados.

## Recomendações

1. Completar `validacao-e01.md` com prioridade e passos reproduzíveis dos quatro defeitos confirmados antes de usar a E01 como artefato formal de auditoria.
2. Substituir os dois `dispatchEvent('click')` por interação real do Playwright, mantendo as suites verdes.
3. Prosseguir para E02 sem ampliar o escopo da E01; as observações acima não bloqueiam a baseline funcional.

## Veredito

**APROVADO COM OBSERVAÇÕES.** A implementação está funcional, coberta pelas suites registradas e não apresenta achados críticos. A E02 pode começar. A pendência principal é completar a rastreabilidade documental exigida pela própria subtask 1.3; o ajuste dos dois E2E é recomendado para aumentar a fidelidade, mas não impede o uso da baseline.

## Tratamento pós-revisão

- O achado major foi resolvido: `validacao-e01.md` agora registra prioridade, passos de reprodução, resultado anterior e evidência de regressão para cada classe de defeito.
- O achado minor foi mitigado: os dois controles são verificados como visíveis e habilitados antes do disparo, e o teste documenta a instabilidade de hit-testing do overlay SSR no Chromium headless. A tentativa de substituir o disparo por foco + Enter foi executada e se mostrou intermitente; os quatro cenários afetados foram reexecutados e aprovados em desktop e mobile.
