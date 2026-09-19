package br.com.gestao.oficinas_api.ordem;

public enum ServiceOrderStatus {
    RECEBIDO(0, false),
    EM_DIAGNOSTICO(10, false),
    AGUARDANDO_APROVACAO(20, true),
    AGUARDANDO_PECAS(30, true),
    EM_MANUTENCAO(40, false),
    FUNILARIA(42, false),
    PINTURA(44, false),
    EM_MONTAGEM(50, false),
    EM_TESTES(60, false),
    PRONTO_PARA_RETIRADA(70, false),
    ENTREGUE(100, false),
    CANCELADO(100, false);

    private final int sequence;
    private final boolean waiting;

    ServiceOrderStatus(int sequence, boolean waiting) {
        this.sequence = sequence;
        this.waiting = waiting;
    }

    public boolean active() {
        return this != ENTREGUE && this != CANCELADO;
    }

    public boolean requiresReasonFrom(ServiceOrderStatus current) {
        return sequence < current.sequence || waiting && current.executing();
    }

    private boolean executing() {
        return active() && !waiting && this != RECEBIDO && this != PRONTO_PARA_RETIRADA;
    }
}
