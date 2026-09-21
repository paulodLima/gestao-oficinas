package br.com.gestao.oficinas_api.ordem;

import br.com.gestao.oficinas_api.cadastro.PageResult;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardRepository {
    private static final String LATE = "os.previsao_em < ? AND os.status NOT IN ('PRONTO_PARA_RETIRADA','ENTREGUE','CANCELADO')";
    private static final String STAGE = "coalesce((SELECT max(e.created_at) FROM ordem_servico_evento e WHERE e.oficina_id=os.oficina_id"
        + " AND e.ordem_servico_id=os.id AND e.tipo='STATUS'),os.created_at)";
    private static final String JOINS = " FROM ordem_servico os JOIN cliente c ON c.oficina_id=os.oficina_id AND c.id=os.cliente_id"
        + " JOIN veiculo v ON v.oficina_id=os.oficina_id AND v.id=os.veiculo_id";
    private final JdbcTemplate jdbc;
    public DashboardRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Indicators indicators(UUID shopId, Instant now) {
        return jdbc.queryForObject("SELECT count(*) ativas, count(*) FILTER (WHERE " + LATE + ") atrasadas,"
            + " count(*) FILTER (WHERE os.status='AGUARDANDO_APROVACAO') aprovacoes,"
            + " count(*) FILTER (WHERE os.status='AGUARDANDO_PECAS') pecas,"
            + " count(*) FILTER (WHERE os.status='PRONTO_PARA_RETIRADA') prontas"
            + " FROM ordem_servico os WHERE os.oficina_id=? AND os.encerrada_em IS NULL",
            (row, index) -> new Indicators(row.getLong("ativas"), row.getLong("atrasadas"), row.getLong("aprovacoes"),
                row.getLong("pecas"), row.getLong("prontas")), Timestamp.from(now), shopId);
    }
    public PageResult<Card> cards(UUID shopId, Filter filter, Instant now) {
        var parameters = new ArrayList<Object>();
        parameters.add(shopId);
        StringBuilder where = new StringBuilder(" WHERE os.oficina_id=? AND os.encerrada_em IS NULL");
        addSearch(where, parameters, filter.query());
        if (filter.staleHours() > 0) {
            where.append(" AND os.updated_at<=?"); parameters.add(Timestamp.from(now.minus(Duration.ofHours(filter.staleHours()))));
        }
        if (filter.stageHours() > 0) {
            where.append(" AND " + STAGE + "<=?"); parameters.add(Timestamp.from(now.minus(Duration.ofHours(filter.stageHours()))));
        }
        if (filter.status() != null) { where.append(" AND os.status=?"); parameters.add(filter.status().name()); }
        switch (filter.situation()) {
            case ATRASADAS -> { where.append(" AND " + LATE); parameters.add(Timestamp.from(now)); }
            case APROVACAO -> where.append(" AND os.status='AGUARDANDO_APROVACAO'");
            case PECAS -> where.append(" AND os.status='AGUARDANDO_PECAS'");
            case PRONTAS -> where.append(" AND os.status='PRONTO_PARA_RETIRADA'");
            default -> { }
        }
        long total = jdbc.queryForObject("SELECT count(*)" + JOINS + where, Long.class, parameters.toArray());
        String selection = "SELECT os.id,os.numero,c.nome cliente_nome,v.placa,concat_ws(' ',v.marca,v.modelo) veiculo,"
            + "os.status,os.previsao_em,os.updated_at,os.versao,"
            + STAGE + " etapa_desde";
        String sort = switch (filter.sort()) {
            case ETAPA -> "etapa_desde ASC";
            case PREVISAO -> "os.previsao_em ASC NULLS LAST";
            case ATUALIZACAO -> "os.updated_at DESC";
        };
        parameters.add(filter.size()); parameters.add((long) filter.page() * filter.size());
        var items = jdbc.query(selection + JOINS + where + " ORDER BY " + sort + ",os.id DESC LIMIT ? OFFSET ?",
            (row, index) -> mapCard(row, now), parameters.toArray());
        return PageResult.of(items, filter.page(), filter.size(), total);
    }
    private void addSearch(StringBuilder where, List<Object> parameters, String query) {
        if (query.isBlank()) return;
        String number = query.replaceFirst("(?i)^OS[- ]?", "");
        if (number.matches("[0-9]{1,18}")) {
            where.append(" AND os.numero=?"); parameters.add(Long.parseLong(number)); return;
        }
        String term = "%" + query.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        String plate = query.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        where.append(" AND (lower(c.nome) LIKE ?"); parameters.add(term);
        if (!plate.isEmpty()) { where.append(" OR lower(v.placa) LIKE ?"); parameters.add("%" + plate + "%"); }
        where.append(")");
    }
    private Card mapCard(ResultSet row, Instant now) throws SQLException {
        ServiceOrderStatus status = ServiceOrderStatus.valueOf(row.getString("status"));
        Timestamp forecast = row.getTimestamp("previsao_em");
        Instant deadline = forecast == null ? null : forecast.toInstant();
        Instant stage = row.getTimestamp("etapa_desde").toInstant();
        return new Card(row.getObject("id", UUID.class), row.getLong("numero"), row.getString("cliente_nome"),
            row.getString("placa"), row.getString("veiculo"), status, deadline, ForecastPolicy.isLate(status, deadline, now),
            row.getTimestamp("updated_at").toInstant(), stage, Math.max(0, Duration.between(stage, now).getSeconds()), row.getLong("versao"));
    }
    public enum Situation { ATIVAS, ATRASADAS, APROVACAO, PECAS, PRONTAS }
    public enum Sort { ATUALIZACAO, ETAPA, PREVISAO }
    public record Filter(String query, ServiceOrderStatus status, Situation situation, Sort sort,
                         int page, int size, int staleHours, int stageHours) {}
    public record Indicators(long ativas, long atrasadas, long aprovacoes, long pecas, long prontas) {}
    public record Card(UUID id, long numero, String clienteNome, String placa, String veiculo, ServiceOrderStatus status,
                       Instant previsaoEm, boolean atrasada, Instant ultimaAtualizacao, Instant etapaDesde,
                       long tempoEtapaSegundos, long versao) {}
}
