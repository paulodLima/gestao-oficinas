package br.com.gestao.oficinas_api.ordem;

import br.com.gestao.oficinas_api.cadastro.PageResult;
import br.com.gestao.oficinas_api.identidade.ApiException;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ServiceOrderRepository {
    private static final String OPEN_OPERATION = "ABRIR_ORDEM_SERVICO";
    private final JdbcTemplate jdbc;
    public ServiceOrderRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public ServiceOrder order(UUID shopId, UUID id) {
        return jdbc.query(select() + " WHERE os.oficina_id=? AND os.id=?", this::map, shopId, id)
            .stream().findFirst().orElseThrow(ServiceOrderRepository::notFound);
    }

    public PageResult<ServiceOrder> orders(UUID shopId, String query, int page, int size) {
        String numberQuery = query.replaceFirst("(?i)^OS[- ]?", "");
        if (numberQuery.matches("[0-9]{1,18}")) {
            long number = Long.parseLong(numberQuery);
            String where = " WHERE os.oficina_id=? AND os.numero=?";
            long total = jdbc.queryForObject("SELECT count(*)" + joins() + where, Long.class, shopId, number);
            var items = jdbc.query(select() + where + " ORDER BY os.entrada_em DESC,os.id DESC LIMIT ? OFFSET ?",
                this::map, shopId, number, size, page * size);
            return PageResult.of(items, page, size, total);
        }
        String term = "%" + escape(query.toLowerCase(Locale.ROOT)) + "%";
        String plate = "%" + query.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT) + "%";
        String where = " WHERE os.oficina_id=? AND (cast(os.numero AS text) LIKE ? OR lower(c.nome) LIKE ? OR lower(v.placa) LIKE ?)";
        long total = jdbc.queryForObject("SELECT count(*)" + joins() + where, Long.class, shopId, term, term, plate);
        var items = jdbc.query(select() + where + " ORDER BY os.entrada_em DESC,os.id DESC LIMIT ? OFFSET ?",
            this::map, shopId, term, term, plate, size, page * size);
        return PageResult.of(items, page, size, total);
    }

    public ServiceOrder create(UUID shopId, UUID ownerId, UUID idempotencyKey, String bodyHash, CreateData data) {
        ServiceOrder replay = replay(shopId, ownerId, idempotencyKey, bodyHash);
        if (replay != null) return replay;
        UUID currentCustomer = jdbc.query("""
            SELECT l.cliente_id FROM veiculo v
              JOIN vinculo_cliente_veiculo l ON l.oficina_id=v.oficina_id
               AND l.veiculo_id=v.id AND l.fim_em IS NULL
             WHERE v.oficina_id=? AND v.id=? FOR UPDATE OF v
            """, (rs, row) -> rs.getObject("cliente_id", UUID.class), shopId, data.vehicleId())
            .stream().findFirst().orElseThrow(ServiceOrderRepository::notFound);
        if (!currentCustomer.equals(data.customerId())) {
            throw new ApiException(409, "RESPONSAVEL_DIVERGENTE", "O cliente não é mais o responsável atual pelo veículo.");
        }
        Long number = jdbc.queryForObject("""
            INSERT INTO ordem_servico_numero(oficina_id,ultimo_numero) VALUES (?,1)
            ON CONFLICT(oficina_id) DO UPDATE SET ultimo_numero=ordem_servico_numero.ultimo_numero+1
            RETURNING ultimo_numero
            """, Long.class, shopId);
        UUID id = UUID.randomUUID();
        try {
            jdbc.update("""
                INSERT INTO ordem_servico(id,oficina_id,numero,cliente_id,veiculo_id,relato_inicial,
                  entrada_em,km_entrada,previsao_em,criado_por)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """, id, shopId, number, data.customerId(), data.vehicleId(), data.report(),
                Timestamp.from(data.entryAt()), data.mileage(), timestamp(data.forecastAt()), ownerId);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(409, "OS_ATIVA_EXISTENTE", "Já existe atendimento ativo para este veículo.");
        }
        jdbc.update("INSERT INTO cadastro_auditoria(id,oficina_id,proprietario_id,recurso,recurso_id,acao) VALUES (?,?,?,?,?,?)",
            UUID.randomUUID(), shopId, ownerId, "ORDEM_SERVICO", id, "ORDEM_SERVICO_ABERTA");
        if (idempotencyKey != null) {
            jdbc.update("UPDATE requisicao_idempotente SET recurso_id=? WHERE oficina_id=? AND proprietario_id=? AND operacao=? AND chave=?",
                id, shopId, ownerId, OPEN_OPERATION, idempotencyKey);
        }
        return order(shopId, id);
    }

    private ServiceOrder replay(UUID shopId, UUID ownerId, UUID key, String bodyHash) {
        if (key == null) return null;
        jdbc.update("DELETE FROM requisicao_idempotente WHERE oficina_id=? AND proprietario_id=? AND operacao=? AND chave=? AND created_at < now() - interval '24 hours'",
            shopId, ownerId, OPEN_OPERATION, key);
        int inserted = jdbc.update("""
            INSERT INTO requisicao_idempotente(id,oficina_id,proprietario_id,operacao,chave,hash_corpo)
            VALUES (?,?,?,?,?,?) ON CONFLICT(oficina_id,proprietario_id,operacao,chave) DO NOTHING
            """, UUID.randomUUID(), shopId, ownerId, OPEN_OPERATION, key, bodyHash);
        if (inserted == 1) return null;
        IdempotentRequest request = jdbc.query("""
            SELECT hash_corpo,recurso_id FROM requisicao_idempotente
             WHERE oficina_id=? AND proprietario_id=? AND operacao=? AND chave=?
            """, (rs, row) -> new IdempotentRequest(rs.getString("hash_corpo"),
                rs.getObject("recurso_id", UUID.class)), shopId, ownerId, OPEN_OPERATION, key).getFirst();
        if (!request.bodyHash().equals(bodyHash)) {
            throw new ApiException(409, "CHAVE_IDEMPOTENCIA_REUTILIZADA", "A chave de idempotência já foi usada com outros dados.");
        }
        if (request.resourceId() == null) {
            throw new ApiException(409, "REQUISICAO_EM_PROCESSAMENTO", "A abertura com esta chave ainda está em processamento.");
        }
        return order(shopId, request.resourceId());
    }

    private String select() {
        return "SELECT os.id,os.numero,os.cliente_id,c.nome cliente_nome,os.veiculo_id,v.placa," +
            "concat_ws(' ',v.marca,v.modelo) veiculo,os.relato_inicial,os.entrada_em,os.km_entrada," +
            "os.status,os.previsao_em,os.versao,os.created_at" + joins();
    }
    private String joins() {
        return " FROM ordem_servico os JOIN cliente c ON c.oficina_id=os.oficina_id AND c.id=os.cliente_id" +
            " JOIN veiculo v ON v.oficina_id=os.oficina_id AND v.id=os.veiculo_id";
    }
    private ServiceOrder map(ResultSet rs, int row) throws SQLException {
        return new ServiceOrder(rs.getObject("id", UUID.class), rs.getLong("numero"),
            rs.getObject("cliente_id", UUID.class), rs.getString("cliente_nome"),
            rs.getObject("veiculo_id", UUID.class), rs.getString("placa"), rs.getString("veiculo"),
            rs.getString("relato_inicial"), rs.getTimestamp("entrada_em").toInstant(), rs.getInt("km_entrada"),
            ServiceOrderStatus.valueOf(rs.getString("status")), instant(rs.getTimestamp("previsao_em")),
            rs.getLong("versao"), rs.getTimestamp("created_at").toInstant());
    }
    private static String escape(String value) { return value.replace("%", "").replace("_", ""); }
    private static Timestamp timestamp(Instant value) { return value == null ? null : Timestamp.from(value); }
    private static Instant instant(Timestamp value) { return value == null ? null : value.toInstant(); }
    private static ApiException notFound() {
        return new ApiException(404, "ORDEM_SERVICO_NAO_ENCONTRADA", "Ordem de serviço não encontrada.");
    }
    public record CreateData(UUID customerId, UUID vehicleId, String report, Instant entryAt,
                             int mileage, Instant forecastAt) {}
    private record IdempotentRequest(String bodyHash, UUID resourceId) {}
}
