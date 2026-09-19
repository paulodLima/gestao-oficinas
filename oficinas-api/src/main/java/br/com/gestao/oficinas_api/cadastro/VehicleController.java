package br.com.gestao.oficinas_api.cadastro;

import br.com.gestao.oficinas_api.identidade.*;
import java.net.URI;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/veiculos")
public class VehicleController {
    private final CustomerVehicleService service;
    public VehicleController(CustomerVehicleService service) { this.service = service; }

    @GetMapping
    public PageResult<Vehicle> list(Authentication authentication, @RequestParam(defaultValue = "") String q,
                                    @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.vehicles(identity(authentication), q, page, size);
    }
    @GetMapping("/{id}")
    public Vehicle get(Authentication authentication, @PathVariable UUID id) { return service.vehicle(identity(authentication), id); }
    @PostMapping
    public ResponseEntity<Vehicle> create(Authentication authentication, @RequestBody CustomerVehicleService.VehicleInput input) {
        Vehicle result = service.createVehicle(identity(authentication), input);
        return ResponseEntity.created(URI.create("/api/veiculos/" + result.id())).body(result);
    }
    @PatchMapping("/{id}")
    public Vehicle update(Authentication authentication, @PathVariable UUID id, @RequestBody Map<String, Object> fields) {
        return service.updateVehicle(identity(authentication), id, fields);
    }
    @PostMapping("/{id}/transferencias")
    public Vehicle transfer(Authentication authentication, @PathVariable UUID id, @RequestBody Transfer input) {
        return service.transfer(identity(authentication), id, input.novoClienteId(), input.expectedVersion());
    }
    public record Transfer(UUID novoClienteId, long expectedVersion) {}
    private Identidade identity(Authentication authentication) {
        if (authentication == null || !(authentication.getDetails() instanceof Identidade owner))
            throw new ApiException(401, "NAO_AUTENTICADO", "Entre novamente.");
        return owner;
    }
}
