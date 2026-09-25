package br.com.gestao.oficinas_api.ordem;

import br.com.gestao.oficinas_api.identidade.ApiException;
import br.com.gestao.oficinas_api.identidade.Identidade;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ordens-servico/{orderId}/acesso")
public class ServiceOrderAccessController {
    private static final Duration LINK_DURATION = Duration.ofDays(7);

    private final JdbcTemplate jdbc;
    private final ServiceOrderService orders;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public ServiceOrderAccessController(JdbcTemplate jdbc, ServiceOrderService orders, Clock clock) {
        this.jdbc = jdbc;
        this.orders = orders;
        this.clock = clock;
    }

    @PostMapping
    @Transactional
    public Link create(Authentication authentication, @PathVariable UUID orderId) {
        Identidade owner = owner(authentication);
        lockOrder(owner, orderId);
        ServiceOrder order = orders.order(owner, orderId);
        if (!order.status().active()) {
            throw new ApiException(409, "ORDEM_ENCERRADA", "Não é possível compartilhar uma ordem encerrada.");
        }
        Instant now = clock.instant();
        jdbc.update("""
            UPDATE portal_link_os SET revogado_em=?
             WHERE oficina_id=? AND ordem_servico_id=? AND revogado_em IS NULL
            """, Timestamp.from(now), owner.oficinaId(), orderId);
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expires = now.plus(LINK_DURATION);
        jdbc.update("""
            INSERT INTO portal_link_os(id,oficina_id,ordem_servico_id,token_hash,expira_em,criado_por)
            VALUES (?,?,?,?,?,?)
            """, UUID.randomUUID(), owner.oficinaId(), orderId, sha(token), Timestamp.from(expires), owner.id());
        return new Link(token, expires);
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<Void> revoke(Authentication authentication, @PathVariable UUID orderId) {
        Identidade owner = owner(authentication);
        lockOrder(owner, orderId);
        orders.order(owner, orderId);
        jdbc.update("""
            UPDATE portal_link_os SET revogado_em=?
             WHERE oficina_id=? AND ordem_servico_id=? AND revogado_em IS NULL
            """, Timestamp.from(clock.instant()), owner.oficinaId(), orderId);
        return ResponseEntity.noContent().build();
    }

    private void lockOrder(Identidade owner, UUID orderId) {
        // Serialize issuance/revocation even when the order has no previous links.
        var rows = jdbc.query("SELECT id FROM ordem_servico WHERE oficina_id=? AND id=? FOR UPDATE",
            (result, row) -> result.getObject(1, UUID.class), owner.oficinaId(), orderId);
        if (rows.isEmpty()) throw new ApiException(404, "ORDEM_NAO_ENCONTRADA", "Ordem não encontrada.");
    }

    private Identidade owner(Authentication authentication) {
        if (authentication == null || !(authentication.getDetails() instanceof Identidade owner)) {
            throw new ApiException(401, "NAO_AUTENTICADO", "Entre novamente.");
        }
        return owner;
    }

    private String sha(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record Link(String token, Instant expiraEm) {}
}
