package br.com.gestao.oficinas_api.ordem;

import br.com.gestao.oficinas_api.identidade.ApiException;
import br.com.gestao.oficinas_api.identidade.Identidade;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class InspectionService {
    private final JdbcTemplate jdbc;
    private final ServiceOrderRepository orders;
    private final ServicePhotoRepository photos;
    private final ObjectMapper json;

    public InspectionService(JdbcTemplate jdbc, ServiceOrderRepository orders,
                             ServicePhotoRepository photos, ObjectMapper json) {
        this.jdbc = jdbc;
        this.orders = orders;
        this.photos = photos;
        this.json = json;
    }

    public List<Inspection> list(Identidade owner, UUID order) {
        orders.order(owner.oficinaId(), order);
        return jdbc.query("SELECT * FROM ordem_servico_vistoria WHERE oficina_id=? AND ordem_servico_id=? ORDER BY numero_versao DESC",
            this::map, owner.oficinaId(), order);
    }

    @Transactional
    public Inspection draft(Identidade owner, UUID order, Map<String, Object> checklist) {
        active(owner, order);
        InspectionChecklistPolicy.validate(checklist);
        validatePhotos(owner.oficinaId(), order, checklist);
        var found = jdbc.query("SELECT id FROM ordem_servico_vistoria WHERE oficina_id=? AND ordem_servico_id=? AND estado='RASCUNHO' FOR UPDATE",
            (result, row) -> result.getObject(1, UUID.class), owner.oficinaId(), order);
        String body = write(checklist);
        UUID id;
        if (!found.isEmpty()) {
            id = found.getFirst();
            int changed = jdbc.update("UPDATE ordem_servico_vistoria SET checklist=?::jsonb,updated_at=now() WHERE id=? AND estado='RASCUNHO'", body, id);
            if (changed != 1) throw staleInspection();
        } else {
            Integer confirmed = jdbc.queryForObject("SELECT count(*) FROM ordem_servico_vistoria WHERE oficina_id=? AND ordem_servico_id=? AND estado='CONFIRMADA'",
                Integer.class, owner.oficinaId(), order);
            if (confirmed != null && confirmed > 0) {
                throw new ApiException(409, "VISTORIA_CONFIRMADA",
                    "A vistoria confirmada é imutável. Registre uma correção com motivo.");
            }
            id = UUID.randomUUID();
            Integer next = jdbc.queryForObject("SELECT coalesce(max(numero_versao),0)+1 FROM ordem_servico_vistoria WHERE oficina_id=? AND ordem_servico_id=?",
                Integer.class, owner.oficinaId(), order);
            jdbc.update("INSERT INTO ordem_servico_vistoria(id,oficina_id,ordem_servico_id,numero_versao,estado,checklist,criado_por) VALUES (?,?,?,?, 'RASCUNHO',?::jsonb,?)",
                id, owner.oficinaId(), order, next, body, owner.id());
        }
        audit(owner, order, "VISTORIA_RASCUNHO_SALVO");
        return find(owner, order, id);
    }

    @Transactional
    public Inspection confirm(Identidade owner, UUID order, long expectedVersion) {
        active(owner, order);
        lockOrder(owner, order, expectedVersion);
        int changed = jdbc.update("UPDATE ordem_servico_vistoria SET estado='CONFIRMADA',updated_at=now() WHERE oficina_id=? AND ordem_servico_id=? AND estado='RASCUNHO'",
            owner.oficinaId(), order);
        if (changed != 1) {
            throw new ApiException(409, "VISTORIA_SEM_RASCUNHO", "Salve a vistoria antes de confirmar.");
        }
        audit(owner, order, "VISTORIA_CONFIRMADA");
        return latestConfirmed(owner, order);
    }

    @Transactional
    public Inspection correct(Identidade owner, UUID order, long expectedVersion,
                              String reason, Map<String, Object> checklist) {
        active(owner, order);
        InspectionChecklistPolicy.validateCorrectionReason(reason);
        InspectionChecklistPolicy.validate(checklist);
        validatePhotos(owner.oficinaId(), order, checklist);
        lockOrder(owner, order, expectedVersion);
        Inspection previous = latestConfirmed(owner, order);
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO ordem_servico_vistoria(id,oficina_id,ordem_servico_id,numero_versao,estado,checklist,motivo_correcao,versao_anterior_id,criado_por) VALUES (?,?,?,?, 'CONFIRMADA',?::jsonb,?,?,?)",
            id, owner.oficinaId(), order, previous.numeroVersao() + 1, write(checklist),
            reason.trim(), previous.id(), owner.id());
        audit(owner, order, "VISTORIA_CORRIGIDA");
        return find(owner, order, id);
    }

    private Inspection latestConfirmed(Identidade owner, UUID order) {
        return jdbc.query("SELECT * FROM ordem_servico_vistoria WHERE oficina_id=? AND ordem_servico_id=? AND estado='CONFIRMADA' ORDER BY numero_versao DESC LIMIT 1",
            this::map, owner.oficinaId(), order).stream().findFirst().orElseThrow(() ->
            new ApiException(409, "VISTORIA_NAO_CONFIRMADA", "Ainda não há uma vistoria confirmada."));
    }

    private void active(Identidade owner, UUID order) {
        if (!orders.lockActive(owner.oficinaId(), order).status().active()) {
            throw new ApiException(409, "ORDEM_ENCERRADA", "A ordem de serviço já foi encerrada.");
        }
    }

    private void lockOrder(Identidade owner, UUID order, long expected) {
        int changed = jdbc.update("UPDATE ordem_servico SET versao=versao+1,updated_at=now() WHERE oficina_id=? AND id=? AND versao=? AND encerrada_em IS NULL",
            owner.oficinaId(), order, expected);
        if (changed != 1) {
            throw new ApiException(409, "ORDEM_DESATUALIZADA", "A ordem mudou. Recarregue antes de continuar.");
        }
    }

    private void validatePhotos(UUID shop, UUID order, Map<String, Object> checklist) {
        Object raw = checklist.get("fotos");
        if (!(raw instanceof Map<?, ?> references)) return;
        for (Object value : references.values()) {
            if (value == null || value.toString().isBlank()) continue;
            try {
                photos.find(shop, order, UUID.fromString(value.toString()));
            } catch (IllegalArgumentException exception) {
                throw new ApiException(400, "DADOS_INVALIDOS", "Referência de foto inválida.");
            }
        }
    }

    private Inspection find(Identidade owner, UUID order, UUID id) {
        return jdbc.query("SELECT * FROM ordem_servico_vistoria WHERE oficina_id=? AND ordem_servico_id=? AND id=?",
            this::map, owner.oficinaId(), order, id).getFirst();
    }

    private Inspection map(ResultSet result, int row) throws java.sql.SQLException {
        try {
            return new Inspection(result.getObject("id", UUID.class), result.getInt("numero_versao"),
                result.getString("estado"), json.readValue(result.getString("checklist"), new TypeReference<>() {}),
                result.getString("motivo_correcao"), result.getTimestamp("created_at").toInstant(),
                result.getTimestamp("updated_at").toInstant());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String write(Map<String, Object> data) {
        try {
            return json.writeValueAsString(data);
        } catch (Exception exception) {
            throw new ApiException(400, "DADOS_INVALIDOS", "Checklist inválido.");
        }
    }

    private void audit(Identidade owner, UUID order, String action) {
        jdbc.update("INSERT INTO cadastro_auditoria(id,oficina_id,proprietario_id,recurso,recurso_id,acao) VALUES (?,?,?,?,?,?)",
            UUID.randomUUID(), owner.oficinaId(), owner.id(), "ORDEM_SERVICO", order, action);
    }

    private ApiException staleInspection() {
        return new ApiException(409, "VISTORIA_DESATUALIZADA",
            "A vistoria foi confirmada. Recarregue antes de editar.");
    }
}
