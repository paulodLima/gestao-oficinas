package br.com.gestao.oficinas_api.notificacoes;

/** Only explicit commercial events can queue mail; photos and private notes cannot. */
public enum NotificationEvent {
    ORDEM_ABERTA("Ordem de serviço aberta", "A abertura do serviço foi registrada."),
    ADICIONAL_ENVIADO("Adicional disponível", "Há uma solicitação adicional disponível para sua análise."),
    DECISAO_REGISTRADA("Decisão registrada", "Uma decisão sobre os serviços adicionais foi registrada."),
    PREVISAO_ALTERADA("Previsão atualizada", "A previsão de conclusão foi atualizada."),
    PRONTO_PARA_RETIRADA("Veículo pronto", "Seu veículo está pronto para retirada."),
    ORDEM_ENCERRADA("Serviço encerrado", "O encerramento do serviço foi registrado."),
    AVALIACAO_SOLICITADA("Avalie o atendimento", "Sua avaliação do atendimento está disponível.");

    private final String title;
    private final String message;
    NotificationEvent(String title, String message) { this.title = title; this.message = message; }
    public String title() { return title; }
    public String message(long number) { return "OS-" + number + " · " + message; }
}
