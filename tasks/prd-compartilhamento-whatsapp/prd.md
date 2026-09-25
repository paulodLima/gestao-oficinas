# Compartilhamento manual pelo WhatsApp

Fonte: `docs/prompt.md`, `docs/tasks.md` (16.0) e cartão https://trello.com/c/rHAC9p6Z.

O proprietário prepara uma mensagem com o link exclusivo de uma OS ativa. Ele escolhe o destinatário e confirma o envio no WhatsApp. O sistema não dispara mensagens, não confirma entrega e não integra este fluxo à outbox de e-mails.

## Aceite

- Mensagem fixa, sem CPF, nome, placa, telefone, valores ou observações internas; somente orientação e link de acompanhamento.
- Botão de abertura manual no WhatsApp e alternativa de copiar o link, com campo selecionável se a área de transferência estiver indisponível.
- Informar validade de sete dias ou encerramento, revogação e invalidação do link anterior ao gerar outro.
- Acesso restrito à OS; autorizar adicionais continua exigindo código independente.
- Funcionar em navegadores desktop e mobile, sem realizar envio real nos testes.

Fora do escopo: automação, campanhas, seleção de contatos, confirmação de entrega, WhatsApp Business API.
