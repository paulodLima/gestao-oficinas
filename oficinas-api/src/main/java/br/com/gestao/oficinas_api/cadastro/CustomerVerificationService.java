package br.com.gestao.oficinas_api.cadastro;

import br.com.gestao.oficinas_api.identidade.*;
import br.com.gestao.oficinas_api.notificacoes.TransactionalEmail;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerVerificationService {
    private final JdbcTemplate jdbc;
    private final CustomerVehicleRepository repository;
    private final TransactionalEmail email;
    private final AuthProperties properties;
    private final Clock clock;
    private final RateLimit limits;
    private final SecureRandom random = new SecureRandom();
    public CustomerVerificationService(JdbcTemplate jdbc, CustomerVehicleRepository repository,
                                       TransactionalEmail email, AuthProperties properties, Clock clock, RateLimit limits) {
        this.jdbc = jdbc; this.repository = repository; this.email = email; this.properties = properties; this.clock = clock;
        this.limits = limits;
    }

    @Transactional
    public Challenge issue(Identidade owner, UUID customerId, String clientAddress) {
        lockCustomer(owner, customerId);
        Customer customer = repository.customer(owner.oficinaId(), customerId);
        if (customer.email().isBlank()) throw new ApiException(400, "EMAIL_AUSENTE", "Informe um e-mail antes de solicitar a verificação.");
        if (customer.emailVerificado()) throw new ApiException(409, "EMAIL_JA_VERIFICADO", "Este e-mail já está verificado.");
        limits.check("customer-verification:" + owner.oficinaId() + ":" + customerId, 5);
        limits.check("customer-verification-ip:" + clientAddress, 30);
        Integer recent = jdbc.queryForObject("""
            SELECT count(*) FROM verificacao_email_cliente
             WHERE oficina_id=? AND cliente_id=? AND created_at>now()-interval '60 seconds'
            """, Integer.class, owner.oficinaId(), customerId);
        if (recent != null && recent > 0)
            throw new ApiException(429, "REENVIO_ANTECIPADO", "Aguarde um minuto antes de solicitar outro código.");
        jdbc.update("DELETE FROM verificacao_email_cliente WHERE oficina_id=? AND cliente_id=? AND usado_em IS NULL",
            owner.oficinaId(), customerId);
        UUID challengeId = UUID.randomUUID();
        String code = "%06d".formatted(random.nextInt(1_000_000));
        Instant expires = clock.instant().plus(Duration.ofMinutes(10));
        jdbc.update("""
            INSERT INTO verificacao_email_cliente(id,oficina_id,cliente_id,codigo_hash,expira_em)
            VALUES (?,?,?,?,?)
            """, challengeId, owner.oficinaId(), customerId, hash(challengeId, code), Timestamp.from(expires));
        email.send(customer.email(), "Confirme seu e-mail · Gestão Oficinas",
            "Seu código de verificação é: " + code + "\n\nEle vale por 10 minutos e pode ser usado uma vez.");
        repository.audit(owner.oficinaId(), owner.id(), "CLIENTE", customerId, "VERIFICACAO_EMAIL_SOLICITADA");
        return new Challenge(challengeId, expires);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public Customer confirm(Identidade owner, UUID customerId, UUID challengeId, String code) {
        if (challengeId == null || code == null || !code.matches("[0-9]{6}")) throw invalid();
        lockCustomer(owner, customerId);
        var rows = jdbc.query("""
            SELECT codigo_hash,expira_em,tentativas,usado_em
              FROM verificacao_email_cliente
             WHERE id=? AND oficina_id=? AND cliente_id=? FOR UPDATE
            """, (rs, row) -> new Verification(rs.getString("codigo_hash"), rs.getTimestamp("expira_em").toInstant(),
                rs.getInt("tentativas"), rs.getTimestamp("usado_em") == null ? null : rs.getTimestamp("usado_em").toInstant()),
            challengeId, owner.oficinaId(), customerId);
        if (rows.isEmpty()) throw invalid();
        Verification verification = rows.getFirst();
        boolean valid = verification.usedAt == null && verification.attempts < 5
            && clock.instant().isBefore(verification.expiresAt)
            && MessageDigest.isEqual(verification.hash.getBytes(StandardCharsets.US_ASCII),
                hash(challengeId, code).getBytes(StandardCharsets.US_ASCII));
        if (!valid) {
            if (verification.usedAt == null && verification.attempts < 5)
                jdbc.update("UPDATE verificacao_email_cliente SET tentativas=tentativas+1 WHERE id=?", challengeId);
            throw invalid();
        }
        jdbc.update("UPDATE verificacao_email_cliente SET usado_em=? WHERE id=?", Timestamp.from(clock.instant()), challengeId);
        int updated = jdbc.update("UPDATE cliente SET email_verificado_em=?,versao=versao+1,updated_at=now() WHERE oficina_id=? AND id=?",
            Timestamp.from(clock.instant()), owner.oficinaId(), customerId);
        if (updated != 1) throw invalid();
        repository.audit(owner.oficinaId(), owner.id(), "CLIENTE", customerId, "EMAIL_VERIFICADO");
        return repository.customer(owner.oficinaId(), customerId);
    }

    private void lockCustomer(Identidade owner, UUID customerId) {
        var rows = jdbc.query("SELECT id FROM cliente WHERE oficina_id=? AND id=? FOR UPDATE",
            (result, row) -> result.getObject(1, UUID.class), owner.oficinaId(), customerId);
        if (rows.isEmpty()) throw new ApiException(404, "NAO_ENCONTRADO", "Cadastro não encontrado.");
    }

    private String hash(UUID challengeId, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.codeSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((challengeId + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) { throw new IllegalStateException(exception); }
    }
    private ApiException invalid() {
        return new ApiException(400, "CODIGO_INVALIDO", "Código inválido ou expirado. Solicite outro.");
    }
    public record Challenge(UUID desafioId, Instant expiraEm) {}
    private record Verification(String hash, Instant expiresAt, int attempts, Instant usedAt) {}
}
