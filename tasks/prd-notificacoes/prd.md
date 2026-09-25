# Notificações transacionais — tarefa 15

Fonte: `docs/prompt.md`, `docs/tasks.md` (15) e cartão Ygx2Pt8V.

A oficina precisa acompanhar eventos relevantes sem perder a operação quando o
provedor de e-mail estiver indisponível. A central privada mostra abertura de OS,
envio de adicional, decisão do cliente e alteração de previsão. E-mails comerciais
são enviados somente ao cliente responsável com contato verificado e ativo.

Aceite: avisos persistentes, leitura, paginação, isolamento entre oficinas,
deduplicação, falha visível, retentativas limitadas e reenvio manual. E-mail não
contém observações internas, fotos, códigos ou links que concedam acesso. Alterações
menores/fotos não disparam e-mail. Entrega SMTP não equivale a leitura.

Pronto, encerramento e avaliação possuem contratos tipados; seus gatilhos e testes
de negócio pertencem às tarefas 17/18. Códigos e recuperação mantêm os fluxos seguros
existentes e reutilizam o mesmo transporte; não persistir segredos em texto claro.
