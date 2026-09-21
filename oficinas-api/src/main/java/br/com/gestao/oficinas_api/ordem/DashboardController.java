package br.com.gestao.oficinas_api.ordem;

import br.com.gestao.oficinas_api.identidade.*;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/painel")
public class DashboardController {
    private final DashboardService service;
    public DashboardController(DashboardService service) { this.service = service; }
    @GetMapping
    public DashboardService.Snapshot get(Authentication authentication, @RequestParam Map<String, String> parameters) {
        if (authentication == null || !(authentication.getDetails() instanceof Identidade owner)) {
            throw new ApiException(401, "NAO_AUTENTICADO", "Entre novamente.");
        }
        return service.get(owner, parameters);
    }
}
