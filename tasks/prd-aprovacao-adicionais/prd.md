# PRD — Aprovação e recusa de serviços adicionais

## Visão Geral

Permitir que o cliente decida, no portal autenticado, sobre serviços adicionais enviados pela oficina. A decisão deve apresentar exatamente a versão, os itens, os valores e o impacto no prazo que serão aceitos ou recusados. Itens independentes podem ser decididos separadamente; itens ligados pelo mesmo grupo são indivisíveis.

O link exclusivo da OS continua sendo suficiente para acompanhamento, mas não autoriza despesas. Toda decisão exige um código temporário enviado ao e-mail verificado do cliente responsável.

## Objetivos

- Permitir decisão parcial ou completa sem considerar silêncio como autorização.
- Preservar identidade, horário, comentário e versão exata de cada decisão.
- Tornar uma confirmação repetida segura e sem decisões duplicadas.
- Impedir decisões sobre versão substituída, solicitação cancelada, OS encerrada ou atendimento de outro cliente.
- Exibir no portal o estado atualizado da solicitação imediatamente após a confirmação.

## Histórias de Usuário

- Como cliente, quero revisar itens, total adicional e impacto no prazo antes de confirmar para compreender o compromisso assumido.
- Como cliente, quero aprovar ou recusar um item independente sem ser obrigado a decidir os demais.
- Como cliente, quero decidir um grupo dependente como uma unidade para não autorizar uma execução tecnicamente incompleta.
- Como oficina, quero um registro auditável da decisão para saber quais itens podem seguir para execução.
- Como cliente em um link de acompanhamento, quero receber um código no contato verificado antes de autorizar qualquer valor.

## Funcionalidades Principais

1. Listar no portal somente solicitações enviadas da OS autorizada, com versão, itens, grupos, totais, fotos públicas, previsão proposta, impacto no prazo e decisões já registradas.
2. Representar cada item independente como um bloco de decisão e cada grupo dependente como um único bloco indivisível.
3. Permitir selecionar aprovação ou recusa por bloco ainda pendente, com comentário opcional de até 1.000 caracteres.
4. Exibir um resumo final com decisões, total aprovado e impacto no prazo antes da confirmação.
5. Emitir código numérico temporário, de uso único e tentativas limitadas, exclusivamente para o e-mail verificado do cliente responsável pela OS.
6. Exigir o código e uma chave idempotente ao confirmar decisões.
7. Registrar identidade do cliente, decisão, data, versão e itens abrangidos em uma operação atômica.
8. Recalcular a solicitação como `ENVIADA`, `PARCIALMENTE_DECIDIDA` ou `DECIDIDA` conforme a cobertura dos itens.
9. Preservar decisões de versões substituídas no histórico, bloqueando novas decisões nelas.
10. Considerar apto à execução somente item aprovado na versão vigente; item pendente ou recusado permanece bloqueado.

## Experiência do Usuário

O portal mostra uma seção “Autorização necessária” após o resumo da OS. Cada bloco apresenta descrições, quantidades, valores e total; grupos dependentes recebem identificação visual e uma única escolha. O cliente pode revisar a seleção, solicitar o código e confirmar em uma etapa explícita.

Estados de carregamento, código enviado, código inválido/expirado, decisão parcial, decisão concluída, versão substituída e indisponibilidade devem ser claros. Controles terão rótulos, foco visível, alvos mínimos de toque e funcionamento a partir de 320 px, sem depender apenas de cor.

## Restrições Técnicas de Alto Nível

- Reutilizar sessão e autorização do portal, revalidando oficina, cliente, veículo e OS em todas as operações.
- O código é uma autorização transacional e deve estar vinculado à solicitação e versão exibidas.
- Códigos e tokens nunca são persistidos em texto puro.
- Decisões são imutáveis, auditáveis e idempotentes.
- Dados internos, CPF e contato completo não aparecem no payload público.
- A confirmação é um aceite operacional; a interface não promete assinatura eletrônica qualificada.

## Fora de Escopo

- Central de notificações, retentativas e avisos gerais da oficina, tratados na Tarefa 15.
- Compartilhamento pelo WhatsApp, tratado na Tarefa 16.
- Encerramento concorrente definitivo da OS, complementado na Tarefa 17.
- Início ou apontamento de execução mecânica; esta tarefa apenas fornece a decisão autorizadora.
