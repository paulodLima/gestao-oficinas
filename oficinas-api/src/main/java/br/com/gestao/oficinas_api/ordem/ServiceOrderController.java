package br.com.gestao.oficinas_api.ordem;

import br.com.gestao.oficinas_api.cadastro.PageResult;
import br.com.gestao.oficinas_api.identidade.*;
import java.net.URI;
import java.util.List;
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
    @PostMapping("/{id}/status")
    public ServiceOrder changeStatus(Authentication authentication, @PathVariable UUID id,
                                     @RequestBody ServiceOrderService.StatusInput input) {
        return service.changeStatus(identity(authentication), id, input);
    }
    @GetMapping("/{id}/atualizacoes")
    public List<ServiceOrderEvent> timeline(Authentication authentication, @PathVariable UUID id) {
        return service.timeline(identity(authentication), id);
    }
    @PostMapping("/{id}/atualizacoes")
    public ServiceOrderEvent publish(Authentication authentication, @PathVariable UUID id,
                                     @RequestBody ServiceOrderService.UpdateInput input) {
        return service.publish(identity(authentication), id, input);
    }
    @GetMapping("/{id}/atualizacoes/publicas")
    public List<PublicServiceOrderEvent> publicTimeline(Authentication authentication, @PathVariable UUID id) {
        return service.publicTimeline(identity(authentication), id);
    }
    @PostMapping("/{id}/previsao")
    public ServiceOrder updateForecast(Authentication authentication, @PathVariable UUID id,
                                       @RequestBody ServiceOrderService.ForecastInput input) {
        return service.updateForecast(identity(authentication), id, input);
    }
    @GetMapping("/{id}/previsoes")
    public List<ServiceOrderForecast> forecasts(Authentication authentication, @PathVariable UUID id) {
        return service.forecasts(identity(authentication), id);
    }
    @GetMapping("/{id}/previsoes/publicas")
    public List<PublicServiceOrderForecast> publicForecasts(Authentication authentication, @PathVariable UUID id) {
        return service.publicForecasts(identity(authentication), id);
    }
    private Identidade identity(Authentication authentication) {
        if (authentication == null || !(authentication.getDetails() instanceof Identidade owner)) {
            throw new ApiException(401, "NAO_AUTENTICADO", "Entre novamente.");
        }
        return owner;
    }
}
