# Cenários de validação — massa sintética do MVP

Referência: [techspec.md](techspec.md). Preparação documental da tarefa 1.4; não são testes já executados. Implementar fixtures isoladas nos testes de cada módulo, sem inserir dados em produção ou enviar e-mail real.

## Massa reproduzível

Usar UUIDs fixos de teste por alias; contatos em example.test capturados no Mailpit. CPF sintético gerado pelo builder de teste com dígitos verificadores, nunca de pessoa real. Relógio fixo T0 = 2026-09-18T12:00:00Z.

| Alias | Oficina | Cadastro e vínculo |
| --- | --- | --- |
| OF_A | Oficina Alfa Teste | slug alfa-teste; America/Sao_Paulo |
| OF_B | Oficina Beta Teste | slug beta-teste; America/Sao_Paulo |
| DONO_A / DONO_B | A / B | dono.a@example.test / dono.b@example.test |
| CLIENTE_A1 | A | cliente.a1@example.test verificado |
| CLIENTE_A2 | A | cliente.a2@example.test não verificado |
| CLIENTE_B1 | B | cliente.b1@example.test verificado |
| VA1 | A | placa ABC1D23; responsável A1 |
| VA2 | A | placa XYZ9876; responsável A1 |
| VB1 | B | placa ABC1D23; responsável B1; testa mesma placa em outra oficina |
| OS_A_ATIVA | A | VA1/A1; EM_MANUTENCAO; previsão T0+2h |
| OS_A_ENCERRADA | A | VA1/A1; ENTREGUE em T0-30d; ordem anterior |
| OS_B_ATIVA | B | VB1/B1; AGUARDANDO_PECAS |
| FOTO_PUBLICA / FOTO_INTERNA | A | OS_A_ATIVA; conteúdos distintos identificáveis |
| ADICIONAL_A_V1 | A | enviada; peça 2 × 100.00 e mão de obra 1 × 50.00; total 250.00 |
| LINK_ANTIGO | A | OS_A_ENCERRADA; revogado |
| GRANT_AVALIACAO | A | fixture entregue em T0-1d; válido até entrega+7d |

Cada teste reinicia fixtures em banco PostgreSQL descartável. Cenários que requerem entrega ou troca de proprietário devem criar estado independente, não depender da execução de outro teste.

## Casos e resultados esperados

| Caso | Ação | Resultado | Tarefas |
| --- | --- | --- | --- |
| C01 | DONO_A consulta OS_B_ATIVA por UUID | 404 sem metadados da B | 2, 5, 19 |
| C02 | CLIENTE_B1 tenta foto da OS_A_ATIVA | 404, nenhum byte | 8, 10, 19 |
| C03 | CLIENTE_A1 consulta galeria | somente FOTO_PUBLICA, sem campos internos | 8, 11 |
| C04 | duas requisições abrem OS para VA1 livre simultaneamente | uma 201 e outra 409; uma linha ativa | 5 |
| C05 | mesma placa em OF_B | não colide com veículo da OF_A | 4, 5 |
| C06 | código após 10min, repetido ou sexto erro | não autentica; limites não revelam cadastro | 10 |
| C07 | código válido seguido de replay | primeiro cria sessão, segundo falha | 10 |
| C08 | CLIENTE_A2 verifica primeiro e-mail | código verifica posse; código não depende de verificação prévia | 4 |
| C09 | link só leitura tenta aprovar adicional | negado sem decisão; solicitar código de aprovação | 14 |
| C10 | aprovar peça e recusar mão de obra independentes | decisões individuais, autorizado 200.00 | 14 |
| C11 | tentar decidir parte de grupo indivisível | 400, nenhuma decisão parcial | 14 |
| C12 | versão substituída recebe decisão antiga | 409, histórico anterior preservado | 13, 14 |
| C13 | decisão e encerramento simultâneos | ordem transacional consistente; nenhuma decisão após encerramento | 17 |
| C14 | mudar status após publicar fotos | galeria mantém fotos de todas as etapas | 6, 11 |
| C15 | upload interrompido e reenviado com mesmo identificador | não duplicar; sem falso sucesso | 8, 9 |
| C16 | câmera negada ou HEIC incompatível | alternativa de arquivo/captura e mensagem; formulário preservado | 9, 20 |
| C17 | previsão T0-1h em manutenção versus pronto | primeira atrasada; pronto apenas aguardando retirada | 12 |
| C18 | encerrar OS com link e sessão já abertos | próximas leituras operacionais negadas; histórico interno íntegro | 17 |
| C19 | nova OS para VA1 após entrega | identidade A1 acessa nova OS; LINK_ANTIGO nunca acessa | 17 |
| C20 | transferir VA1 sem OS ativa para A2 | A2 não acessa histórico A1; A1 não acessa nova OS de A2 | 4, 17 |
| C21 | avaliação duplicada ou após sete dias | 409 duplicidade ou 401 expiração; não reativa OS | 18 |
| C22 | falha SMTP e retry do mesmo evento | OS preservada; envio retentável sem duplicação lógica | 15 |
| C23 | reenviar decisão com mesma Idempotency-Key e decisões por item diferentes | 409, não reutilizar resultado de outro corpo | 14 |
| C24 | dono envia expectedVersion antiga | 409, não sobrescrever atualização alheia | 6, 12 |
| C25 | pular etapa e voltar sem motivo | salto permitido; retorno recusado até informar motivo | 6 |
| C26 | custo decimal 3 × 0.10 | total 0.30 exato | 13 |
| C27 | sessão autenticada sem OS ativa | servico:null e contato; sem galeria antiga | 11, 17 |

## Critérios de evidência

Registrar requisição sem segredo, status, assertivas de banco e navegador quando aplicável. Testes de concorrência usam barreira para disparar simultaneamente e verificam estado final. Testes móveis incluem Safari/iOS e Chrome/Android com permissão de câmera, rede lenta, imagens rotacionadas, galeria, 320px e alvos de 44px. Não registrar CPF/token/senha em relatório.
