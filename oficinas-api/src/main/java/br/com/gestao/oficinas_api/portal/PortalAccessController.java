package br.com.gestao.oficinas_api.portal;

import br.com.gestao.oficinas_api.identidade.ApiException;
import br.com.gestao.oficinas_api.identidade.AuthProperties;
import br.com.gestao.oficinas_api.notificacoes.TransactionalEmail;
import br.com.gestao.oficinas_api.ordem.PhotoStorage;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portal")
public class PortalAccessController {
    private static final Duration CHALLENGE_DURATION = Duration.ofMinutes(10);
    private static final Duration SESSION_DURATION = Duration.ofHours(24);
    private static final int MAX_ATTEMPTS = 5;

    private final JdbcTemplate jdbc;
    private final TransactionalEmail email;
    private final AuthProperties auth;
    private final Clock clock;
    private final PhotoStorage storage;
    private final PortalAccessPolicy policy;
    private final SecureRandom random = new SecureRandom();

    public PortalAccessController(JdbcTemplate jdbc, TransactionalEmail email, AuthProperties auth,
                                  Clock clock, PhotoStorage storage, PortalAccessPolicy policy) {
        this.jdbc = jdbc;
        this.email = email;
        this.auth = auth;
        this.clock = clock;
        this.storage = storage;
        this.policy = policy;
    }

    @PostMapping("/acesso/codigo")
    public ResponseEntity<Map<String, Object>> requestCode(@RequestBody Request body) {
        UUID publicId = UUID.randomUUID();
        if (body.oficinaSlug() == null || body.placa() == null) {
            return accepted(publicId);
        }
        String plate = normalizePlate(body.placa());
        var targets = jdbc.query("""
            SELECT c.id, c.email
              FROM oficina o
              JOIN veiculo v ON v.oficina_id=o.id
              JOIN vinculo_cliente_veiculo l ON l.oficina_id=v.oficina_id
                   AND l.veiculo_id=v.id AND l.fim_em IS NULL
              JOIN cliente c ON c.oficina_id=l.oficina_id AND c.id=l.cliente_id
             WHERE o.slug=? AND v.placa=? AND c.email_verificado_em IS NOT NULL AND c.ativo=true
            """, (result, row) -> new Target(result.getObject(1, UUID.class), result.getString(2)),
            body.oficinaSlug(), plate);
        if (targets.isEmpty()) {
            return accepted(publicId);
        }

        try {
            String value = "%06d".formatted(random.nextInt(1_000_000));
            jdbc.update("""
                INSERT INTO portal_desafio(id,oficina_id,cliente_id,placa,codigo_hash,expira_em)
                SELECT ?,o.id,?,?,?,? FROM oficina o WHERE o.slug=?
                """, publicId, targets.getFirst().id(), plate, hash(publicId, value),
                Timestamp.from(clock.instant().plus(CHALLENGE_DURATION)), body.oficinaSlug());
            email.send(targets.getFirst().email(), "Código de acesso · Gestão Oficinas",
                "Seu código é: " + value + "\n\nVálido por 10 minutos.");
        } catch (Exception ignored) {
            // A resposta é deliberadamente idêntica para não revelar cadastros ou falhas de entrega.
        }
        return accepted(publicId);
    }

