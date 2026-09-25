package br.com.gestao.oficinas_api.avaliacao;

import br.com.gestao.oficinas_api.identidade.*;
import jakarta.servlet.http.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portal/avaliacoes")
public class ReviewController {
    private static final String SESSION_KEY = "AVALIACAO_ENTREGA";
    private final ReviewService service;
    private final ReviewAccessService access;
    private final RateLimit limits;
    private final ClientAddress addresses;
    public ReviewController(ReviewService service, ReviewAccessService access, RateLimit limits, ClientAddress addresses) {
        this.service = service; this.access = access; this.limits = limits; this.addresses = addresses;
    }
    @PostMapping("/acesso") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void exchange(@RequestBody TokenInput input, HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.removeAttribute(SESSION_KEY);
        limits.check("review-link:" + addresses.resolve(request), 60);
        var grant = access.exchange(input == null ? null : input.token());
        request.changeSessionId();
        session.setAttribute(SESSION_KEY, grant.id());
    }
    @GetMapping("/resumo") public ReviewService.Summary summary(HttpSession session) { return service.summary(grant(session)); }
    @PostMapping public ReviewService.Review submit(HttpSession session, @RequestBody Submission input) {
        UUID id = grant(session);
        limits.check("review-submit:" + id, 30);
        if (input == null || !id.equals(input.contexto())) throw new ApiException(409, "AVALIACAO_CONTEXTO_ALTERADO",
            "O convite mudou em outra aba. Reabra o convite do atendimento que deseja avaliar.");
        return service.submit(id, new ReviewPolicy.Input(input.nota(), input.comentario(), input.consentimentoPublicacao()));
    }
    @PostMapping("/sair") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(HttpSession session) { session.removeAttribute(SESSION_KEY); }
    private UUID grant(HttpSession session) {
        if (!(session.getAttribute(SESSION_KEY) instanceof UUID id)) throw ReviewAccessService.unavailable();
        return id;
    }
    public record TokenInput(String token) {}
    public record Submission(UUID contexto, BigDecimal nota, String comentario, Boolean consentimentoPublicacao) {}
}
