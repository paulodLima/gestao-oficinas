package br.com.gestao.oficinas_api.ordem;

import br.com.gestao.oficinas_api.identidade.*;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/ordens-servico/{orderId}/vistoria")
public class InspectionController {
 private final InspectionService service; public InspectionController(InspectionService service){this.service=service;}
 @GetMapping public List<Inspection> list(Authentication a,@PathVariable UUID orderId){return service.list(owner(a),orderId);}
 @PutMapping public Inspection draft(Authentication a,@PathVariable UUID orderId,@RequestBody Map<String,Object> checklist){return service.draft(owner(a),orderId,checklist);}
 @PostMapping("/confirmacoes") public Inspection confirm(Authentication a,@PathVariable UUID orderId,@RequestBody Version body){return service.confirm(owner(a),orderId,body.expectedVersion());}
 @PostMapping("/correcoes") public Inspection correct(Authentication a,@PathVariable UUID orderId,@RequestBody Correction body){return service.correct(owner(a),orderId,body.expectedVersion(),body.motivo(),body.checklist());}
 private Identidade owner(Authentication a){if(a==null||!(a.getDetails() instanceof Identidade o))throw new ApiException(401,"NAO_AUTENTICADO","Entre novamente.");return o;}
 public record Version(long expectedVersion){} public record Correction(long expectedVersion,String motivo,Map<String,Object> checklist){}
}
