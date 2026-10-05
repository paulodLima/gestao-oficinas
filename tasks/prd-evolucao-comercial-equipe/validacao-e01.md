# Validação E01 — Baseline das jornadas essenciais

Data da validação: 29/09/2026.

## Resultado

E01 concluída. O MVP ficou com baseline automatizada verde no backend, frontend e jornadas completas em navegador. Não foi identificado defeito bloqueador aberto de perda de dados, isolamento entre oficinas ou acesso indevido dentro do escopo validado.

## Mapeamento funcional e evidências

| Jornada | Implementação reutilizada | Evidência principal |
| --- | --- | --- |
| Cadastro, login, recuperação e sessão | módulos `identidade` e telas de autenticação | `AuthIntegrationTest` e `e2e/auth.spec.ts` |
| Clientes, veículos, CPF e placa | módulo `cadastro` e página única de cadastros | `CpfPlateTest`, `CustomerVehicleIntegrationTest` e `e2e/customer-vehicle.spec.ts` |
| Abertura, busca, datas, km e concorrência da OS | módulo `ordem` e página de OS | `ServiceOrderIntegrationTest` e `e2e/service-order.spec.ts` |
| Etapas, comunicação e prazo | status, atualizações e previsões da OS | `ServiceOrderStatusTest`, `ForecastPolicyTest` e `e2e/service-order.spec.ts` |
| Vistoria e fotos privadas/públicas | vistoria e armazenamento de fotos da OS | `InspectionChecklistPolicyTest`, `PhotoStorageTest`, `e2e/service-order.spec.ts` e `e2e/portal.spec.ts` |
| Adicionais e aprovação do cliente | módulos `adicional` e decisão no portal | testes `Additional*` e `e2e/portal.spec.ts` |
| Link/código, troca de link e revogação | módulo `portal` e compartilhamento da OS | testes `PortalAccess*`, `e2e/manual-share.spec.ts` e `e2e/closure.spec.ts` |
| Encerramento, entrega, retorno e nova OS | encerramento da OS | `OrderClosureIntegrationTest`, `OrderClosurePolicyTest` e `e2e/closure.spec.ts` |
| Perfil e horário da oficina | módulo `oficina` e tela Perfil | `ShopIntegrationTest` e `e2e/shop.spec.ts` |

## Regressões confirmadas e corrigidas

1. A seleção pública de oficina aceitava diferenças de caixa e acentos, mas não espaços antes/depois. A normalização agora remove espaços externos e há regressão automatizada cobrindo a entrada.
2. A grade semanal de horários excedia a largura em telas de 320 px. O layout móvel agora reorganiza horários em linhas sem rolagem horizontal.
3. Testes antigos ainda usavam o layout anterior, com formulários e blocos da OS simultaneamente visíveis. Os cenários foram atualizados para busca focada, abertura explícita de cadastros, seleção da OS e navegação por abas.
4. Fixtures de adicionais e portal estavam defasadas dos contratos atuais (`blocos` e catálogo público de oficinas). Foram alinhadas sem criar contrato paralelo.

### Registro de reprodução

| Prioridade | Defeito | Passos de reprodução antes da correção | Resultado anterior | Evidência após correção |
| --- | --- | --- | --- | --- |
| P1 | Nome da oficina com espaços externos não era reconhecido no portal | Abrir `/acompanhar`; escolher uma oficina; informar o mesmo nome com espaços antes/depois e caixa diferente; solicitar código | Mensagem “Selecione uma oficina da lista” e nenhuma solicitação de código | `portal-access.component.spec.ts` valida nome com espaços e caixa diferente |
| P1 | Horários provocavam rolagem horizontal em celular estreito | Entrar em Perfil; manter ao menos um dia aberto; reduzir a viewport para 320 × 760 | `documentElement.scrollWidth` ficava maior que `innerWidth` | `e2e/shop.spec.ts` valida o perfil sem transbordamento em 320 px |
| P2 | Jornadas automatizadas não refletiam telas focadas e abas atuais | Rodar `npm run test:e2e` após a reorganização de Clientes, Veículos e OS | Localizadores aguardavam formulários/ações fora da aba ou ainda não abertos | 46/46 jornadas E2E aprovadas em desktop e mobile |
| P2 | Fixtures não compilavam com os contratos atuais | Rodar `npm test -- --watch=false --browsers=ChromeHeadless` | Erros de TypeScript por ausência de `blocos` e uso do antigo campo `slug` | 116/116 testes do frontend aprovados |

## Execuções

| Validação | Resultado |
| --- | --- |
| `mvn test` | 144 testes; 0 falhas; 0 erros |
| `npm test -- --watch=false --browsers=ChromeHeadless` | 116 testes; 116 aprovados |
| `npm run build` | concluído; apenas avisos de orçamento CSS já conhecidos |
| `npm run test:e2e` | 46 jornadas aprovadas: 23 desktop e 23 mobile |
| `docker compose up -d --build --force-recreate api app` | imagens geradas e serviços iniciados |

Os testes E2E usaram massa sintética. O envio SMTP foi direcionado temporariamente ao Mailpit local; ao final, a API foi recriada novamente com a configuração do arquivo local. Nenhuma comunicação real foi enviada.

## Limitações e pendências externas

- A aceitação de destinatários externos pelo SMTP corporativo depende da política de relay do ambiente e não foi simulada como sucesso.
- Os avisos de tamanho CSS não bloqueiam o build, mas devem ser acompanhados em refatoração visual futura.
- Esta baseline não implementa equipe/funcionário; essa expansão começa na E02.

## Conclusão operacional

As jornadas essenciais do proprietário e do cliente estão cobertas em desktop e celular, incluindo revogação de link antigo após encerramento e abertura de um novo atendimento. A E02 pode iniciar sobre esta baseline.
