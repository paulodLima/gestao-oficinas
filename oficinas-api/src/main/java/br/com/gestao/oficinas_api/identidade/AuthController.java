package br.com.gestao.oficinas_api.identidade;

import java.time.Clock;
import java.util.Map;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService service;
    private final RateLimit limits;
    private final IdentidadeRepository repository;
    private final SecurityContextRepository contexts;
    private final Clock clock;
    private final ClientAddress addresses;
    public AuthController(AuthService service,RateLimit limits,IdentidadeRepository repository,
                          SecurityContextRepository contexts,Clock clock,ClientAddress addresses) {
        this.service=service;this.limits=limits;this.repository=repository;this.contexts=contexts;this.clock=clock;
        this.addresses=addresses;
    }
    public record Cadastro(@NotBlank @Size(max=120) String nome, @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(max=100) String senha, @NotBlank @Size(max=120) String nomeOficina) {}
    public record Login(@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(max=100) String senha) {}
    public record Recuperacao(@NotBlank @Email @Size(max=254) String email) {}
    public record Redefinicao(@NotBlank @Size(max=100) String token,@NotBlank @Size(max=100) String novaSenha) {}
    public record OficinaView(java.util.UUID id,String nome) {}
    public record Me(java.util.UUID id,String nome,String email,OficinaView oficina) {}
    private Me view(IdentidadeRepository.Proprietario o) { return new Me(o.id(),o.nome(),o.email(),new OficinaView(o.oficinaId(),o.oficinaNome())); }

    @GetMapping("/auth/csrf") public Map<String,String> csrf(CsrfToken token) {
        return Map.of("token",token.getToken(),"headerName",token.getHeaderName());
    }
    @PostMapping("/auth/cadastro") public ResponseEntity<Me> register(@Valid @RequestBody Cadastro body,HttpServletRequest req) {
        limits.check("register:"+addresses.resolve(req),20);
        var o=service.register(body.nome(),body.email(),body.senha(),body.nomeOficina());
        return ResponseEntity.status(201).body(view(o));
    }
    @PostMapping("/auth/login") public Me login(@Valid @RequestBody Login body,HttpServletRequest req,HttpServletResponse res) {
        limits.check("login-ip:"+addresses.resolve(req),100);
        limits.check("login-email:"+AuthService.normalize(body.email()),15);
        var o=service.authenticate(body.email(),body.senha());
        var old=req.getSession(false); if(old!=null)old.invalidate();
        req.getSession(true);
        var auth=UsernamePasswordAuthenticationToken.authenticated(o.id().toString(),null,
            AuthorityUtils.createAuthorityList("ROLE_PROPRIETARIO"));
        auth.setDetails(new Identidade(o.id(),o.oficinaId(),o.authVersion(),clock.instant()));
        var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);contexts.saveContext(context,req,res);
        return view(o);
    }
    @GetMapping("/auth/me") public Me me(Authentication auth) { return view(owner(auth)); }
    @GetMapping("/oficina") public OficinaView shop(Authentication auth) {
        var o=owner(auth);return new OficinaView(o.oficinaId(),o.oficinaNome());
    }
    private IdentidadeRepository.Proprietario owner(Authentication auth) {
        if(!(auth.getDetails() instanceof Identidade identity))throw new ApiException(401,"NAO_AUTENTICADO","Entre novamente.");
        return repository.porIdentidade(identity).orElseThrow(()->new ApiException(401,"NAO_AUTENTICADO","Entre novamente."));
    }
    @PostMapping("/auth/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest req) {
        var session=req.getSession(false);if(session!=null)session.invalidate();SecurityContextHolder.clearContext();
    }
    @PostMapping("/auth/recuperacao") @ResponseStatus(HttpStatus.ACCEPTED)
    public void recover(@Valid @RequestBody Recuperacao body,HttpServletRequest req) {
        limits.check("recovery-ip:"+addresses.resolve(req),30);
        limits.check("recovery-email:"+AuthService.normalize(body.email()),5);
        try { service.recover(body.email()); }
        catch(ApiException e) {
            if(e.status!=503) throw e;
            // No address, token or provider exception in logs; preserve the generic public response.
            org.slf4j.LoggerFactory.getLogger(AuthController.class)
                .warn("PASSWORD_RECOVERY_EMAIL_UNAVAILABLE: check transactional mail configuration/provider");
        }
    }
    @PostMapping("/auth/redefinicao") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody Redefinicao body,HttpServletRequest req) {
        limits.check("reset:"+addresses.resolve(req),30);service.reset(body.token(),body.novaSenha());
    }
}
