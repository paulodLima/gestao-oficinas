package br.com.gestao.oficinas_api.adicional;

import br.com.gestao.oficinas_api.identidade.ApiException;
import br.com.gestao.oficinas_api.identidade.Identidade;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ordens-servico/{orderId}/adicionais")
public class AdditionalRequestController {
    private final AdditionalRequestService service;

    public AdditionalRequestController(AdditionalRequestService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdditionalRequest> list(Authentication authentication, @PathVariable UUID orderId) {
        return service.list(identity(authentication), orderId);
    }

    @PostMapping
    public ResponseEntity<AdditionalRequest> create(Authentication authentication,
                                                     @PathVariable UUID orderId,
                                                     @RequestBody AdditionalRequestService.DraftInput input) {
        AdditionalRequest result = service.create(identity(authentication), orderId, input);
        return ResponseEntity.created(URI.create("/api/ordens-servico/" + orderId
            + "/adicionais/" + result.id())).body(result);
    }

    @PatchMapping("/{requestId}")
    public AdditionalRequest edit(Authentication authentication, @PathVariable UUID orderId,
                                  @PathVariable UUID requestId,
                                  @RequestBody AdditionalRequestService.DraftInput input) {
        return service.edit(identity(authentication), orderId, requestId, input);
    }

    @PostMapping("/{requestId}/envio")
    public AdditionalRequest send(Authentication authentication, @PathVariable UUID orderId,
                                  @PathVariable UUID requestId,
                                  @RequestBody AdditionalRequestService.VersionInput input) {
        return service.send(identity(authentication), orderId, requestId, input);
    }

    @PostMapping("/{requestId}/substituicoes")
    public AdditionalRequest replace(Authentication authentication, @PathVariable UUID orderId,
                                     @PathVariable UUID requestId,
                                     @RequestBody AdditionalRequestService.ReplacementInput input) {
        return service.replace(identity(authentication), orderId, requestId, input);
    }

    @PostMapping("/{requestId}/cancelamento")
    public AdditionalRequest cancel(Authentication authentication, @PathVariable UUID orderId,
                                    @PathVariable UUID requestId,
                                    @RequestBody AdditionalRequestService.CancelInput input) {
        return service.cancel(identity(authentication), orderId, requestId, input);
    }

    private Identidade identity(Authentication authentication) {
        if (authentication == null || !(authentication.getDetails() instanceof Identidade owner)) {
            throw new ApiException(401, "NAO_AUTENTICADO", "Entre novamente.");
        }
        return owner;
    }
}
