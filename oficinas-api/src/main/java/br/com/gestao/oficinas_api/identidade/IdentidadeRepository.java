package br.com.gestao.oficinas_api.identidade;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IdentidadeRepository {
    private final JdbcTemplate jdbc;
    public IdentidadeRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Proprietario(UUID id, UUID oficinaId, String nome, String email,
                               String senhaHash, boolean ativo, long authVersion, String oficinaNome) {}
    private List<Proprietario> query(String where, Object... args) {
        return jdbc.query("""
            SELECT p.*, o.nome AS oficina_nome FROM proprietario p
            JOIN oficina o ON o.id=p.oficina_id WHERE """ + " " + where,
            (rs,n) -> new Proprietario(rs.getObject("id", UUID.class), rs.getObject("oficina_id", UUID.class),
                rs.getString("nome"), rs.getString("email"), rs.getString("senha_hash"), rs.getBoolean("ativo"),
                rs.getLong("auth_version"), rs.getString("oficina_nome")), args);
    }
    public Optional<Proprietario> porEmail(String email) { return query("p.email=?", email).stream().findFirst(); }
    public Optional<Proprietario> porIdentidade(Identidade identidade) {
        return query("p.id=? AND p.oficina_id=?", identidade.id(), identidade.oficinaId()).stream().findFirst();
    }
    public Proprietario cadastrar(String nome, String email, String hash, String nomeOficina) {
        UUID oficina = UUID.randomUUID(), id = UUID.randomUUID();
        jdbc.update("INSERT INTO oficina(id,nome,slug) VALUES (?,?,?)", oficina, nomeOficina, "oficina-" + oficina);
        jdbc.update("INSERT INTO proprietario(id,oficina_id,nome,email,senha_hash) VALUES (?,?,?,?,?)",
            id, oficina, nome, email, hash);
        auditar(id, oficina, "CADASTRO");
        return new Proprietario(id, oficina, nome, email, hash, true, 0, nomeOficina);
    }
    public void auditar(UUID id, UUID oficina, String acao) {
        jdbc.update("INSERT INTO identidade_auditoria(id,oficina_id,proprietario_id,acao) VALUES (?,?,?,?)",
            UUID.randomUUID(), oficina, id, acao);
    }
}

