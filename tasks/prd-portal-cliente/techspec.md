# Especificação técnica — Portal do cliente

Complementa [docs/techspec.md](../../docs/techspec.md) e a especificação de [acesso seguro](../prd-acesso-cliente/techspec.md).

## API

- `GET /api/portal/servico-atual?veiculoId=` retorna uma projeção explícita `{ oficina, servico }`.
- `oficina` contém somente nome, telefone e e-mail de contato.
- `servico` contém somente id, número, status, previsão, placa, descrição do veículo, pendência pública, última atualização, motivo público e próxima ação da previsão mais recente.
- `GET /api/portal/ordens-servico/{id}/atualizacoes` retorna apenas eventos publicados com texto público e sem autoria interna, em ordem cronológica estável.
- `GET /api/portal/ordens-servico/{id}/fotos` retorna somente fotos `PRONTA` e `publicada=true`, em ordem cronológica estável.
- `GET /api/portal/ordens-servico/{orderId}/fotos/{photoId}/conteudo?tamanho=original` entrega o arquivo original autorizado; sem o parâmetro, entrega a miniatura quando disponível.

## Interface

- Componente standalone Angular mantém a sessão limitada existente e carrega serviço, fotos e atualizações públicas.
- Galeria filtra localmente por etapa sem remover registros de outras etapas da resposta.
- Lightbox acessível usa `role=dialog`, fechamento por Escape, botões de pelo menos 44 px e navegação por gesto horizontal.
- Datas são apresentadas em `pt-BR`; imagens da grade usam carregamento preguiçoso.
- Erros 401 limpam o estado do portal e retornam à autenticação com mensagem de acesso expirado.

## Testes

- Unitários Angular: ordenação cronológica, filtro, retenção entre etapas, navegação e tratamento de 401.
- Integração Spring: projeção pública, contato no estado vazio, exclusão de conteúdo/fotos internas e ordenação.
- E2E: mudança de status não remove fotos publicadas anteriores.

