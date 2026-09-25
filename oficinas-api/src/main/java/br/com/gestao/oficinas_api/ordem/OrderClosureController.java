package br.com.gestao.oficinas_api.ordem;

import br.com.gestao.oficinas_api.identidade.ApiException;
import br.com.gestao.oficinas_api.identidade.Identidade;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ordens-servico/{id}/encerramento")
public class OrderClosureController {
    private final OrderClosureService service;
    public OrderClosureController(OrderClosureService service) { this.service = service; }

    @GetMapping
    public OrderClosureService.Summary summary(Authentication auth, @PathVariable UUID id) {
        return service.summary(identity(auth), id);
    }

    @PostMapping
    public ServiceOrder close(Authentication auth, @PathVariable UUID id, @RequestBody OrderClosurePolicy.Input input) {
        return service.close(identity(auth), id, input);
    }

    private Identidade identity(Authentication auth) {
        if (auth == null || !(auth.getDetails() instanceof Identidade owner)) {
            throw new ApiException(401, "NAO_AUTENTICADO", "Entre novamente.");
        }
        return owner;
    }
}
