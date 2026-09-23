package br.com.gestao.oficinas_api.ordem;

import br.com.gestao.oficinas_api.identidade.ApiException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ServicePhotoRepository {
    private final JdbcTemplate jdbc;
    public ServicePhotoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public Optional<ServicePhoto> byUpload(UUID shop, UUID order, UUID upload) { return jdbc.query("SELECT * FROM ordem_servico_foto WHERE oficina_id=? AND ordem_servico_id=? AND upload_id=? AND estado='PRONTA'", this::map, shop, order, upload).stream().findFirst(); }
    public List<ServicePhoto> list(UUID shop, UUID order) { return jdbc.query("SELECT * FROM ordem_servico_foto WHERE oficina_id=? AND ordem_servico_id=? AND estado='PRONTA' ORDER BY created_at DESC", this::map, shop, order); }
    public Stored find(UUID shop, UUID order, UUID id) { return jdbc.query("SELECT * FROM ordem_servico_foto WHERE oficina_id=? AND ordem_servico_id=? AND id=? AND estado='PRONTA'", this::stored, shop, order, id).stream().findFirst().orElseThrow(() -> new ApiException(404,"FOTO_NAO_ENCONTRADA","Foto não encontrada.")); }
    public void add(UUID shop, UUID owner, UUID order, UUID upload, ServiceOrderStatus stage, String caption, boolean published, String name, PhotoStorage.StoredPhoto file) {
        jdbc.update("INSERT INTO ordem_servico_foto(id,oficina_id,ordem_servico_id,etapa,legenda,publicada,nome_arquivo,tipo_conteudo,tamanho_bytes,chave_arquivo,chave_miniatura,upload_id,criado_por) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)", UUID.randomUUID(),shop,order,stage.name(),caption,published,name,file.contentType(),file.size(),file.key(),file.thumbnailKey(),upload,owner);
        audit(shop,owner,order,"FOTO_" + (published ? "PUBLICADA" : "ENVIADA"));
    }
    public void remove(UUID shop, UUID owner, UUID order, UUID id) { jdbc.update("UPDATE ordem_servico_foto SET estado='REMOVIDA',updated_at=now() WHERE oficina_id=? AND ordem_servico_id=? AND id=? AND estado='PRONTA'",shop,order,id); audit(shop,owner,order,"FOTO_REMOVIDA"); }
    private void audit(UUID shop, UUID owner, UUID order, String action) { jdbc.update("INSERT INTO cadastro_auditoria(id,oficina_id,proprietario_id,recurso,recurso_id,acao) VALUES (?,?,?,?,?,?)",UUID.randomUUID(),shop,owner,"ORDEM_SERVICO",order,action); }
    private ServicePhoto map(ResultSet r,int row) throws SQLException { return new ServicePhoto(r.getObject("id",UUID.class),ServiceOrderStatus.valueOf(r.getString("etapa")),r.getString("legenda"),r.getBoolean("publicada"),r.getString("tipo_conteudo"),r.getLong("tamanho_bytes"),r.getString("chave_miniatura")!=null,r.getTimestamp("created_at").toInstant()); }
    private Stored stored(ResultSet r,int row) throws SQLException { return new Stored(map(r,row),r.getString("chave_arquivo"),r.getString("chave_miniatura")); }
    public record Stored(ServicePhoto photo,String key,String thumbnailKey) {}
}
