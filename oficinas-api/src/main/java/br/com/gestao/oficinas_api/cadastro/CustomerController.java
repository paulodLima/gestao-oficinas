package br.com.gestao.oficinas_api.cadastro;

import br.com.gestao.oficinas_api.identidade.*;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
public class CustomerController {
    private final CustomerVehicleService service;
    private final CustomerVerificationService verification;
    private final ClientAddress addresses;
    public CustomerController(CustomerVehicleService service, CustomerVerificationService verification, ClientAddress addresses) {
        this.service = service; this.verification = verification; this.addresses = addresses;
    }

    @GetMapping
    public PageResult<Customer> list(Authentication authentication, @RequestParam(defaultValue = "") String q,
                                     @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.customers(identity(authentication), q, page, size);
    }
    @GetMapping("/{id}")
    public Customer get(Authentication authentication, @PathVariable UUID id) { return service.customer(identity(authentication), id); }
    @PostMapping
    public ResponseEntity<Customer> create(Authentication authentication, @RequestBody CustomerVehicleService.CustomerInput input) {
        Customer result = service.createCustomer(identity(authentication), input);
        return ResponseEntity.created(URI.create("/api/clientes/" + result.id())).body(result);
    }
    @PatchMapping("/{id}")
    public Customer update(Authentication authentication, @PathVariable UUID id, @RequestBody Map<String, Object> fields) {
        return service.updateCustomer(identity(authentication), id, fields);
    }
    @PostMapping("/{id}/verificacao")
    public ResponseEntity<CustomerVerificationService.Challenge> issue(Authentication authentication, @PathVariable UUID id,
                                                                        HttpServletRequest request) {
        return ResponseEntity.accepted().body(verification.issue(identity(authentication), id, addresses.resolve(request)));
    }
    @PostMapping("/{id}/verificacao/confirmacao")
    public Customer confirm(Authentication authentication, @PathVariable UUID id, @RequestBody Confirmation input) {
        return verification.confirm(identity(authentication), id, input.desafioId(), input.codigo());
    }
    public record Confirmation(UUID desafioId, String codigo) {}
    private Identidade identity(Authentication authentication) {
        if (authentication == null || !(authentication.getDetails() instanceof Identidade owner))
            throw new ApiException(401, "NAO_AUTENTICADO", "Entre novamente.");
        return owner;
    }
}
