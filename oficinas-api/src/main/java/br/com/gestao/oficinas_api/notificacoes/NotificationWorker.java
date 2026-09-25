package br.com.gestao.oficinas_api.notificacoes;

import br.com.gestao.oficinas_api.identidade.AuthProperties;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationWorker {
    private final JdbcTemplate jdbc;
    private final TransactionalEmail email;
    private final Clock clock;
    private final AuthProperties auth;
    public NotificationWorker(JdbcTemplate jdbc, TransactionalEmail email, Clock clock, AuthProperties auth) {
        this.jdbc = jdbc; this.email = email; this.clock = clock; this.auth = auth;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processOne() {
        var pending = jdbc.query("""
            SELECT e.*,n.titulo,n.mensagem,o.nome oficina_nome,o.slug oficina_slug FROM notificacao_email e
              JOIN notificacao n ON n.id=e.notificacao_id
              JOIN oficina o ON o.id=n.oficina_id
             WHERE e.estado='PENDENTE' AND e.proxima_tentativa_em<=?
             ORDER BY e.proxima_tentativa_em,e.id LIMIT 1 FOR UPDATE OF e SKIP LOCKED
            """, (rs, row) -> new Pending(rs.getObject("id", UUID.class), rs.getObject("oficina_id", UUID.class),
                rs.getObject("cliente_id", UUID.class), rs.getString("destinatario"),
                rs.getTimestamp("verificado_em"), rs.getInt("tentativas"), rs.getInt("tentativas_ciclo"),
                rs.getString("titulo"), rs.getString("mensagem"), rs.getString("oficina_nome"),
                rs.getString("oficina_slug")), Timestamp.from(clock.instant()));
        if (pending.isEmpty()) return false;
        Pending item = pending.getFirst();
        // Hold a contact lock through delivery so an email change cannot overtake validation.
        var valid = jdbc.query("""
            SELECT id FROM cliente WHERE oficina_id=? AND id=? AND ativo=true
              AND email=? AND email_verificado_em=? FOR SHARE
            """, (rs, row) -> rs.getObject(1, UUID.class), item.shop(), item.customer(), item.recipient(), item.verified());
        if (valid.isEmpty()) {
            finish(item, "CANCELADO", "CONTATO_ALTERADO", false);
            return true;
        }
        try {
            email.send(item.recipient(), item.title() + " · Gestão Oficinas", item.message()
                + "\n\nOficina: " + item.shopName() + "\nIdentificador da oficina: " + item.shopSlug()
                + "\n\nConsulte os detalhes no portal da oficina: " + auth.publicUrl() + "/acompanhar"
                + "\nInforme o identificador acima e a placa do veículo para receber seu código de acesso."
                + "\nO acesso continua exigindo sua identificação. Este e-mail não autoriza serviços.");
        } catch (RuntimeException failure) {
            // Never persist provider exception messages or addresses in logs/API responses.
            finish(item, "FALHOU", "EMAIL_INDISPONIVEL", item.cycle() + 1 < 5);
            return true;
        }
        finish(item, "ENVIADO", null, false);
        return true;
    }

    private void finish(Pending item, String result, String error, boolean retry) {
        int attempt = item.attempts() + 1;
        jdbc.update("""
            INSERT INTO notificacao_email_tentativa(id,email_id,numero,resultado,erro,created_at)
            VALUES (?,?,?,?,?,?)
            """, UUID.randomUUID(), item.id(), attempt, result, error, Timestamp.from(clock.instant()));
        jdbc.update("""
            UPDATE notificacao_email SET estado=?,tentativas=?,tentativas_ciclo=tentativas_ciclo+1,
              proxima_tentativa_em=?,ultimo_erro=?,enviado_em=? WHERE id=?
            """, retry ? "PENDENTE" : result, attempt,
            Timestamp.from(clock.instant().plus(retry ? retryDelay(item.cycle() + 1) : Duration.ZERO)),
            error, "ENVIADO".equals(result) ? Timestamp.from(clock.instant()) : null, item.id());
    }

    static Duration retryDelay(int failedAttempts) {
        return Duration.ofMinutes(switch (failedAttempts) { case 1 -> 1; case 2 -> 5; case 3 -> 15; case 4 -> 60;
            default -> throw new IllegalArgumentException("Limite de tentativas atingido"); });
    }
    private record Pending(UUID id, UUID shop, UUID customer, String recipient, Timestamp verified,
                           int attempts, int cycle, String title, String message, String shopName, String shopSlug) {}
}
