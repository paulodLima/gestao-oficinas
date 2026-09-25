package br.com.gestao.oficinas_api.adicional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdditionalRequest(UUID id, UUID ordemServicoId, Status estado, long versao,
                                String motivoCancelamento, List<Version> versoes,
                                Instant createdAt, Instant updatedAt) {
    public enum Status { RASCUNHO, ENVIADA, PARCIALMENTE_DECIDIDA, DECIDIDA, CANCELADA }
    public enum VersionStatus { RASCUNHO, ENVIADA, SUBSTITUIDA }
    public enum ItemType { PECA, MAO_DE_OBRA }

    public record Version(UUID id, int numero, VersionStatus estado, String problema,
                          String justificativa, Instant previsaoProposta, String impactoPrazo,
                          String motivoSubstituicao, BigDecimal total, List<UUID> fotoIds,
                          List<Item> itens, Instant enviadaEm, Instant substituidaEm,
                          Instant createdAt, Instant updatedAt) {}

    public record Item(UUID id, ItemType tipo, String descricao, BigDecimal quantidade,
                       BigDecimal valorUnitario, BigDecimal total, String grupoDependencia,
                       int ordem) {}
}
