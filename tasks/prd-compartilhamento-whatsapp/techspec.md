# Especificação técnica — tarefa 16

## Base e dependências

Reutilizar os endpoints autenticados `POST/DELETE /api/ordens-servico/{id}/acesso` da tarefa 10 e a troca pública `POST /api/portal/acesso/link`. Preservar CSRF, isolamento por oficina, token aleatório de 256 bits, hash persistido e revalidação por requisição.

## Implementação

1. Componente Angular isolado no detalhe da OS, mantendo o visual existente. Token somente em memória. Limpar estado ao mudar OS, encerrar ou destruir componente; ignorar respostas atrasadas.
2. Link na origem atual em `/acompanhar#token=...`. Trocar por sessão e remover o token da barra antes de chamadas assíncronas. Aceitar links antigos com query token; não criar novos nesse formato. Restaurar sessão HTTP ao recarregar sem persistir token no navegador.
3. Composição pura e codificação com `encodeURIComponent` no destino fixo `https://wa.me/?text=...`. Âncora direta após preparação evita bloqueio de popup assíncrono. `noopener noreferrer` e política global `no-referrer` existente.
4. Copiar com Clipboard API; erro explícito e campo readonly selecionável. Não registrar entrega nem chamar APIs do WhatsApp automaticamente. Revogar disponível mesmo após recarregar; gerar novamente invalida anteriores.
5. Serializar criação/revogação por lock na OS para evitar dois links ativos em emissões concorrentes. Nenhuma nova tabela ou dependência.
6. Observar navegações finais/ignoradas do roteador para links recebidos na mesma aba, inclusive repetidos. Limpar dados anteriores, serializar trocas de concessão e descartar respostas de contextos antigos; cancelar a observação ao destruir o componente. A limpeza com `Location.replaceState` não dispara outra autenticação.

## Validação

- Unitários: composição/codificação, ausência de dados pessoais, sucesso/falha de cópia, erros da API, revogação, troca de OS, expiração e encerramento.
- PostgreSQL: validade, hash, escopo, reemissão, concorrência, encerramento, revogação/expiração de sessões.
- E2E desktop e mobile: preparar, abrir destino interceptado com token fictício, copiar/fallback, revogar, autenticar pelo fragmento e recarregar sem token. Não enviar mensagens reais.
- Build Angular e suítes unitárias completas; revisão independente antes do merge.

Fonte oficial consultada: [WhatsApp — click to chat](https://faq.whatsapp.com/5913398998672934), formato de mensagem sem destinatário. O envio depende da escolha e confirmação do usuário e da disponibilidade do aplicativo/site externo.

Context7 não disponível; referências primárias complementares: [ciclo de vida Angular](https://angular.dev/guide/components/lifecycle), [Clipboard.writeText](https://developer.mozilla.org/en-US/docs/Web/API/Clipboard/writeText), [locks PostgreSQL 17](https://www.postgresql.org/docs/17/explicit-locking.html).
