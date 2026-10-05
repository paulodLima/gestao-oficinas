# PRD — Evolução comercial e área mobile da equipe

## Visão Geral

Versão 1.0 · 29/09/2026 · Slug: evolucao-comercial-equipe.
Escopo aprovado na conversa: 15 entregas incrementais, sem implementar funcionalidades nesta etapa documental.
Evoluir o MVP de acompanhamento para uma operação confiável, atualizada pela equipe no celular e comercializável por assinatura. Preservar histórico, contratos e recursos já entregues; identificar lacunas antes de refazer módulos.

Público inicial proposto para validação: pequenas oficinas de funilaria/pintura e mecânica. O segmento é hipótese comercial, não comprovação de demanda. Benefício: menos solicitações de status, atualizações simples e adicionais aprovados com clareza.

## Objetivos

- Permitir localizar a OS e registrar uma atualização simples no celular com meta de usabilidade de até 30 segundos, excluindo transmissão de arquivos e medida em piloto.
- Nenhuma alteração de etapa sem evidência exigida, autorização ou confirmação de persistência.
- Impedir acesso entre oficinas e impedir que funcionário bloqueado continue operando.
- Medir ativação, uso semanal, atualizações por OS, acesso ao portal, tempo de aprovação, suporte por oficina e disposição a pagar.
- Pilotar com cinco oficinas mediante adesão explícita. Meta proposta: quatro abrirem e compartilharem a primeira OS com no máximo uma sessão de orientação; três seguirem ativas ao final de quatro semanas.
- Não prometer disponibilidade ou escala sem teste; validar recuperação de dados e custos antes de venda ampliada.

## Histórias de Usuário

- Como proprietário, convido funcionários, escolho funções e controlo quem pode atualizar cada OS.
- Como técnico, entro com meu login, busco pela placa ou abro meus serviços e concluo uma etapa com fotos.
- Como técnico com conexão instável, vejo a falha e tento novamente sem duplicar fotos ou avançar duas vezes.
- Como atendimento, acompanho pendências e comunico alterações, sem aprovar despesas pelo cliente.
- Como cliente, vejo fotos da etapa correta, avanço real e previsão identificada como estimativa.
- Como gestor, identifico gargalos e valores autorizados sem confundi-los com recebimentos.
- Como operador do SaaS, acompanho consumo, suporte e assinaturas sem acesso irrestrito aos dados das oficinas.

## Funcionalidades Principais

