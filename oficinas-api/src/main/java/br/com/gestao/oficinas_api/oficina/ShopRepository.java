package br.com.gestao.oficinas_api.oficina;

import br.com.gestao.oficinas_api.identidade.ApiException;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ShopRepository {
    private final JdbcTemplate jdbc;
    public ShopRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public ShopProfile find(UUID id) { return query("id=?", id); }
    public ShopProfile findPublic(String slug) { return query("slug=? AND perfil_publico=true", slug); }
    private ShopProfile query(String condition, Object value) {
        return jdbc.query("SELECT id,slug,nome,telefone,email_contato,endereco,horario,fuso,perfil_publico,"
                + "logo IS NOT NULL AS tem_logo,versao FROM oficina WHERE " + condition,
            (row, index) -> new ShopProfile(row.getObject("id", UUID.class), row.getString("slug"),
                row.getString("nome"), row.getString("telefone"), row.getString("email_contato"),
                row.getString("endereco"), row.getString("horario"), row.getString("fuso"),
                row.getBoolean("perfil_publico"), row.getBoolean("tem_logo"), row.getLong("versao")), value)
            .stream().findFirst().orElseThrow(ShopRepository::notFound);
    }
    public void save(ShopProfile profile) {
        checkUpdated(jdbc.update("""
            UPDATE oficina SET nome=?,telefone=?,email_contato=?,endereco=?,horario=?,fuso=?,
            perfil_publico=?,versao=versao+1,updated_at=now() WHERE id=? AND versao=?
            """, profile.nome(), profile.telefone(), profile.emailContato(), profile.endereco(),
            profile.horario(), profile.fuso(), profile.perfilPublico(), profile.id(), profile.versao()));
    }
    public void saveLogo(UUID id, long version, byte[] image) {
        checkUpdated(jdbc.update("UPDATE oficina SET logo=?,versao=versao+1,updated_at=now() WHERE id=? AND versao=?",
            image, id, version));
    }
    public byte[] findLogo(UUID id) {
        byte[] image = jdbc.queryForObject("SELECT logo FROM oficina WHERE id=?", byte[].class, id);
        if (image == null) throw notFound();
        return image;
    }
    public byte[] findPublicLogo(String slug) {
        return jdbc.query("SELECT logo FROM oficina WHERE slug=? AND perfil_publico=true AND logo IS NOT NULL",
            (row, index) -> row.getBytes("logo"), slug).stream().findFirst().orElseThrow(ShopRepository::notFound);
    }
    private void checkUpdated(int rows) {
        if (rows != 1) throw new ApiException(409, "OFICINA_DESATUALIZADA", "Os dados mudaram. Recarregue antes de salvar.");
    }
    private static ApiException notFound() {
        return new ApiException(404, "OFICINA_INDISPONIVEL", "Dados indisponíveis.");
    }
}
