# Especificação técnica

- Spring Boot/JDBC/PostgreSQL e Angular existentes; migração V15 aditiva.
- GET/POST /api/ordens-servico/{id}/encerramento, somente proprietário da oficina. POST recebe tipo ENTREGUE/CANCELADO, confirmado=true, expectedVersion, cancelarPendencias e motivo opcional (obrigatório no cancelamento).
- Transação única: bloquear OS, validar versão/estado, contar e cancelar pendências autorizadas, gravar encerramento e evento de status/auditoria, revogar links/códigos específicos e registrar notificação. Histórico permanece interno e íntegro.
- Ordem de bloqueios: OS antes de solicitação/versão/itens/desafio. Todas as escritas de adicionais, fotos e vistorias usam a mesma trava da OS. Escritas de status/previsão já fazem UPDATE condicional à OS ativa.
- Não revogar desafios/sessões de identidade global do cliente: autorização é revalidada contra vínculo e OS atual em cada acesso. Grants de link exclusivo permanecem presos à OS original e são revogados.
- Nova visita usa o endpoint existente de criação de OS, consulta veículo e responsável atual por ID (independente da paginação), preenche apenas esse vínculo e solicita relato/quilometragem novos. 404 ou responsável inativo preservam a OS selecionada com mensagem. Restrição única de OS ativa permanece.
- Interface mostra confirmação em duas etapas, impacto e opção explícita de cancelar adicionais pendentes; histórico continua visível com ações operacionais desabilitadas.
- Testes: política unitária, integração HTTP/PostgreSQL real incluindo concorrência, sessões e isolamento; Angular e E2E desktop/mobile com API simulada; revisão independente antes do merge.

Referência de concorrência: https://www.postgresql.org/docs/17/explicit-locking.html (bloqueios duram até o fim da transação; leitura bloqueante reavalia a linha atualizada).
