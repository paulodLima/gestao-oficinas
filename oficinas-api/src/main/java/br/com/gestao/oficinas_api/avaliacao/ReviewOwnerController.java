package br.com.gestao.oficinas_api.avaliacao;

import br.com.gestao.oficinas_api.cadastro.PageResult;
import br.com.gestao.oficinas_api.identidade.*;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ReviewOwnerController {
    private final ReviewService service;
    private final ReviewAccessService access;
    public ReviewOwnerController(ReviewService service, ReviewAccessService access) { this.service = service; this.access = access; }
    @GetMapping("/avaliacoes")
    public PageResult<ReviewService.OwnerReview> list(Authentication auth, @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return service.list(identity(auth).oficinaId(), page, size);
    }
    @GetMapping("/avaliacoes/configuracao")
    public ReviewService.Configuration configuration(Authentication auth) { return service.configuration(identity(auth).oficinaId()); }
    @PatchMapping("/avaliacoes/configuracao")
    public ReviewService.Configuration configure(Authentication auth, @RequestBody ReviewService.Configuration input) {
        return service.configure(identity(auth).oficinaId(), input.googleUrl());
    }
    @PostMapping("/ordens-servico/{id}/avaliacao/convite")
    public ReviewAccessService.Invitation invitation(Authentication auth, @PathVariable UUID id) { return access.invitation(identity(auth), id); }
    @DeleteMapping("/ordens-servico/{id}/avaliacao/convite") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(Authentication auth, @PathVariable UUID id) { access.revoke(identity(auth), id); }
    private Identidade identity(Authentication auth) {
        if (auth == null || !(auth.getDetails() instanceof Identidade owner)) throw new ApiException(401, "NAO_AUTENTICADO", "Entre novamente.");
        return owner;
    }
}
