# PRD — Portal do cliente e galeria de todas as etapas

O cliente autenticado acompanha somente o serviço ativo do veículo autorizado. A página identifica oficina e veículo, apresenta status, previsão estimada, pendências e última atualização e, logo abaixo, reúne a linha do tempo e todas as fotos publicadas da ordem de serviço.

## Requisitos funcionais

- Mostrar somente a OS ativa vinculada à oficina, ao cliente e ao veículo autorizados.
- Exibir o contato público da oficina mesmo quando não houver OS ativa.
- Apresentar status atual sem porcentagem ou progresso artificial.
- Mostrar previsão como estimativa, pendência pública e última atualização disponível.
- Reunir todas as fotos publicadas da OS em ordem cronológica, inclusive de etapas anteriores.
- Identificar etapa, legenda e data; permitir filtro por etapa, ampliação e navegação anterior/próxima por toque ou deslize.
- Exibir a linha do tempo pública em ordem cronológica.
- Tratar explicitamente ausência de serviço e expiração da sessão.

## Regras de segurança

- Nunca retornar CPF, contato do cliente, observação interna, motivo interno, autoria interna ou fotos privadas/removidas.
- Revalidar oficina, cliente, veículo e OS em todas as consultas e downloads.
- Links exclusivos continuam limitados à OS autorizada e expiram ou deixam de funcionar após revogação/encerramento.
- Não exibir histórico completo de atendimentos encerrados nem criar módulo de comparação antes/depois.

## Critérios de aceite

- Fotos publicadas de etapas anteriores continuam disponíveis após mudança de status.
- Galeria possui filtro, zoom, botões anterior/próximo e gesto horizontal.
- Respostas públicas não contêm campos internos.
- Estado sem OS informa “Nenhum serviço em andamento” e apresenta contato da oficina.
- Acesso expirado volta ao fluxo de autenticação com mensagem clara.

