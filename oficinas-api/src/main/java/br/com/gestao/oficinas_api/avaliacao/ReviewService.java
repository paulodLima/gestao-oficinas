package br.com.gestao.oficinas_api.avaliacao;

import br.com.gestao.oficinas_api.cadastro.PageResult;
import br.com.gestao.oficinas_api.identidade.ApiException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {
    private final JdbcTemplate jdbc;
    private final ReviewAccessService access;
    private final ReviewPolicy policy;
    private final Clock clock;
    public ReviewService(JdbcTemplate jdbc, ReviewAccessService access, ReviewPolicy policy, Clock clock) {
        this.jdbc = jdbc; this.access = access; this.policy = policy; this.clock = clock;
    }
    @Transactional(readOnly = true)
    public Summary summary(UUID invitation) {
        var grant = access.require(invitation, false);
        var context = jdbc.queryForObject("""
            SELECT os.numero,os.encerrada_em,o.nome,o.telefone,o.email_contato,o.google_avaliacao_url,
                   concat_ws(' ',v.marca,v.modelo) veiculo
              FROM ordem_servico os JOIN oficina o ON o.id=os.oficina_id
              JOIN veiculo v ON v.oficina_id=os.oficina_id AND v.id=os.veiculo_id
             WHERE os.oficina_id=? AND os.id=?
            """, (rs, row) -> new Context(rs.getLong(1), rs.getTimestamp(2).toInstant(), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)), grant.shop(), grant.order());
        var updates = jdbc.query("""
            SELECT texto_publico,created_at FROM ordem_servico_evento
             WHERE oficina_id=? AND ordem_servico_id=? AND publicada=true AND nullif(btrim(texto_publico),'') IS NOT NULL
             ORDER BY created_at,id
            """, (rs, row) -> new PublicUpdate(rs.getString(1), rs.getTimestamp(2).toInstant()), grant.shop(), grant.order());
        return new Summary(grant.id(), context, updates, existing(grant.order()), grant.expires(), ReviewPolicy.CONSENT);
    }
    @Transactional
    public Review submit(UUID invitation, ReviewPolicy.Input input) {
        var rating = policy.validate(input);
        var grant = access.require(invitation, true);
        Review existing = existing(grant.order());
        if (existing != null) {
            if (existing.nota() == rating.score() && Objects.equals(existing.comentario(), rating.comment())
                && existing.consentimentoPublicacao() == rating.consent()) return existing;
            throw new ApiException(409, "AVALIACAO_JA_REGISTRADA", "Este atendimento já foi avaliado. A avaliação anterior foi preservada.");
        }
        Timestamp at = Timestamp.from(clock.instant());
        jdbc.update("""
            INSERT INTO avaliacao(id,oficina_id,ordem_servico_id,cliente_id,convite_id,nota,comentario,
                consentimento_publicacao,consentimento_texto,consentimento_em,created_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?)
            """, UUID.randomUUID(), grant.shop(), grant.order(), grant.customer(), grant.id(), rating.score(), rating.comment(),
                rating.consent(), ReviewPolicy.CONSENT, rating.consent() ? at : null, at);
        jdbc.update("INSERT INTO avaliacao_auditoria(id,oficina_id,ordem_servico_id,cliente_id,acao,created_at) VALUES (?,?,?,?,'AVALIACAO_REGISTRADA',?)",
            UUID.randomUUID(), grant.shop(), grant.order(), grant.customer(), at);
        return existing(grant.order());
    }
    @Transactional(readOnly = true)
    public PageResult<OwnerReview> list(UUID shop, int page, int size) {
        if (page < 0 || page > 1_000_000 || size < 1 || size > 100) throw new ApiException(400, "PAGINACAO_INVALIDA", "Página ou tamanho inválido.");
        long total = jdbc.queryForObject("SELECT count(*) FROM avaliacao WHERE oficina_id=?", Long.class, shop);
        var items = jdbc.query("""
            SELECT a.*,os.numero FROM avaliacao a JOIN ordem_servico os ON os.oficina_id=a.oficina_id AND os.id=a.ordem_servico_id
             WHERE a.oficina_id=? ORDER BY a.created_at DESC,a.id DESC LIMIT ? OFFSET ?
            """, (rs, row) -> new OwnerReview(rs.getObject("id", UUID.class), rs.getObject("ordem_servico_id", UUID.class),
                rs.getLong("numero"), rs.getInt("nota"), rs.getString("comentario"), rs.getBoolean("consentimento_publicacao"),
                rs.getTimestamp("created_at").toInstant()), shop, size, (long) page * size);
        return PageResult.of(items, page, size, total);
    }
    public Configuration configuration(UUID shop) {
        return new Configuration(jdbc.queryForObject("SELECT google_avaliacao_url FROM oficina WHERE id=?", String.class, shop));
    }
    @Transactional
    public Configuration configure(UUID shop, String googleUrl) {
        String validated = policy.googleUrl(googleUrl);
        jdbc.update("UPDATE oficina SET google_avaliacao_url=? WHERE id=?", validated, shop);
        return new Configuration(validated);
    }
    private Review existing(UUID order) {
        return jdbc.query("SELECT nota,comentario,consentimento_publicacao,created_at FROM avaliacao WHERE ordem_servico_id=?",
            (rs, row) -> new Review(rs.getInt(1), rs.getString(2), rs.getBoolean(3), rs.getTimestamp(4).toInstant()), order)
            .stream().findFirst().orElse(null);
    }
    public record Configuration(String googleUrl) {}
    public record Review(int nota, String comentario, boolean consentimentoPublicacao, Instant createdAt) {}
    public record OwnerReview(UUID id, UUID ordemServicoId, long numero, int nota, String comentario, boolean consentimentoPublicacao, Instant createdAt) {}
    public record Context(long numero, Instant entregueEm, String oficinaNome, String telefone, String email, String googleUrl, String veiculo) {}
    public record PublicUpdate(String texto, Instant createdAt) {}
    public record Summary(UUID contexto, Context atendimento, List<PublicUpdate> atualizacoes, Review avaliacao, Instant expiraEm, String textoConsentimento) {}
}
