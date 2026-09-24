# Especificação técnica — Serviços adicionais

Complementa [docs/techspec.md](../../docs/techspec.md), especialmente as seções 3, 5 e 6.

- Persistir solicitação, versão e itens com `numeric(15,2)` para valores e `numeric(12,3)` para quantidades.
- Total do item é quantidade × valor unitário, arredondado em duas casas; total da versão é a soma dos itens.
- Fotos referenciadas devem pertencer à mesma oficina/OS e estar publicadas antes do envio.
- Escritas exigem sessão do proprietário, CSRF, OS ativa e versão esperada; mudanças, envios e substituições são auditados na mesma transação.
- Endpoints: criar/listar, editar rascunho, enviar, substituir e cancelar sob `/api/ordens-servico/{id}/adicionais`.
