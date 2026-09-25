package br.com.gestao.oficinas_api.portal;

import br.com.gestao.oficinas_api.adicional.AdditionalDecisionService;
import br.com.gestao.oficinas_api.identidade.ClientAddress;
import br.com.gestao.oficinas_api.identidade.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portal/ordens-servico/{orderId}/adicionais")
public class AdditionalDecisionController {
    private final PortalAccessController access;
    private final AdditionalDecisionService decisions;
    private final RateLimit limits;
    private final ClientAddress addresses;

    public AdditionalDecisionController(PortalAccessController access, AdditionalDecisionService decisions,
                                        RateLimit limits, ClientAddress addresses) {
        this.access = access;
        this.decisions = decisions;
        this.limits = limits;
        this.addresses = addresses;
    }

    @GetMapping
    public List<AdditionalDecisionService.PublicRequest> list(HttpSession session,
                                                              @PathVariable UUID orderId) {
        PortalAccessController.Grant grant = access.authorize(session, orderId);
        return decisions.list(grant.office(), access.responsibleCustomer(grant, orderId), orderId);
    }

    @PostMapping("/{requestId}/codigo")
    public AdditionalDecisionService.ChallengeIssued issueCode(HttpSession session,
                                                               @PathVariable UUID orderId,
                                                               @PathVariable UUID requestId, HttpServletRequest request) {
        limits.check("additional-issue-ip:" + addresses.resolve(request), 30);
        PortalAccessController.Grant grant = access.authorize(session, orderId);
        return decisions.issueCode(grant.office(), access.responsibleCustomer(grant, orderId),
            orderId, requestId);
    }

    @PostMapping("/{requestId}/decisoes")
    public AdditionalDecisionService.PublicRequest confirm(HttpSession session,
                                                            @PathVariable UUID orderId,
                                                            @PathVariable UUID requestId,
                                                            @RequestHeader("Idempotency-Key") String key,
                                                            @RequestBody AdditionalDecisionService.Confirmation input,
                                                            HttpServletRequest request) {
        limits.check("additional-confirm-ip:" + addresses.resolve(request), 60);
        PortalAccessController.Grant grant = access.authorize(session, orderId);
        return decisions.confirm(grant.office(), access.responsibleCustomer(grant, orderId),
            orderId, requestId, key, input);
    }
}
