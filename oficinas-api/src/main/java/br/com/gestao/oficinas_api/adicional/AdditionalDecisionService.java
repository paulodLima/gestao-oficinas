package br.com.gestao.oficinas_api.adicional;

import br.com.gestao.oficinas_api.identidade.ApiException;
import br.com.gestao.oficinas_api.identidade.AuthProperties;
import br.com.gestao.oficinas_api.notificacoes.TransactionalEmail;
import br.com.gestao.oficinas_api.notificacoes.NotificationService;
import br.com.gestao.oficinas_api.notificacoes.NotificationEvent;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdditionalDecisionService {
    private static final Duration CHALLENGE_DURATION = Duration.ofMinutes(10);
    private static final int MAX_ATTEMPTS = 5;
    private final JdbcTemplate jdbc;
    private final TransactionalEmail email;
    private final AuthProperties auth;
    private final AdditionalDecisionPolicy policy;
    private final Clock clock;
    private final NotificationService notifications;
    private final SecureRandom random = new SecureRandom();

    public AdditionalDecisionService(JdbcTemplate jdbc, TransactionalEmail email, AuthProperties auth,
                                     AdditionalDecisionPolicy policy, Clock clock, NotificationService notifications) {
        this.jdbc = jdbc;
        this.email = email;
        this.auth = auth;
        this.policy = policy;
        this.clock = clock;
        this.notifications = notifications;
    }

    public List<PublicRequest> list(UUID shop, UUID customer, UUID order) {
        verifyResponsibleCustomer(shop, customer, order);
        return jdbc.query("""
            SELECT id,estado,versao,created_at,updated_at FROM solicitacao_adicional
             WHERE oficina_id=? AND ordem_servico_id=?
               AND estado<>'CANCELADA' AND EXISTS (
                 SELECT 1 FROM adicional_versao v WHERE v.solicitacao_id=solicitacao_adicional.id
                   AND v.estado IN ('ENVIADA','SUBSTITUIDA'))
             ORDER BY created_at DESC,id DESC
            """, (result, row) -> publicRequest(shop, customer, order,
                result.getObject("id", UUID.class), result.getString("estado"),
                result.getLong("versao"), result.getTimestamp("created_at").toInstant(),
                result.getTimestamp("updated_at").toInstant()), shop, order);
    }

    @Transactional
    public ChallengeIssued issueCode(UUID shop, UUID customer, UUID order, UUID requestId) {
        lockOrder(shop, order);
        verifyResponsibleCustomer(shop, customer, order);
        Current current = current(shop, order, requestId, false);
        if (!Set.of("ENVIADA", "PARCIALMENTE_DECIDIDA").contains(current.state())) {
            throw conflict("Esta solicitação não aceita novas decisões.");
        }
        Contact contact = jdbc.query("""
            SELECT email,email_verificado_em FROM cliente
             WHERE oficina_id=? AND id=? AND ativo=true
            """, (result, row) -> new Contact(result.getString(1), instant(result.getTimestamp(2))),
            shop, customer).stream().findFirst().orElseThrow(this::accessDenied);
        if (contact.verifiedAt() == null || contact.email() == null || contact.email().isBlank()) {
            throw new ApiException(409, "EMAIL_NAO_VERIFICADO",
                "Confirme um e-mail com a oficina antes de decidir os adicionais.");
        }
        jdbc.update("""
            UPDATE adicional_desafio SET usado_em=now()
             WHERE oficina_id=? AND cliente_id=? AND solicitacao_id=? AND usado_em IS NULL
            """, shop, customer, requestId);
        UUID challengeId = UUID.randomUUID();
        String code = "%06d".formatted(random.nextInt(1_000_000));
        Instant expires = clock.instant().plus(CHALLENGE_DURATION);
        jdbc.update("""
            INSERT INTO adicional_desafio(id,oficina_id,cliente_id,ordem_servico_id,solicitacao_id,
              versao_id,versao_solicitacao,codigo_hash,expira_em)
            VALUES (?,?,?,?,?,?,?,?,?)
            """, challengeId, shop, customer, order, requestId, current.versionId(),
            current.requestVersion(), hash(challengeId, code), Timestamp.from(expires));
        email.send(contact.email(), "Código para decidir adicionais · Gestão Oficinas",
            "Seu código é: " + code + "\n\nVálido por 10 minutos e somente para esta solicitação.");
        return new ChallengeIssued(challengeId, expires);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public PublicRequest confirm(UUID shop, UUID customer, UUID order, UUID requestId,
                                 String idempotencyKey, Confirmation input) {
        verifyConfirmation(idempotencyKey, input);
        lockOrder(shop, order);
        verifyResponsibleCustomer(shop, customer, order);
        Current current = current(shop, order, requestId, true);
        String payloadHash = payloadHash(input);
        var previous = jdbc.query("""
            SELECT payload_hash FROM adicional_decisao_operacao
             WHERE solicitacao_id=? AND idempotency_key=?
            """, (result, row) -> result.getString(1), requestId, idempotencyKey);
        if (!previous.isEmpty()) {
            if (!previous.getFirst().equals(payloadHash)) {
                throw conflict("A chave de idempotência já foi usada com outra decisão.");
            }
            return publicRequest(shop, customer, order, requestId, current.state(),
                current.requestVersion(), current.createdAt(), current.updatedAt());
        }
        if (!Set.of("ENVIADA", "PARCIALMENTE_DECIDIDA").contains(current.state())
            || current.requestVersion() != input.versao()) {
            throw conflict("A solicitação foi alterada. Atualize a página antes de decidir.");
        }
        Challenge challenge = challenge(input.desafioId());
        boolean valid = challenge.shop().equals(shop) && challenge.customer().equals(customer)
            && challenge.order().equals(order) && challenge.request().equals(requestId)
            && challenge.versionId().equals(current.versionId())
            && challenge.requestVersion() == current.requestVersion()
            && policy.validChallenge(challenge.expires(), challenge.attempts(), challenge.used(),
                challenge.hash(), hash(input.desafioId(), input.codigo()), clock.instant());
        if (!valid) {
            jdbc.update("UPDATE adicional_desafio SET tentativas=tentativas+1 WHERE id=? AND tentativas<?",
                input.desafioId(), MAX_ATTEMPTS);
            throw new ApiException(400, "CODIGO_INVALIDO", "Código inválido ou expirado.");
        }

        Map<String, List<ItemRow>> blocks = blocks(current.versionId());
        Set<String> selected = input.decisoes().stream().map(BlockDecision::bloco).collect(Collectors.toSet());
        if (selected.size() != input.decisoes().size() || !blocks.keySet().containsAll(selected)) {
            throw new ApiException(400, "DECISAO_INVALIDA", "Selecione apenas blocos pendentes desta versão.");
        }
        Set<UUID> decided = decidedItems(current.versionId());
        for (String block : selected) {
            if (blocks.get(block).stream().anyMatch(item -> decided.contains(item.id()))) {
                throw conflict("Um dos blocos selecionados já foi decidido.");
            }
        }
        UUID operationId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO adicional_decisao_operacao(id,oficina_id,cliente_id,ordem_servico_id,
              solicitacao_id,versao_id,idempotency_key,payload_hash,comentario)
            VALUES (?,?,?,?,?,?,?,?,?)
            """, operationId, shop, customer, order, requestId, current.versionId(),
            idempotencyKey, payloadHash, blankToNull(input.comentario()));
        Map<String, Decision> decisions = input.decisoes().stream().collect(Collectors.toMap(
            BlockDecision::bloco, BlockDecision::decisao));
        for (String block : selected) {
            for (ItemRow item : blocks.get(block)) {
                jdbc.update("""
                    INSERT INTO adicional_item_decisao(id,operacao_id,solicitacao_id,versao_id,
                      item_id,cliente_id,decisao,bloco) VALUES (?,?,?,?,?,?,?,?)
                    """, UUID.randomUUID(), operationId, requestId, current.versionId(), item.id(),
                    customer, decisions.get(block).name(), block);
            }
        }
        jdbc.update("UPDATE adicional_desafio SET usado_em=? WHERE id=?",
            Timestamp.from(clock.instant()), input.desafioId());
        Integer total = jdbc.queryForObject("SELECT count(*) FROM adicional_item WHERE versao_id=?",
            Integer.class, current.versionId());
        Integer completed = jdbc.queryForObject("SELECT count(*) FROM adicional_item_decisao WHERE versao_id=?",
            Integer.class, current.versionId());
        String state = completed != null && completed.equals(total) ? "DECIDIDA" : "PARCIALMENTE_DECIDIDA";
        jdbc.update("""
            UPDATE solicitacao_adicional SET estado=?,versao=versao+1,updated_at=now()
             WHERE id=?
            """, state, requestId);
        Current updated = current(shop, order, requestId, false);
        notifications.record(shop, order, NotificationEvent.DECISAO_REGISTRADA, operationId.toString());
        return publicRequest(shop, customer, order, requestId, updated.state(),
            updated.requestVersion(), updated.createdAt(), updated.updatedAt());
    }

    private PublicRequest publicRequest(UUID shop, UUID customer, UUID order, UUID requestId,
                                        String state, long requestVersion, Instant created, Instant updated) {
        List<PublicVersion> versions = jdbc.query("""
            SELECT id,numero,estado,problema,justificativa,previsao_proposta,impacto_prazo,
                   motivo_substituicao,enviada_em,substituida_em
              FROM adicional_versao WHERE solicitacao_id=? AND estado IN ('ENVIADA','SUBSTITUIDA')
             ORDER BY numero DESC
            """, (result, row) -> {
                UUID versionId = result.getObject("id", UUID.class);
                List<PublicBlock> publicBlocks = publicBlocks(versionId);
                BigDecimal total = publicBlocks.stream().map(PublicBlock::total)
                    .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
                BigDecimal approvedTotal = publicBlocks.stream()
                    .filter(block -> "APROVADO".equals(block.decisao())).map(PublicBlock::total)
                    .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
                List<UUID> photos = jdbc.query("""
                    SELECT f.foto_id FROM adicional_versao_foto f
                      JOIN ordem_servico_foto p ON p.id=f.foto_id
                     WHERE f.versao_id=? AND p.publicada=true AND p.estado='PRONTA' ORDER BY f.ordem
                    """, (photosResult, photoRow) -> photosResult.getObject(1, UUID.class), versionId);
                return new PublicVersion(versionId, result.getInt("numero"), result.getString("estado"),
                    result.getString("problema"), result.getString("justificativa"),
                    instant(result.getTimestamp("previsao_proposta")), result.getString("impacto_prazo"),
                    result.getString("motivo_substituicao"), total, approvedTotal, photos, publicBlocks,
                    instant(result.getTimestamp("enviada_em")), instant(result.getTimestamp("substituida_em")));
            }, requestId);
        return new PublicRequest(requestId, order, state, requestVersion, versions, created, updated);
    }

    private List<PublicBlock> publicBlocks(UUID versionId) {
        Map<String, List<ItemRow>> grouped = blocks(versionId);
        Map<UUID, DecisionRow> decisions = jdbc.query("""
            SELECT item_id,decisao,created_at FROM adicional_item_decisao WHERE versao_id=?
            """, result -> {
                Map<UUID, DecisionRow> values = new LinkedHashMap<>();
                while (result.next()) values.put(result.getObject(1, UUID.class),
                    new DecisionRow(result.getString(2), result.getTimestamp(3).toInstant()));
                return values;
            }, versionId);
        List<PublicBlock> result = new ArrayList<>();
        grouped.forEach((key, items) -> {
            DecisionRow decision = decisions.get(items.getFirst().id());
            BigDecimal total = items.stream().map(ItemRow::total).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
            result.add(new PublicBlock(key, items.getFirst().group(), total,
                items.stream().map(item -> new PublicItem(item.id(), item.type(), item.description(),
                    item.quantity(), item.unitValue(), item.total())).toList(),
                decision == null ? null : decision.decision(), decision == null ? null : decision.at()));
        });
        return result;
    }

    private Map<String, List<ItemRow>> blocks(UUID versionId) {
        List<ItemRow> items = jdbc.query("""
            SELECT id,tipo,descricao,quantidade,valor_unitario,total,grupo_dependencia,ordem
              FROM adicional_item WHERE versao_id=? ORDER BY ordem
            """, (result, row) -> new ItemRow(result.getObject("id", UUID.class),
                result.getString("tipo"), result.getString("descricao"), result.getBigDecimal("quantidade"),
                result.getBigDecimal("valor_unitario"), result.getBigDecimal("total"),
                result.getString("grupo_dependencia"), result.getInt("ordem")), versionId);
        Map<String, List<ItemRow>> grouped = new LinkedHashMap<>();
        for (ItemRow item : items) {
            String key = item.group() == null || item.group().isBlank()
                ? "item:" + item.id() : "grupo:" + item.group();
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(item);
        }
        return grouped;
    }

    private Set<UUID> decidedItems(UUID versionId) {
        return Set.copyOf(jdbc.query("SELECT item_id FROM adicional_item_decisao WHERE versao_id=?",
            (result, row) -> result.getObject(1, UUID.class), versionId));
    }

    private Current current(UUID shop, UUID order, UUID requestId, boolean lock) {
        String suffix = lock ? " FOR UPDATE" : "";
        return jdbc.query("""
            SELECT s.estado,s.versao,s.created_at,s.updated_at,v.id versao_id
              FROM solicitacao_adicional s JOIN adicional_versao v ON v.solicitacao_id=s.id
             WHERE s.oficina_id=? AND s.ordem_servico_id=? AND s.id=? AND v.estado='ENVIADA'
            """ + suffix, (result, row) -> new Current(result.getString("estado"),
                result.getLong("versao"), result.getObject("versao_id", UUID.class),
                result.getTimestamp("created_at").toInstant(), result.getTimestamp("updated_at").toInstant()),
            shop, order, requestId).stream().findFirst().orElseThrow(() ->
                new ApiException(404, "ADICIONAL_NAO_ENCONTRADO", "Solicitação adicional não encontrada."));
    }

    private Challenge challenge(UUID id) {
        return jdbc.query("""
            SELECT oficina_id,cliente_id,ordem_servico_id,solicitacao_id,versao_id,
                   versao_solicitacao,codigo_hash,expira_em,tentativas,usado_em
              FROM adicional_desafio WHERE id=? FOR UPDATE
            """, (result, row) -> new Challenge(result.getObject(1, UUID.class),
                result.getObject(2, UUID.class), result.getObject(3, UUID.class),
                result.getObject(4, UUID.class), result.getObject(5, UUID.class), result.getLong(6),
                result.getString(7), result.getTimestamp(8).toInstant(), result.getInt(9),
                instant(result.getTimestamp(10))), id).stream().findFirst()
            .orElseThrow(() -> new ApiException(400, "CODIGO_INVALIDO", "Código inválido ou expirado."));
    }

    private void lockOrder(UUID shop, UUID order) {
        // Always lock OS before request/version/challenge, including code issuance.
        jdbc.query("SELECT id FROM ordem_servico WHERE oficina_id=? AND id=? FOR UPDATE",
            (result, row) -> result.getObject(1, UUID.class), shop, order);
    }

    private void verifyResponsibleCustomer(UUID shop, UUID customer, UUID order) {
        Integer count = jdbc.queryForObject("""
            SELECT count(*) FROM ordem_servico os JOIN cliente c
              ON c.oficina_id=os.oficina_id AND c.id=os.cliente_id
             WHERE os.oficina_id=? AND os.id=? AND os.cliente_id=?
               AND os.encerrada_em IS NULL AND c.ativo=true
            """, Integer.class, shop, order, customer);
        if (count == null || count == 0) throw accessDenied();
    }

    private void verifyConfirmation(String key, Confirmation input) {
        if (key == null || key.isBlank() || key.length() > 100 || input == null
            || input.desafioId() == null || input.codigo() == null || !input.codigo().matches("[0-9]{6}")
            || input.decisoes() == null || input.decisoes().isEmpty() || input.decisoes().size() > 50
            || input.versao() < 0 || (input.comentario() != null && input.comentario().length() > 1000)
            || input.decisoes().stream().anyMatch(value -> value == null || value.bloco() == null
                || value.bloco().isBlank() || value.decisao() == null)) {
            throw new ApiException(400, "DECISAO_INVALIDA", "Revise as decisões informadas.");
        }
    }

    private String payloadHash(Confirmation input) {
        String decisions = input.decisoes().stream().sorted(Comparator.comparing(BlockDecision::bloco))
            .map(value -> value.bloco() + "=" + value.decisao()).collect(Collectors.joining("|"));
        return sha(input.desafioId() + ":" + input.codigo() + ":" + input.versao() + ":"
            + blankToNull(input.comentario()) + ":" + decisions);
    }

    private String hash(UUID id, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(auth.codeSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((id + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) { throw new IllegalStateException(exception); }
    }

    private String sha(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) { throw new IllegalStateException(exception); }
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private Instant instant(Timestamp value) { return value == null ? null : value.toInstant(); }
    private ApiException conflict(String detail) { return new ApiException(409, "CONFLITO_DE_DECISAO", detail); }
    private ApiException accessDenied() { return new ApiException(404, "SERVICO_NAO_ENCONTRADO", "Serviço não encontrado."); }

    public enum Decision { APROVADO, RECUSADO }
    public record BlockDecision(String bloco, Decision decisao) {}
    public record Confirmation(UUID desafioId, String codigo, long versao,
                               List<BlockDecision> decisoes, String comentario) {}
    public record ChallengeIssued(UUID desafioId, Instant expiraEm) {}
    public record PublicRequest(UUID id, UUID ordemServicoId, String estado, long versao,
                                List<PublicVersion> versoes, Instant createdAt, Instant updatedAt) {}
    public record PublicVersion(UUID id, int numero, String estado, String problema,
                                String justificativa, Instant previsaoProposta, String impactoPrazo,
                                String motivoSubstituicao, BigDecimal total, BigDecimal totalAprovado,
                                List<UUID> fotoIds,
                                List<PublicBlock> blocos, Instant enviadaEm, Instant substituidaEm) {}
    public record PublicBlock(String id, String grupoDependencia, BigDecimal total,
                              List<PublicItem> itens, String decisao, Instant decididaEm) {}
    public record PublicItem(UUID id, String tipo, String descricao, BigDecimal quantidade,
                             BigDecimal valorUnitario, BigDecimal total) {}
    private record Contact(String email, Instant verifiedAt) {}
    private record Current(String state, long requestVersion, UUID versionId,
                           Instant createdAt, Instant updatedAt) {}
    private record Challenge(UUID shop, UUID customer, UUID order, UUID request, UUID versionId,
                             long requestVersion, String hash, Instant expires, int attempts, Instant used) {}
    private record ItemRow(UUID id, String type, String description, BigDecimal quantity,
                           BigDecimal unitValue, BigDecimal total, String group, int order) {}
    private record DecisionRow(String decision, Instant at) {}
}
