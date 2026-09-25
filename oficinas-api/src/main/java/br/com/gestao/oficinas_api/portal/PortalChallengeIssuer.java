package br.com.gestao.oficinas_api.portal;

import br.com.gestao.oficinas_api.identidade.AuthProperties;
import br.com.gestao.oficinas_api.identidade.RateLimit;
import br.com.gestao.oficinas_api.notificacoes.TransactionalEmail;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortalChallengeIssuer {
    private static final Duration LIFETIME = Duration.ofMinutes(10);
    private static final Duration COOLDOWN = Duration.ofSeconds(60);
    private static final int REQUESTS_PER_IP = 30;
    private static final int REQUESTS_PER_CONTACT = 5;
    private final JdbcTemplate jdbc;
    private final TransactionalEmail email;
    private final AuthProperties auth;
    private final RateLimit limits;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public PortalChallengeIssuer(JdbcTemplate jdbc, TransactionalEmail email, AuthProperties auth,
                                 RateLimit limits, Clock clock) {
        this.jdbc = jdbc;
        this.email = email;
        this.auth = auth;
        this.limits = limits;
        this.clock = clock;
    }

    @Transactional
    public void issue(UUID id, PortalAccessController.Request body, String address) {
        limits.check("portal-issue-ip:" + address, REQUESTS_PER_IP);
        if (body.oficinaSlug() == null || body.placa() == null
            || body.oficinaSlug().length() > 120 || body.placa().length() > 20) return;
        String plate = body.placa().replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        var targets = jdbc.query("""
            SELECT c.id,c.oficina_id,c.email,c.acesso_versao FROM oficina o
              JOIN veiculo v ON v.oficina_id=o.id
              JOIN vinculo_cliente_veiculo l ON l.oficina_id=v.oficina_id
                AND l.veiculo_id=v.id AND l.fim_em IS NULL
              JOIN cliente c ON c.oficina_id=l.oficina_id AND c.id=l.cliente_id
             WHERE o.slug=? AND v.placa=? AND c.email_verificado_em IS NOT NULL AND c.ativo=true
             FOR UPDATE OF c
            """, (row, number) -> new Target(row.getObject(1, UUID.class), row.getObject(2, UUID.class),
                row.getString(3), row.getLong(4)), body.oficinaSlug(), plate);
        if (targets.isEmpty()) return;
        Target target = targets.getFirst();
        limits.check("portal-issue-contact:" + target.email(), REQUESTS_PER_CONTACT);
        if (jdbc.queryForObject("""
            SELECT count(*) FROM portal_desafio WHERE oficina_id=? AND cliente_id=? AND created_at>?
            """, Integer.class, target.office(), target.customer(),
            Timestamp.from(clock.instant().minus(COOLDOWN))) > 0) return;
        jdbc.update("DELETE FROM portal_desafio WHERE oficina_id=? AND cliente_id=? AND usado_em IS NULL",
            target.office(), target.customer());
        String code = "%06d".formatted(random.nextInt(1_000_000));
        jdbc.update("""
            INSERT INTO portal_desafio(id,oficina_id,cliente_id,placa,codigo_hash,expira_em,acesso_versao)
            VALUES (?,?,?,?,?,?,?)
            """, id, target.office(), target.customer(), plate,
            PortalCodeHash.hash(auth.codeSecret(), id, code),
            Timestamp.from(clock.instant().plus(LIFETIME)), target.version());
        email.send(target.email(), "Código de acesso · Gestão Oficinas",
            "Seu código é: " + code + "\n\nVálido por 10 minutos.");
    }

    private record Target(UUID customer, UUID office, String email, long version) {}
}