1. **RF01 — Confiabilidade:** validar jornadas atuais de entrada, fotos, acesso, adicionais e entrega; corrigir regressões comprovadas com testes.
2. **RF02 — Equipe:** cadastro individual com nome, celular, função e identificador de login; convite único com validade, definição de senha, recuperação, logout e bloqueio. Não usar CPF ou senha compartilhada. Permitir entregar convite manualmente sem contratar SMS.
3. **RF03 — Permissões:** proprietário administra; atendimento opera cadastros/comunicação conforme matriz; técnico consulta e atualiza somente conteúdo necessário. Dono configura alcance por funcionário: somente atribuídas (padrão aprovado) ou todas as OS da própria oficina. Busca também respeita esse alcance. Entrega/cancelamento são restritos; técnico não vê financeiro nem aprova adicionais.
4. **RF04 — Área mobile:** busca normalizada por placa, lista paginada de serviços atribuídos, placa/modelo/etapa em destaque, fotos e orientações. Sem menus administrativos. Consulta vazia não despeja o cadastro da oficina.
5. **RF05 — Fluxos:** modelos mecânica, funilaria/pintura e rápido; etapas opcionais e política de fotos por etapa. A OS conserva o fluxo escolhido; editar um modelo não altera retroativamente serviços em andamento. Mudança explícita de fluxo é auditada.
6. **RF06 — Conclusão:** prévia de fotos, comentário e próxima etapa; ação “Salvar e avançar”. Evidências pertencem à etapa concluída. Exigir ao menos uma foto nova válida para etapas configuradas; espera por peças/aprovação pede motivo, foto opcional. Retorno exige justificativa e preserva passagens anteriores. Nenhum caminho alternativo pode contornar exigência de evidência.
7. **RF07 — Falhas:** reenvio seguro, progresso, prevenção de duplo clique, conflito quando outra pessoa altera a OS e aviso de sessão expirada. Não prometer funcionamento offline completo; informar limites de preservação ao fechar/recarregar.
8. **RF08 — Portal:** exibir etapas previstas, concluídas, atual e restantes até pronto; etapas puladas não contam como concluídas e entrega é distinta de pronto. Separar previsão temporal de contagem. Histórico/fotos só públicos; confirmação clara antes de publicar. Permissão de publicar é separada de acesso operacional; se desabilitada, evidências aguardam liberação do atendimento.
9. **RF09 — Valores e entrega:** orçamento inicial versionado, adicionais autorizados e total autorizado sem dupla contagem. Resumo de entrega restrito e compartilhável; manter revogação dos links operacionais antigos. Não reativar avaliações ocultas.
10. **RF10 — Gestão:** filtros por período; entradas/entregas, tempo nas etapas, atrasos, OS sem atualização pública, aprovações pendentes e valores autorizados. Explicar fórmula e limitações de dados históricos.
11. **RF11 — Comunicação:** estados de envio, falha e reenvio; auditar tentativas, evitar duplicidade e manter compartilhamento manual de WhatsApp. SMTP aceito não significa mensagem lida.
12. **RF12 — Arquivos e recuperação:** armazenamento privado escalável, miniaturas, cotas por oficina, migração verificável e backups/restauração de banco e arquivos. Nunca apagar automaticamente fotos existentes para aplicar plano menor.
13. **RF13 — Produção:** isolamento, gestão de segredos, HTTPS, monitoramento, alertas, trilha auditável e teste de carga com massa definida. Política de retenção/exportação deve ser explícita antes do lançamento.
14. **RF14 — Operação comercial:** onboarding guiado, importação com prévia e erros por linha, planos/cotas, testes de assinatura em ambiente de testes, cancelamento/exportação e administração restrita. Provedor e valores finais são decisões de ativação, não autorização para contratar.
15. **RF15 — Piloto:** roteiro de implantação e entrevistas, métricas, avaliação de preço/custo/suporte e decisão documentada de prosseguir, ajustar ou interromper. R$ 99/R$ 149 são hipóteses de teste, não tabela comercial publicada.

## Experiência do Usuário

Jornada principal: login → meus serviços/busca → conferir veículo → concluir etapa → capturar/revisar fotos → confirmar visibilidade e próxima etapa → confirmação do servidor.
A foto não é publicada ao ser selecionada. Antes do envio, mostrar quantidade, etapa e público de destino. Permitir fotos sem mudar etapa.
Manter marca/nome da oficina e identidade visual atual. Funcionar de 320 px a desktop, alvos de toque de 44 px, labels, foco visível, navegação por teclado, erros associados aos campos, estado não dependente apenas de cor e movimento reduzido.
Câmera e galeria com alternativa quando a permissão é negada. Testar Safari/iOS e Chrome/Android reais para captura. Reutilizar mensagens curtas; não expor CPF/contatos completos na tela técnica.
A meta de 30 segundos não justifica publicar sem confirmação nem remover controles de segurança.

## Restrições Técnicas de Alto Nível

Evolução da base Angular, Spring e PostgreSQL; preservar contas/OS existentes e isolamento multiempresa. Autorização em cada operação e arquivo. Fotos privadas por padrão de segurança; liberação pública explícita.
Múltiplas tentativas não podem gerar múltiplas conclusões. Bloqueio/revogação vale também para sessão aberta.
Serviços de nuvem, e-mail e cobrança exigem configuração autorizada e ambiente de testes. Não contratar nem enviar convites reais nesta fase.
Sem promessa jurídica de assinatura qualificada. Minimizar dados pessoais, documentar retenção e submeter documentos legais à revisão especializada antes de comercialização.

## Fora de Escopo

Estoque/financeiro/fiscal completos, aplicativo nativo, operação offline completa, WhatsApp/SMS automáticos, IA, múltiplas filiais, marketplace e migração para microserviços.
Não executar piloto com terceiros, cobrar clientes, definir preços definitivos ou provisionar infraestrutura nesta tarefa documental. Integrações com ERPs e WhatsApp oficial permanecem oportunidades futuras, dependentes de demanda e orçamento.

