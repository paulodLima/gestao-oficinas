package br.com.gestao.oficinas_api.avaliacao;

import br.com.gestao.oficinas_api.identidade.*;
import br.com.gestao.oficinas_api.notificacoes.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class ReviewAccessService {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final AuthProperties auth;
    private final NotificationService notifications;
    public ReviewAccessService(JdbcTemplate jdbc, Clock clock, AuthProperties auth, NotificationService notifications) {
        this.jdbc = jdbc; this.clock = clock; this.auth = auth; this.notifications = notifications;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void issueForDelivery(UUID shop, UUID order) {
        var rows = jdbc.query("""
            SELECT os.cliente_id,os.encerrada_em,c.email,c.email_verificado_em FROM ordem_servico os
              JOIN cliente c ON c.oficina_id=os.oficina_id AND c.id=os.cliente_id
             WHERE os.oficina_id=? AND os.id=? AND os.status='ENTREGUE' FOR UPDATE OF os
            """, (rs, row) -> new Delivery(rs.getObject(1, UUID.class), rs.getTimestamp(2).toInstant(),
                rs.getString(3), rs.getTimestamp(4)), shop, order);
        if (rows.isEmpty()) throw new ApiException(409, "AVALIACAO_INDISPONIVEL", "Somente uma OS entregue pode receber avaliação.");
        Delivery delivery = rows.getFirst();
        Instant expires = delivery.at().plus(ReviewPolicy.VALIDITY);
        if (!clock.instant().isBefore(expires)) throw unavailable();
        UUID id = UUID.randomUUID();
        int added = jdbc.update("""
            INSERT INTO avaliacao_convite(id,oficina_id,ordem_servico_id,cliente_id,contato_email,contato_verificado_em,token_hash,expira_em)
            VALUES (?,?,?,?,?,?,?,?) ON CONFLICT(ordem_servico_id) DO NOTHING
            """, id, shop, order, delivery.customer(), delivery.email(), delivery.verified(), hash(token(id)), Timestamp.from(expires));
        if (added == 1) notifications.record(shop, order, NotificationEvent.AVALIACAO_SOLICITADA, id.toString());
    }

    @Transactional
    public Invitation invitation(Identidade owner, UUID order) {
        ensureOwned(owner.oficinaId(), order);
        issueForDelivery(owner.oficinaId(), order);
        UUID id = jdbc.queryForObject("SELECT id FROM avaliacao_convite WHERE oficina_id=? AND ordem_servico_id=?", UUID.class, owner.oficinaId(), order);
        Access access = exchange(token(id));
        return new Invitation(token(id), access.expires());
    }

    @Transactional
    public void revoke(Identidade owner, UUID order) {
        ensureOwned(owner.oficinaId(), order);
        jdbc.update("UPDATE avaliacao_convite SET revogado_em=coalesce(revogado_em,?) WHERE oficina_id=? AND ordem_servico_id=?",
            Timestamp.from(clock.instant()), owner.oficinaId(), order);
        jdbc.update("INSERT INTO cadastro_auditoria(id,oficina_id,proprietario_id,recurso,recurso_id,acao) VALUES (?,?,?,'ORDEM_SERVICO',?,'CONVITE_AVALIACAO_REVOGADO')",
            UUID.randomUUID(), owner.oficinaId(), owner.id(), order);
    }

    @Transactional(readOnly = true)
    public Access exchange(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw unavailable();
        var ids = jdbc.query("SELECT id FROM avaliacao_convite WHERE token_hash=?", (rs, row) -> rs.getObject(1, UUID.class), hash(token));
        if (ids.isEmpty()) throw unavailable();
        return require(ids.getFirst(), false);
    }

    public Access require(UUID id, boolean lock) {
        var rows = jdbc.query("""
            SELECT i.id,i.oficina_id,i.ordem_servico_id,i.cliente_id,i.expira_em FROM avaliacao_convite i
              JOIN ordem_servico os ON os.oficina_id=i.oficina_id AND os.id=i.ordem_servico_id AND os.cliente_id=i.cliente_id
              JOIN cliente c ON c.oficina_id=i.oficina_id AND c.id=i.cliente_id
             WHERE i.id=? AND i.revogado_em IS NULL AND i.expira_em>? AND os.status='ENTREGUE'
               AND c.ativo=true AND c.email IS NOT DISTINCT FROM i.contato_email
               AND c.email_verificado_em IS NOT DISTINCT FROM i.contato_verificado_em
            """ + (lock ? " FOR UPDATE OF i FOR SHARE OF c" : ""),
            (rs, row) -> new Access(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getObject(3, UUID.class),
                rs.getObject(4, UUID.class), rs.getTimestamp(5).toInstant()), id, Timestamp.from(clock.instant()));
        return rows.stream().findFirst().orElseThrow(ReviewAccessService::unavailable);
    }

    public Optional<String> emailLink(UUID shop, UUID id) {
        try {
            Access access = exchange(token(id));
            if (!access.shop().equals(shop)) return Optional.empty();
            return Optional.of(auth.publicUrl() + "/avaliar#token=" + token(id));
        } catch (ApiException invalid) { return Optional.empty(); }
    }

    private void ensureOwned(UUID shop, UUID order) {
        if (jdbc.query("SELECT id FROM ordem_servico WHERE oficina_id=? AND id=? FOR UPDATE",
            (rs, row) -> rs.getObject(1, UUID.class), shop, order).isEmpty()) {
            throw new ApiException(404, "ORDEM_NAO_ENCONTRADA", "Ordem de serviço não encontrada.");
        }
    }
    private String token(UUID id) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(auth.codeSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(("AVALIACAO_ENTREGA_V1:" + id).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) { throw new IllegalStateException("Não foi possível gerar credencial de avaliação", error); }
    }
    private String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception error) { throw new IllegalStateException(error); }
    }
    static ApiException unavailable() { return new ApiException(401, "AVALIACAO_ACESSO_INVALIDO", "Acesso de avaliação inválido ou expirado. Solicite orientação à oficina."); }
    private record Delivery(UUID customer, Instant at, String email, Timestamp verified) {}
    public record Access(UUID id, UUID shop, UUID order, UUID customer, Instant expires) {}
    public record Invitation(String token, Instant expiraEm) {}
}
