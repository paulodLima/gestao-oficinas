package br.com.gestao.oficinas_api.notificacoes;

import br.com.gestao.oficinas_api.cadastro.PageResult;
import br.com.gestao.oficinas_api.identidade.ApiException;
import br.com.gestao.oficinas_api.identidade.Identidade;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notificacoes")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }
    @GetMapping
    public PageResult<NotificationService.Notice> list(Authentication authentication,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean naoLidas) {
        return service.list(identity(authentication).oficinaId(), page, size, naoLidas);
    }
    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(Authentication authentication, @PathVariable UUID id, @RequestBody ReadInput input) {
        UUID shop = identity(authentication).oficinaId();
        if (input == null || !Boolean.TRUE.equals(input.lida())) {
            throw new ApiException(400, "LEITURA_INVALIDA", "Informe lida=true.");
        }
        service.markRead(shop, id);
    }
    @PostMapping("/{id}/reenvio")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void retry(Authentication authentication, @PathVariable UUID id) {
        service.retry(identity(authentication).oficinaId(), id);
    }
    private Identidade identity(Authentication authentication) {
        if (authentication == null || !(authentication.getDetails() instanceof Identidade owner)) {
            throw new ApiException(401, "NAO_AUTENTICADO", "Entre novamente.");
        }
        return owner;
    }
    public record ReadInput(Boolean lida) {}
}