    @PostMapping("/acesso/validacao")
    @Transactional
    public ResponseEntity<Void> validateCode(@RequestBody Validate body, HttpSession session) {
        if (body.desafioId() == null || body.codigo() == null || !body.codigo().matches("[0-9]{6}")) {
            throw invalidCode();
        }
        var challenges = jdbc.query("""
            SELECT oficina_id,cliente_id,placa,codigo_hash,expira_em,tentativas,usado_em
              FROM portal_desafio WHERE id=? FOR UPDATE
            """, (result, row) -> new Challenge(
                result.getObject(1, UUID.class), result.getObject(2, UUID.class), result.getString(3),
                result.getString(4), result.getTimestamp(5).toInstant(), result.getInt(6),
                instant(result.getTimestamp(7))), body.desafioId());
        if (challenges.isEmpty()) {
            throw invalidCode();
        }

        Challenge challenge = challenges.getFirst();
        boolean valid = policy.validChallenge(challenge.expires(), challenge.attempts(), challenge.used(),
            challenge.hash(), hash(body.desafioId(), body.codigo()), clock.instant());
        if (valid) {
            valid = hasCurrentVehicleLink(challenge.office(), challenge.customer(), challenge.plate());
        }
        if (!valid) {
            jdbc.update("UPDATE portal_desafio SET tentativas=tentativas+1 WHERE id=? AND tentativas<?",
                body.desafioId(), MAX_ATTEMPTS);
            throw invalidCode();
        }

        jdbc.update("UPDATE portal_desafio SET usado_em=? WHERE id=?", Timestamp.from(clock.instant()), body.desafioId());
        session.setAttribute("PORTAL_CLIENTE",
            new Grant(challenge.office(), challenge.customer(), null, null, clock.instant()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/acesso/link")
    @Transactional
    public ResponseEntity<Void> consumeLink(@RequestBody Link body, HttpSession session) {
        if (body.token() == null || body.token().length() < 40) {
            throw invalidLink();
        }
        String digest = sha(body.token());
        var links = jdbc.query("""
            SELECT id,oficina_id,ordem_servico_id,expira_em,revogado_em
              FROM portal_link_os WHERE token_hash=? FOR UPDATE
            """, (result, row) -> new LinkGrant(
                result.getObject(1, UUID.class), result.getObject(2, UUID.class),
                result.getObject(3, UUID.class), result.getTimestamp(4).toInstant(),
                instant(result.getTimestamp(5))), digest);
        if (links.isEmpty()) {
            throw invalidLink();
        }
        LinkGrant link = links.getFirst();
        if (!policy.validLink(link.expires(), link.revoked(), clock.instant()) || !activeOrder(link)) {
            throw invalidLink();
        }
        jdbc.update("UPDATE portal_link_os SET usado_em=? WHERE id=?", Timestamp.from(clock.instant()), link.id());
        session.setAttribute("PORTAL_CLIENTE",
            new Grant(link.office(), null, link.order(), link.id(), clock.instant()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/veiculos")
    public List<Vehicle> vehicles(HttpSession session) {
        Grant grant = grant(session);
        if (grant.order() != null) {
            return List.of();
        }
        return jdbc.query("""
            SELECT v.id,v.placa,concat_ws(' ',v.marca,v.modelo)
              FROM veiculo v
              JOIN vinculo_cliente_veiculo l ON l.oficina_id=v.oficina_id
                   AND l.veiculo_id=v.id AND l.fim_em IS NULL
             WHERE v.oficina_id=? AND l.cliente_id=? ORDER BY v.placa
            """, (result, row) -> new Vehicle(result.getObject(1, UUID.class), result.getString(2),
                result.getString(3)), grant.office(), grant.customer());
    }

    @GetMapping("/servico-atual")
    public CurrentService current(HttpSession session, @RequestParam(required = false) UUID veiculoId) {
        Grant grant = grant(session);
        Office office = jdbc.queryForObject("""
            SELECT nome,telefone,email_contato FROM oficina WHERE id=?
            """, (result, row) -> new Office(result.getString(1), result.getString(2), result.getString(3)),
            grant.office());
        var rows = jdbc.query("""
            SELECT os.id,os.numero,os.status,os.previsao_em,v.placa,
                   concat_ws(' ',v.marca,v.modelo) veiculo,
                   previsao.motivo_publico,previsao.proxima_acao,
                   GREATEST(
                     os.created_at,
                     COALESCE((SELECT max(e.created_at) FROM ordem_servico_evento e
                       WHERE e.oficina_id=os.oficina_id AND e.ordem_servico_id=os.id
                         AND e.publicada=true AND nullif(btrim(e.texto_publico),'') IS NOT NULL), os.created_at),
                     COALESCE((SELECT max(f.created_at) FROM ordem_servico_previsao f
                       WHERE f.oficina_id=os.oficina_id AND f.ordem_servico_id=os.id), os.created_at),
                     COALESCE((SELECT max(foto.created_at) FROM ordem_servico_foto foto
                       WHERE foto.oficina_id=os.oficina_id AND foto.ordem_servico_id=os.id
                         AND foto.estado='PRONTA' AND foto.publicada=true), os.created_at)
                   ) ultima_atualizacao
              FROM ordem_servico os
              JOIN veiculo v ON v.id=os.veiculo_id AND v.oficina_id=os.oficina_id
              LEFT JOIN vinculo_cliente_veiculo l ON l.oficina_id=v.oficina_id
                   AND l.veiculo_id=v.id AND l.fim_em IS NULL
              LEFT JOIN LATERAL (
                SELECT f.motivo_publico,f.proxima_acao
                  FROM ordem_servico_previsao f
                 WHERE f.oficina_id=os.oficina_id AND f.ordem_servico_id=os.id
                 ORDER BY f.created_at DESC,f.id DESC LIMIT 1
              ) previsao ON true
             WHERE os.oficina_id=? AND os.encerrada_em IS NULL
               AND (?::uuid IS NULL OR os.veiculo_id=?)
               AND (?::uuid IS NULL OR l.cliente_id=?)
               AND (?::uuid IS NULL OR os.id=?)
             ORDER BY os.entrada_em DESC LIMIT 1
            """, (result, row) -> new Service(
                result.getObject("id", UUID.class), result.getLong("numero"), result.getString("status"),
                instant(result.getTimestamp("previsao_em")), result.getString("placa"),
                result.getString("veiculo"), publicPending(result.getString("status")),
                result.getString("motivo_publico"), result.getString("proxima_acao"),
                result.getTimestamp("ultima_atualizacao").toInstant()),
            grant.office(), veiculoId, veiculoId, grant.customer(), grant.customer(), grant.order(), grant.order());
        return new CurrentService(office, rows.isEmpty() ? null : rows.getFirst());
    }

    @GetMapping("/ordens-servico/{id}/atualizacoes")
    public List<PublicUpdate> updates(HttpSession session, @PathVariable UUID id) {
        Grant grant = authorize(session, id);
        return jdbc.query("""
            SELECT e.id,e.tipo,e.status_anterior,e.status_novo,e.texto_publico,e.created_at
              FROM ordem_servico_evento e
             WHERE e.oficina_id=? AND e.ordem_servico_id=? AND e.publicada=true
               AND nullif(btrim(e.texto_publico),'') IS NOT NULL
             ORDER BY e.created_at ASC,e.id ASC
            """, (result, row) -> new PublicUpdate(
                result.getObject(1, UUID.class), result.getString(2), result.getString(3),
                result.getString(4), result.getString(5), result.getTimestamp(6).toInstant()),
            grant.office(), id);
    }

    @GetMapping("/ordens-servico/{id}/fotos")
    public List<PublicPhotoView> photos(HttpSession session, @PathVariable UUID id) {
        Grant grant = authorize(session, id);
        return jdbc.query("""
            SELECT id,etapa,legenda,created_at FROM ordem_servico_foto
             WHERE oficina_id=? AND ordem_servico_id=? AND estado='PRONTA' AND publicada=true
             ORDER BY created_at ASC,id ASC
            """, (result, row) -> new PublicPhotoView(
                result.getObject(1, UUID.class), result.getString(2), result.getString(3),
                result.getTimestamp(4).toInstant()), grant.office(), id);
    }

    @GetMapping("/ordens-servico/{orderId}/fotos/{photoId}/conteudo")
    public ResponseEntity<byte[]> photo(HttpSession session, @PathVariable UUID orderId,
                                        @PathVariable UUID photoId,
                                        @RequestParam(defaultValue = "miniatura") String tamanho) {
        Grant grant = authorize(session, orderId);
        var rows = jdbc.query("""
            SELECT chave_miniatura,chave_arquivo,tipo_conteudo FROM ordem_servico_foto
             WHERE oficina_id=? AND ordem_servico_id=? AND id=? AND publicada=true AND estado='PRONTA'
            """, (result, row) -> new PublicPhoto(result.getString(1), result.getString(2), result.getString(3)),
            grant.office(), orderId, photoId);
        if (rows.isEmpty()) {
            throw new ApiException(404, "FOTO_NAO_ENCONTRADA", "Foto não encontrada.");
        }
        PublicPhoto photo = rows.getFirst();
        boolean original = "original".equalsIgnoreCase(tamanho);
        String key = original || photo.thumb() == null ? photo.key() : photo.thumb();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .contentType(MediaType.parseMediaType(original || photo.thumb() == null ? photo.type() : "image/jpeg"))
            .body(storage.read(key));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build();
    }

    private Grant grant(HttpSession session) {
        Object stored = session.getAttribute("PORTAL_CLIENTE");
        if (!(stored instanceof Grant)) {
            expire(session);
        }
        Grant grant = (Grant) stored;
        if (policy.expiredSession(grant.createdAt(), clock.instant(), SESSION_DURATION)) {
            expire(session);
        }
        if (grant.linkId() != null && !currentLinkIsValid(grant)) {
            expire(session);
        }
        if (grant.customer() != null && !activeCustomer(grant.office(), grant.customer())) {
            expire(session);
        }
        return grant;
    }

    private Grant authorize(HttpSession session, UUID order) {
        Grant grant = grant(session);
        if (grant.order() != null) {
            if (grant.order().equals(order)) {
                return grant;
            }
            throw serviceNotFound();
        }
        Integer count = jdbc.queryForObject("""
            SELECT count(*) FROM ordem_servico os
              JOIN vinculo_cliente_veiculo l ON l.oficina_id=os.oficina_id
                   AND l.veiculo_id=os.veiculo_id AND l.fim_em IS NULL
             WHERE os.oficina_id=? AND os.id=? AND l.cliente_id=? AND os.encerrada_em IS NULL
            """, Integer.class, grant.office(), order, grant.customer());
        if (count == null || count == 0) {
            throw serviceNotFound();
        }
        return grant;
    }

    private boolean hasCurrentVehicleLink(UUID office, UUID customer, String plate) {
        Integer count = jdbc.queryForObject("""
            SELECT count(*) FROM cliente c
              JOIN vinculo_cliente_veiculo l ON l.oficina_id=c.oficina_id
                   AND l.cliente_id=c.id AND l.fim_em IS NULL
              JOIN veiculo v ON v.oficina_id=l.oficina_id AND v.id=l.veiculo_id
             WHERE c.oficina_id=? AND c.id=? AND c.ativo=true
               AND c.email_verificado_em IS NOT NULL AND v.placa=?
            """, Integer.class, office, customer, plate);
        return count != null && count > 0;
    }

    private boolean activeCustomer(UUID office, UUID customer) {
        Integer count = jdbc.queryForObject("""
            SELECT count(*) FROM cliente
             WHERE oficina_id=? AND id=? AND ativo=true AND email_verificado_em IS NOT NULL
            """, Integer.class, office, customer);
        return count != null && count > 0;
    }

    private boolean activeOrder(LinkGrant link) {
        Integer count = jdbc.queryForObject("""
            SELECT count(*) FROM ordem_servico
             WHERE oficina_id=? AND id=? AND encerrada_em IS NULL
            """, Integer.class, link.office(), link.order());
        return count != null && count > 0;
    }

    private boolean currentLinkIsValid(Grant grant) {
        var links = jdbc.query("""
            SELECT expira_em,revogado_em FROM portal_link_os
             WHERE id=? AND oficina_id=? AND ordem_servico_id=?
            """, (result, row) -> new LinkState(result.getTimestamp(1).toInstant(), instant(result.getTimestamp(2))),
            grant.linkId(), grant.office(), grant.order());
        if (links.isEmpty() || !policy.validLink(links.getFirst().expires(), links.getFirst().revoked(), clock.instant())) {
            return false;
        }
        Integer active = jdbc.queryForObject("""
            SELECT count(*) FROM ordem_servico
             WHERE oficina_id=? AND id=? AND encerrada_em IS NULL
            """, Integer.class, grant.office(), grant.order());
        return active != null && active > 0;
    }

    private void expire(HttpSession session) {
        session.invalidate();
        throw new ApiException(401, "ACESSO_EXPIRADO", "Acesse novamente.");
    }

    private ResponseEntity<Map<String, Object>> accepted(UUID challengeId) {
        return ResponseEntity.accepted().body(Map.of("desafioId", challengeId));
    }

    private String hash(UUID id, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(auth.codeSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((id + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String sha(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String normalizePlate(String value) {
        return value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    private Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private ApiException invalidCode() {
        return new ApiException(400, "CODIGO_INVALIDO", "Código inválido ou expirado.");
    }

    private ApiException invalidLink() {
        return new ApiException(400, "LINK_INVALIDO", "Link inválido ou expirado.");
    }

    private ApiException serviceNotFound() {
        return new ApiException(404, "SERVICO_NAO_ENCONTRADO", "Serviço não encontrado.");
    }

    private String publicPending(String status) {
        return switch (status) {
            case "AGUARDANDO_APROVACAO" -> "Aguardando aprovação do cliente";
            case "AGUARDANDO_PECAS" -> "Aguardando chegada de peças";
            case "PRONTO_PARA_RETIRADA" -> "Veículo pronto para retirada";
            default -> null;
        };
    }

    public record Request(String oficinaSlug, String placa) {}
    public record Validate(UUID desafioId, String codigo) {}
    public record Link(String token) {}
    public record Vehicle(UUID id, String placa, String veiculo) {}
    public record CurrentService(Office oficina, Service servico) {}
    public record Office(String nome, String telefone, String email) {}
    public record Service(UUID id, long numero, String status, Instant previsaoEm, String placa,
                          String veiculo, String pendencia, String motivoPrevisao,
                          String proximaAcao, Instant ultimaAtualizacao) {}
    public record PublicUpdate(UUID id, String tipo, String statusAnterior, String statusNovo,
                               String texto, Instant createdAt) {}
    public record PublicPhotoView(UUID id, String etapa, String legenda, Instant createdAt) {}
    private record Target(UUID id, String email) {}
    private record Challenge(UUID office, UUID customer, String plate, String hash, Instant expires,
                             int attempts, Instant used) {}
    private record LinkGrant(UUID id, UUID office, UUID order, Instant expires, Instant revoked) {}
    private record LinkState(Instant expires, Instant revoked) {}
    private record PublicPhoto(String thumb, String key, String type) {}
    private record Grant(UUID office, UUID customer, UUID order, UUID linkId, Instant createdAt)
        implements java.io.Serializable {}
}
