package br.com.gestao.oficinas_api.adicional;

import br.com.gestao.oficinas_api.identidade.ApiException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdditionalRequestRepository {
    private final JdbcTemplate jdbc;

    public AdditionalRequestRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AdditionalRequest> list(UUID shop, UUID order) {
        return jdbc.query("""
            SELECT * FROM solicitacao_adicional
             WHERE oficina_id=? AND ordem_servico_id=? ORDER BY created_at DESC,id DESC
            """, (result, row) -> mapRequest(result, shop, order), shop, order);
    }

    public AdditionalRequest request(UUID shop, UUID order, UUID id) {
        return jdbc.query("""
            SELECT * FROM solicitacao_adicional
             WHERE oficina_id=? AND ordem_servico_id=? AND id=?
            """, (result, row) -> mapRequest(result, shop, order), shop, order, id).stream().findFirst()
            .orElseThrow(() -> new ApiException(404, "ADICIONAL_NAO_ENCONTRADO",
                "Solicitação adicional não encontrada."));
    }

    public AdditionalRequest create(UUID shop, UUID owner, UUID order, DraftData data) {
        UUID requestId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO solicitacao_adicional(id,oficina_id,ordem_servico_id,criado_por)
            VALUES (?,?,?,?)
            """, requestId, shop, order, owner);
        insertVersion(versionId, requestId, shop, order, 1, data);
        audit(shop, owner, requestId, "ADICIONAL_RASCUNHO_CRIADO");
        return request(shop, order, requestId);
    }

    public AdditionalRequest edit(UUID shop, UUID owner, UUID order, UUID id,
                                  long expectedVersion, UUID versionId, DraftData data) {
        lockRequest(shop, order, id, expectedVersion, "RASCUNHO");
        jdbc.update("DELETE FROM adicional_versao_foto WHERE versao_id=?", versionId);
        jdbc.update("DELETE FROM adicional_item WHERE versao_id=?", versionId);
        int changed = jdbc.update("""
            UPDATE adicional_versao SET problema=?,justificativa=?,previsao_proposta=?,
                   impacto_prazo=?,updated_at=now()
             WHERE id=? AND solicitacao_id=? AND estado='RASCUNHO'
            """, data.problem(), data.justification(), timestamp(data.proposedForecast()),
            data.deadlineImpact(), versionId, id);
        if (changed != 1) throw conflict("A versão enviada não pode ser alterada.");
        insertChildren(versionId, data);
        audit(shop, owner, id, "ADICIONAL_RASCUNHO_EDITADO");
        return request(shop, order, id);
    }

    public AdditionalRequest send(UUID shop, UUID owner, UUID order, UUID id,
                                  long expectedVersion, UUID versionId) {
        lockRequest(shop, order, id, expectedVersion, "RASCUNHO");
        int changed = jdbc.update("""
            UPDATE adicional_versao SET estado='ENVIADA',enviada_em=now(),updated_at=now()
             WHERE id=? AND solicitacao_id=? AND estado='RASCUNHO'
            """, versionId, id);
        if (changed != 1) throw conflict("A versão atual já foi enviada.");
        jdbc.update("UPDATE solicitacao_adicional SET estado='ENVIADA' WHERE id=?", id);
        audit(shop, owner, id, "ADICIONAL_ENVIADO");
        return request(shop, order, id);
    }

    public AdditionalRequest replace(UUID shop, UUID owner, UUID order, UUID id,
                                     long expectedVersion, AdditionalRequest.Version current,
                                     String reason, DraftData data) {
        lockRequest(shop, order, id, expectedVersion, "ENVIADA");
        int changed = jdbc.update("""
            UPDATE adicional_versao SET estado='SUBSTITUIDA',motivo_substituicao=?,
                   substituida_em=now(),updated_at=now()
             WHERE id=? AND solicitacao_id=? AND estado='ENVIADA'
            """, reason, current.id(), id);
        if (changed != 1) throw conflict("A versão atual não pode mais ser substituída.");
        UUID versionId = UUID.randomUUID();
        insertVersion(versionId, id, shop, order, current.numero() + 1, data);
        jdbc.update("UPDATE solicitacao_adicional SET estado='RASCUNHO' WHERE id=?", id);
        audit(shop, owner, id, "ADICIONAL_VERSAO_SUBSTITUIDA");
        return request(shop, order, id);
    }

    public AdditionalRequest cancel(UUID shop, UUID owner, UUID order, UUID id,
                                    long expectedVersion, String reason) {
        int changed = jdbc.update("""
            UPDATE solicitacao_adicional
               SET estado='CANCELADA',motivo_cancelamento=?,versao=versao+1,updated_at=now()
             WHERE oficina_id=? AND ordem_servico_id=? AND id=? AND versao=?
               AND estado NOT IN ('CANCELADA','DECIDIDA')
            """, reason, shop, order, id, expectedVersion);
        if (changed != 1) throw conflict("A solicitação foi alterada ou não pode ser cancelada.");
        audit(shop, owner, id, "ADICIONAL_CANCELADO");
        return request(shop, order, id);
    }

    public void validatePhotos(UUID shop, UUID order, List<UUID> photoIds, boolean published) {
        for (UUID photoId : photoIds) {
            String publication = published ? " AND publicada=true" : "";
            Integer count = jdbc.queryForObject("""
                SELECT count(*) FROM ordem_servico_foto
                 WHERE oficina_id=? AND ordem_servico_id=? AND id=? AND estado='PRONTA'
                """ + publication, Integer.class, shop, order, photoId);
            if (count == null || count != 1) {
                String detail = published
                    ? "Publique todas as fotos selecionadas antes do envio."
                    : "Uma das fotos não pertence a esta ordem de serviço.";
                throw new ApiException(400, "FOTO_ADICIONAL_INVALIDA", detail);
            }
        }
    }

    private void insertVersion(UUID versionId, UUID requestId, UUID shop, UUID order,
                               int number, DraftData data) {
        jdbc.update("""
            INSERT INTO adicional_versao(id,solicitacao_id,oficina_id,ordem_servico_id,numero,
              problema,justificativa,previsao_proposta,impacto_prazo)
            VALUES (?,?,?,?,?,?,?,?,?)
            """, versionId, requestId, shop, order, number, data.problem(), data.justification(),
            timestamp(data.proposedForecast()), data.deadlineImpact());
        insertChildren(versionId, data);
    }

    private void insertChildren(UUID versionId, DraftData data) {
        for (int index = 0; index < data.items().size(); index++) {
            ItemData item = data.items().get(index);
            jdbc.update("""
                INSERT INTO adicional_item(id,versao_id,tipo,descricao,quantidade,valor_unitario,
                  total,grupo_dependencia,ordem) VALUES (?,?,?,?,?,?,?,?,?)
                """, UUID.randomUUID(), versionId, item.type().name(), item.description(),
                item.quantity(), item.unitValue(), item.total(), item.dependencyGroup(), index);
        }
        for (int index = 0; index < data.photoIds().size(); index++) {
            jdbc.update("INSERT INTO adicional_versao_foto(versao_id,foto_id,ordem) VALUES (?,?,?)",
                versionId, data.photoIds().get(index), index);
        }
    }

    private AdditionalRequest mapRequest(ResultSet result, UUID shop, UUID order) throws SQLException {
        UUID id = result.getObject("id", UUID.class);
        return new AdditionalRequest(id, order,
            AdditionalRequest.Status.valueOf(result.getString("estado")), result.getLong("versao"),
            result.getString("motivo_cancelamento"), versions(id),
            result.getTimestamp("created_at").toInstant(), result.getTimestamp("updated_at").toInstant());
    }

    private List<AdditionalRequest.Version> versions(UUID requestId) {
        return jdbc.query("""
            SELECT * FROM adicional_versao WHERE solicitacao_id=? ORDER BY numero ASC
            """, (result, row) -> mapVersion(result, requestId), requestId);
    }

    private AdditionalRequest.Version mapVersion(ResultSet result, UUID requestId) throws SQLException {
        UUID id = result.getObject("id", UUID.class);
        List<AdditionalRequest.Item> items = items(id);
        BigDecimal total = items.stream().map(AdditionalRequest.Item::total)
            .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        return new AdditionalRequest.Version(id, result.getInt("numero"),
            AdditionalRequest.VersionStatus.valueOf(result.getString("estado")),
            result.getString("problema"), result.getString("justificativa"),
            instant(result.getTimestamp("previsao_proposta")), result.getString("impacto_prazo"),
            result.getString("motivo_substituicao"), total, photos(id), items,
            instant(result.getTimestamp("enviada_em")), instant(result.getTimestamp("substituida_em")),
            result.getTimestamp("created_at").toInstant(), result.getTimestamp("updated_at").toInstant());
    }

    private List<AdditionalRequest.Item> items(UUID versionId) {
        return jdbc.query("""
            SELECT * FROM adicional_item WHERE versao_id=? ORDER BY ordem ASC
            """, (result, row) -> new AdditionalRequest.Item(
                result.getObject("id", UUID.class),
                AdditionalRequest.ItemType.valueOf(result.getString("tipo")),
                result.getString("descricao"), result.getBigDecimal("quantidade"),
                result.getBigDecimal("valor_unitario"), result.getBigDecimal("total"),
                result.getString("grupo_dependencia"), result.getInt("ordem")), versionId);
    }

    private List<UUID> photos(UUID versionId) {
        return jdbc.query("""
            SELECT foto_id FROM adicional_versao_foto WHERE versao_id=? ORDER BY ordem ASC
            """, (result, row) -> result.getObject(1, UUID.class), versionId);
    }

    private void lockRequest(UUID shop, UUID order, UUID id, long expectedVersion, String state) {
        int changed = jdbc.update("""
            UPDATE solicitacao_adicional SET versao=versao+1,updated_at=now()
             WHERE oficina_id=? AND ordem_servico_id=? AND id=? AND versao=? AND estado=?
            """, shop, order, id, expectedVersion, state);
        if (changed != 1) throw conflict("A solicitação foi alterada. Atualize a página e tente novamente.");
    }

    private void audit(UUID shop, UUID owner, UUID request, String action) {
        jdbc.update("""
            INSERT INTO cadastro_auditoria(id,oficina_id,proprietario_id,recurso,recurso_id,acao)
            VALUES (?,?,?,?,?,?)
            """, UUID.randomUUID(), shop, owner, "SOLICITACAO_ADICIONAL", request, action);
    }

    private Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private ApiException conflict(String detail) {
        return new ApiException(409, "CONFLITO_DE_VERSAO", detail);
    }

    public record DraftData(String problem, String justification, Instant proposedForecast,
                            String deadlineImpact, List<UUID> photoIds, List<ItemData> items) {}
    public record ItemData(AdditionalRequest.ItemType type, String description, BigDecimal quantity,
                           BigDecimal unitValue, BigDecimal total, String dependencyGroup) {}
}
