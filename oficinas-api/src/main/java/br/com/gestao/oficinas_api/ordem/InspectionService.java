package br.com.gestao.oficinas_api.ordem;

import br.com.gestao.oficinas_api.identidade.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InspectionService {
    private final JdbcTemplate jdbc; private final ServiceOrderRepository orders; private final ObjectMapper json;
    public InspectionService(JdbcTemplate jdbc, ServiceOrderRepository orders, ObjectMapper json) { this.jdbc=jdbc; this.orders=orders; this.json=json; }
    public List<Inspection> list(Identidade owner, UUID order) { orders.order(owner.oficinaId(),order); return jdbc.query("SELECT * FROM ordem_servico_vistoria WHERE oficina_id=? AND ordem_servico_id=? ORDER BY numero_versao DESC",this::map,owner.oficinaId(),order); }
    @Transactional public Inspection draft(Identidade owner, UUID order, Map<String,Object> checklist) {
        active(owner,order); validate(checklist); var found=jdbc.query("SELECT id FROM ordem_servico_vistoria WHERE oficina_id=? AND ordem_servico_id=? AND estado='RASCUNHO' FOR UPDATE",(r,n)->r.getObject(1,UUID.class),owner.oficinaId(),order);
        String body=write(checklist); UUID id;
        if(!found.isEmpty()) { id=found.getFirst(); if(jdbc.update("UPDATE ordem_servico_vistoria SET checklist=?::jsonb,updated_at=now() WHERE id=? AND estado='RASCUNHO'",body,id)!=1) throw new ApiException(409,"VISTORIA_DESATUALIZADA","A vistoria foi confirmada. Recarregue antes de editar."); }
        else { id=UUID.randomUUID(); Integer next=jdbc.queryForObject("SELECT coalesce(max(numero_versao),0)+1 FROM ordem_servico_vistoria WHERE oficina_id=? AND ordem_servico_id=?",Integer.class,owner.oficinaId(),order); jdbc.update("INSERT INTO ordem_servico_vistoria(id,oficina_id,ordem_servico_id,numero_versao,estado,checklist,criado_por) VALUES (?,?,?,?, 'RASCUNHO',?::jsonb,?)",id,owner.oficinaId(),order,next,body,owner.id()); }
        audit(owner,order,"VISTORIA_RASCUNHO_SALVO"); return find(owner,order,id);
    }
    @Transactional public Inspection confirm(Identidade owner, UUID order, long expectedVersion) {
        active(owner,order); lockOrder(owner,order,expectedVersion); int changed=jdbc.update("UPDATE ordem_servico_vistoria SET estado='CONFIRMADA',updated_at=now() WHERE oficina_id=? AND ordem_servico_id=? AND estado='RASCUNHO'",owner.oficinaId(),order);
        if(changed!=1) throw new ApiException(409,"VISTORIA_SEM_RASCUNHO","Salve a vistoria antes de confirmar."); audit(owner,order,"VISTORIA_CONFIRMADA"); return jdbc.query("SELECT * FROM ordem_servico_vistoria WHERE oficina_id=? AND ordem_servico_id=? AND estado='CONFIRMADA' ORDER BY numero_versao DESC LIMIT 1",this::map,owner.oficinaId(),order).getFirst();
    }
    @Transactional public Inspection correct(Identidade owner, UUID order, long expectedVersion, String reason, Map<String,Object> checklist) {
        active(owner,order); if(reason==null||reason.isBlank()||reason.length()>1000) throw new ApiException(400,"DADOS_INVALIDOS","Informe o motivo da correção."); validate(checklist); lockOrder(owner,order,expectedVersion);
        Inspection previous=jdbc.query("SELECT * FROM ordem_servico_vistoria WHERE oficina_id=? AND ordem_servico_id=? AND estado='CONFIRMADA' ORDER BY numero_versao DESC LIMIT 1",this::map,owner.oficinaId(),order).stream().findFirst().orElseThrow(()->new ApiException(409,"VISTORIA_NAO_CONFIRMADA","Ainda não há uma vistoria confirmada."));
        UUID id=UUID.randomUUID(); jdbc.update("INSERT INTO ordem_servico_vistoria(id,oficina_id,ordem_servico_id,numero_versao,estado,checklist,motivo_correcao,versao_anterior_id,criado_por) VALUES (?,?,?,?, 'CONFIRMADA',?::jsonb,?,?,?)",id,owner.oficinaId(),order,previous.numeroVersao()+1,write(checklist),reason.trim(),previous.id(),owner.id()); audit(owner,order,"VISTORIA_CORRIGIDA"); return find(owner,order,id);
    }
    private void active(Identidade owner,UUID order) { if(!orders.order(owner.oficinaId(),order).status().active()) throw new ApiException(409,"ORDEM_ENCERRADA","A ordem de serviço já foi encerrada."); }
    private void lockOrder(Identidade owner,UUID order,long expected) { if(jdbc.update("UPDATE ordem_servico SET versao=versao+1,updated_at=now() WHERE oficina_id=? AND id=? AND versao=? AND encerrada_em IS NULL",owner.oficinaId(),order,expected)!=1) throw new ApiException(409,"ORDEM_DESATUALIZADA","A ordem mudou. Recarregue antes de continuar."); }
    private void validate(Map<String,Object> data) { if(data==null||data.size()>12) throw new ApiException(400,"DADOS_INVALIDOS","Checklist inválido."); Object km=data.get("quilometragem"); if(km instanceof Number n && (n.longValue()<0||n.longValue()>9_999_999)) throw new ApiException(400,"DADOS_INVALIDOS","Quilometragem inválida."); }
    private Inspection find(Identidade owner,UUID order,UUID id) { return jdbc.query("SELECT * FROM ordem_servico_vistoria WHERE oficina_id=? AND ordem_servico_id=? AND id=?",this::map,owner.oficinaId(),order,id).getFirst(); }
    private Inspection map(ResultSet r,int n) throws java.sql.SQLException { try { return new Inspection(r.getObject("id",UUID.class),r.getInt("numero_versao"),r.getString("estado"),json.readValue(r.getString("checklist"),new TypeReference<>(){}),r.getString("motivo_correcao"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant()); } catch(Exception e) { throw new IllegalStateException(e); } }
    private String write(Map<String,Object> data) { try{return json.writeValueAsString(data);}catch(Exception e){throw new ApiException(400,"DADOS_INVALIDOS","Checklist inválido.");} }
    private void audit(Identidade owner,UUID order,String action) { jdbc.update("INSERT INTO cadastro_auditoria(id,oficina_id,proprietario_id,recurso,recurso_id,acao) VALUES (?,?,?,?,?,?)",UUID.randomUUID(),owner.oficinaId(),owner.id(),"ORDEM_SERVICO",order,action); }
}
