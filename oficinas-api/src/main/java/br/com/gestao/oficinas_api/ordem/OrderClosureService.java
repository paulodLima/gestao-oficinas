package br.com.gestao.oficinas_api.ordem;

import br.com.gestao.oficinas_api.identidade.ApiException;
import br.com.gestao.oficinas_api.identidade.Identidade;
import br.com.gestao.oficinas_api.notificacoes.NotificationEvent;
import br.com.gestao.oficinas_api.notificacoes.NotificationService;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderClosureService {
    private final JdbcTemplate jdbc;
    private final ServiceOrderRepository orders;
    private final OrderClosurePolicy policy;
    private final NotificationService notifications;
    private final Clock clock;

    public OrderClosureService(JdbcTemplate jdbc, ServiceOrderRepository orders, OrderClosurePolicy policy,
                               NotificationService notifications, Clock clock) {
        this.jdbc = jdbc; this.orders = orders; this.policy = policy;
        this.notifications = notifications; this.clock = clock;
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Summary summary(Identidade owner, UUID id) {
        ServiceOrder order = orders.order(owner.oficinaId(), id);
        Detail detail = jdbc.query("""
            SELECT e.tipo,e.motivo,e.pendencias_canceladas,e.created_at,p.nome
              FROM ordem_servico_encerramento e JOIN proprietario p ON p.id=e.autor_id
             WHERE e.oficina_id=? AND e.ordem_servico_id=?
            """, (rs, row) -> new Detail(ServiceOrderStatus.valueOf(rs.getString(1)), rs.getString(2),
                rs.getInt(3), rs.getTimestamp(4).toInstant(), rs.getString(5)), owner.oficinaId(), id)
            .stream().findFirst().orElse(null);
        return new Summary(order.versao(), pending(owner.oficinaId(), id), detail);
    }

    @Transactional
    public ServiceOrder close(Identidade owner, UUID id, OrderClosurePolicy.Input input) {
        String reason = policy.validate(input);
        UUID shop = owner.oficinaId();
        ServiceOrder current = orders.lockActive(shop, id);
        if (current.versao() != input.expectedVersion()) {
            throw new ApiException(409, "ORDEM_DESATUALIZADA", "A ordem mudou. Recarregue antes de confirmar o encerramento.");
        }
        int pending = pending(shop, id);
        policy.requirePendingConsent(pending, input.cancelarPendencias());
        Timestamp at = Timestamp.from(clock.instant());
        jdbc.update("""
            UPDATE solicitacao_adicional SET estado='CANCELADA',versao=versao+1,updated_at=?,
                   motivo_cancelamento='Pendência cancelada explicitamente no encerramento da OS'
             WHERE oficina_id=? AND ordem_servico_id=? AND estado IN ('RASCUNHO','ENVIADA','PARCIALMENTE_DECIDIDA')
            """, at, shop, id);
        jdbc.update("""
            UPDATE ordem_servico SET status=?,encerrada_em=?,versao=versao+1,updated_at=?
             WHERE oficina_id=? AND id=?
            """, input.tipo().name(), at, at, shop, id);
        jdbc.update("""
            INSERT INTO ordem_servico_encerramento(ordem_servico_id,oficina_id,tipo,motivo,pendencias_canceladas,autor_id,created_at)
            VALUES (?,?,?,?,?,?,?)
            """, id, shop, input.tipo().name(), reason, pending, owner.id(), at);
        jdbc.update("""
            INSERT INTO ordem_servico_evento(id,oficina_id,ordem_servico_id,tipo,status_anterior,status_novo,motivo,autor_id,created_at)
            VALUES (?,?,?,'STATUS',?,?,?,?,?)
            """, UUID.randomUUID(), shop, id, current.status().name(), input.tipo().name(), reason, owner.id(), at);
        jdbc.update("""
            INSERT INTO cadastro_auditoria(id,oficina_id,proprietario_id,recurso,recurso_id,acao)
            VALUES (?,?,?,'ORDEM_SERVICO',?,'ORDEM_ENCERRADA')
            """, UUID.randomUUID(), shop, owner.id(), id);
        jdbc.update("UPDATE portal_link_os SET revogado_em=? WHERE oficina_id=? AND ordem_servico_id=? AND revogado_em IS NULL", at, shop, id);
        jdbc.update("UPDATE adicional_desafio SET usado_em=? WHERE oficina_id=? AND ordem_servico_id=? AND usado_em IS NULL", at, shop, id);
        notifications.record(shop, id, NotificationEvent.ORDEM_ENCERRADA, id.toString());
        return orders.order(shop, id);
    }

    private int pending(UUID shop, UUID id) {
        return jdbc.queryForObject("""
            SELECT count(*) FROM solicitacao_adicional WHERE oficina_id=? AND ordem_servico_id=?
             AND estado IN ('RASCUNHO','ENVIADA','PARCIALMENTE_DECIDIDA')
            """, Integer.class, shop, id);
    }

    public record Summary(long versao, int pendencias, Detail encerramento) {}
    public record Detail(ServiceOrderStatus tipo, String motivo, int pendenciasCanceladas,
                         Instant createdAt, String autorNome) {}
}
