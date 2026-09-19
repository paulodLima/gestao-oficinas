package br.com.gestao.oficinas_api.ordem;

public enum ServiceOrderStatus {
    RECEBIDO, EM_DIAGNOSTICO, AGUARDANDO_APROVACAO, AGUARDANDO_PECAS,
    EM_MANUTENCAO, EM_MONTAGEM, EM_TESTES, PRONTO_PARA_RETIRADA,
    ENTREGUE, CANCELADO, FUNILARIA, PINTURA;

    public boolean active() {
        return this != ENTREGUE && this != CANCELADO;
    }
}
