package br.com.gestao.oficinas_api.oficina;

import br.com.gestao.oficinas_api.identidade.*;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ShopController {
    private final ShopService service;
    private final ShopRepository repository;
    public ShopController(ShopService service, ShopRepository repository) {
        this.service = service;
        this.repository = repository;
    }
    @GetMapping("/oficina")
    public ShopProfile get(Authentication authentication) { return service.find(identity(authentication)); }
    @PatchMapping("/oficina")
    public ShopProfile update(Authentication authentication, @RequestBody Map<String, Object> fields) {
        var owner = identity(authentication);
        return service.update(owner, fields);
    }
    @PutMapping(value = "/oficina/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ShopProfile upload(Authentication authentication, @RequestParam long versao, @RequestPart MultipartFile arquivo) {
        var owner = identity(authentication);
        return service.updateLogo(owner, versao, arquivo);
    }
    @DeleteMapping("/oficina/logo")
    public ShopProfile remove(Authentication authentication, @RequestParam long versao) {
        var owner = identity(authentication);
        return service.removeLogo(owner, versao);
    }
    @GetMapping("/oficina/logo")
    public ResponseEntity<byte[]> logo(Authentication authentication) {
        return image(repository.findLogo(identity(authentication).oficinaId()));
    }
    @GetMapping("/publico/oficinas/{slug}")
    public ShopProfile.PublicProfile publicProfile(@PathVariable String slug) {
        return repository.findPublic(slug).publicView();
    }
    @GetMapping("/publico/oficinas/{slug}/logo")
    public ResponseEntity<byte[]> publicLogo(@PathVariable String slug) {
        return image(repository.findPublicLogo(slug));
    }
    private ResponseEntity<byte[]> image(byte[] bytes) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore())
            .header("X-Content-Type-Options", "nosniff").body(bytes);
    }
    private Identidade identity(Authentication authentication) {
        if (authentication == null || !(authentication.getDetails() instanceof Identidade owner))
            throw new ApiException(401, "NAO_AUTENTICADO", "Entre novamente.");
        return owner;
    }
}
