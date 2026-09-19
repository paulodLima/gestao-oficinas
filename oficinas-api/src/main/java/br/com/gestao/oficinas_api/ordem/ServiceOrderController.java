package br.com.gestao.oficinas_api.ordem;

import br.com.gestao.oficinas_api.cadastro.PageResult;
import br.com.gestao.oficinas_api.identidade.*;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ordens-servico")
public class ServiceOrderController {
    private final ServiceOrderService service;
    public ServiceOrderController(ServiceOrderService service) { this.service = service; }

    @GetMapping
    public PageResult<ServiceOrder> list(Authentication authentication,
                                         @RequestParam(defaultValue = "") String q,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return service.orders(identity(authentication), q, page, size);
    }
    @GetMapping("/{id}")
    public ServiceOrder get(Authentication authentication, @PathVariable UUID id) {
        return service.order(identity(authentication), id);
    }
    @PostMapping
    public ResponseEntity<ServiceOrder> create(Authentication authentication,
                                                @RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey,
                                                @RequestBody ServiceOrderService.CreateInput input) {
        ServiceOrder result = service.create(identity(authentication), idempotencyKey, input);
        return ResponseEntity.created(URI.create("/api/ordens-servico/" + result.id())).body(result);
    }
    private Identidade identity(Authentication authentication) {
        if (authentication == null || !(authentication.getDetails() instanceof Identidade owner)) {
            throw new ApiException(401, "NAO_AUTENTICADO", "Entre novamente.");
        }
        return owner;
    }
}
