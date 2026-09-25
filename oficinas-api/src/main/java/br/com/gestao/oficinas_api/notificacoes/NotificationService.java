package br.com.gestao.oficinas_api.notificacoes;

import br.com.gestao.oficinas_api.cadastro.PageResult;
import br.com.gestao.oficinas_api.identidade.ApiException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final JdbcTemplate jdbc;
    public NotificationService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID shop, UUID order, NotificationEvent event, String reference) {
        var contexts = jdbc.query("""
            SELECT os.numero,c.id,c.email,c.email_verificado_em,c.ativo
              FROM ordem_servico os JOIN cliente c ON c.oficina_id=os.oficina_id AND c.id=os.cliente_id
             WHERE os.oficina_id=? AND os.id=?
            """, (rs, row) -> new Contact(rs.getLong(1), rs.getObject(2, UUID.class),
                rs.getString(3), rs.getTimestamp(4), rs.getBoolean(5)), shop, order);
        if (contexts.isEmpty()) throw new IllegalStateException("Ordem do evento não encontrada");
        Contact contact = contexts.getFirst();
        UUID id = UUID.randomUUID();
        int inserted = jdbc.update("""
            INSERT INTO notificacao(id,oficina_id,ordem_servico_id,evento,referencia,titulo,mensagem)
            VALUES (?,?,?,?,?,?,?) ON CONFLICT (oficina_id,evento,referencia) DO NOTHING
            """, id, shop, order, event.name(), reference, event.title(), event.message(contact.number()));
        // A replay must not enqueue mail later merely because the contact became verified.
        if (inserted == 0 || !eligible(contact.email(), contact.verified(), contact.active())) return;
        jdbc.update("""
            INSERT INTO notificacao_email(id,oficina_id,notificacao_id,cliente_id,destinatario,verificado_em)
            VALUES (?,?,?,?,?,?)
            """, UUID.randomUUID(), shop, id, contact.customer(), contact.email(), contact.verified());
    }

    static boolean eligible(String email, Timestamp verified, boolean active) {
        return active && email != null && !email.isBlank() && verified != null;
    }

    @Transactional(readOnly = true)
    public PageResult<Notice> list(UUID shop, int page, int size, boolean unread) {
        if (page < 0 || page > 1_000_000 || size < 1 || size > 100) {
            throw new ApiException(400, "PAGINACAO_INVALIDA", "Informe uma página e tamanho válidos.");
        }
        String condition = unread ? " AND n.lida_em IS NULL" : "";
        Long total = jdbc.queryForObject("SELECT count(*) FROM notificacao n WHERE n.oficina_id=?" + condition,
            Long.class, shop);
        var items = jdbc.query("""
            SELECT n.*,e.estado,e.tentativas,e.proxima_tentativa_em,e.ultimo_erro
              FROM notificacao n LEFT JOIN notificacao_email e ON e.notificacao_id=n.id
             WHERE n.oficina_id=?
            """ + condition + " ORDER BY n.created_at DESC,n.id DESC LIMIT ? OFFSET ?",
            (rs, row) -> new Notice(rs.getObject("id", UUID.class), rs.getObject("ordem_servico_id", UUID.class),
                rs.getString("evento"), rs.getString("titulo"), rs.getString("mensagem"),
                rs.getTimestamp("lida_em") != null, rs.getTimestamp("created_at").toInstant(),
                rs.getString("estado") == null ? "SEM_EMAIL_VERIFICADO" : rs.getString("estado"),
                rs.getInt("tentativas"), instant(rs.getTimestamp("proxima_tentativa_em")),
                rs.getString("ultimo_erro")), shop, size, (long) page * size);
        return PageResult.of(items, page, size, total == null ? 0 : total);
    }

    public void markRead(UUID shop, UUID id) {
        if (jdbc.update("UPDATE notificacao SET lida_em=coalesce(lida_em,now()) WHERE oficina_id=? AND id=?",
            shop, id) == 0) throw missing();
    }

    @Transactional
    public void retry(UUID shop, UUID id) {
        // Return identical not-found responses across tenants, then atomically guard replay/cooldown.
        Integer found = jdbc.queryForObject("SELECT count(*) FROM notificacao WHERE oficina_id=? AND id=?",
            Integer.class, shop, id);
        if (found == null || found == 0) throw missing();
        int changed = jdbc.update("""
            UPDATE notificacao_email SET estado='PENDENTE',tentativas_ciclo=0,
              proxima_tentativa_em=now(),ultimo_reenvio_em=now()
             WHERE oficina_id=? AND notificacao_id=? AND estado='FALHOU'
               AND (ultimo_reenvio_em IS NULL OR ultimo_reenvio_em < now()-interval '1 minute')
            """, shop, id);
        if (changed == 0) throw new ApiException(409, "REENVIO_INDISPONIVEL",
            "Somente um envio com falha pode ser reenviado. Aguarde um minuto entre reenvios.");
    }

    private ApiException missing() { return new ApiException(404, "AVISO_NAO_ENCONTRADO", "Aviso não encontrado."); }
    private Instant instant(Timestamp value) { return value == null ? null : value.toInstant(); }
    private record Contact(long number, UUID customer, String email, Timestamp verified, boolean active) {}
    public record Notice(UUID id, UUID ordemServicoId, String evento, String titulo, String mensagem,
                         boolean lida, Instant createdAt, String emailEstado, int tentativas,
                         Instant proximaTentativaEm, String ultimoErro) {}
}
