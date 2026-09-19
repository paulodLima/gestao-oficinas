package br.com.gestao.oficinas_api.oficina;

import br.com.gestao.oficinas_api.identidade.*;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ShopService {
    private static final Set<String> FIELDS = Set.of("nome", "telefone", "emailContato", "endereco", "horario", "fuso", "perfilPublico", "versao");
    private final ShopRepository repository;
    private final IdentidadeRepository identities;
    private final LogoValidator logos;
    public ShopService(ShopRepository repository, IdentidadeRepository identities, LogoValidator logos) {
        this.repository = repository;
        this.identities = identities;
        this.logos = logos;
    }
    public ShopProfile find(Identidade identity) { return repository.find(identity.oficinaId()); }
    @Transactional
    public ShopProfile update(Identidade identity, Map<String, Object> fields) {
        if (!FIELDS.containsAll(fields.keySet())) throw invalid("Campo não permitido.");
        var current = find(identity);
        long version = version(fields.get("versao"));
        String name = text(fields, "nome", current.nome(), 120);
        String timezone = text(fields, "fuso", current.fuso(), 80);
        String email = text(fields, "emailContato", current.emailContato(), 254);
        String phone = text(fields, "telefone", current.telefone(), 30);
        if (name.isBlank()) throw invalid("Informe o nome da oficina.");
        if (!ZoneId.getAvailableZoneIds().contains(timezone)) throw invalid("Informe um fuso IANA válido.");
        if (!email.isEmpty() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw invalid("Informe um e-mail válido.");
        if (!phone.isEmpty() && !phone.matches("[+0-9() .-]{8,30}")) throw invalid("Informe um telefone válido.");
        Object visible = fields.getOrDefault("perfilPublico", current.perfilPublico());
        if (!(visible instanceof Boolean)) throw invalid("Publicação inválida.");
        repository.save(new ShopProfile(current.id(), current.slug(), name, phone, email,
            text(fields, "endereco", current.endereco(), 500), text(fields, "horario", current.horario(), 1000),
            timezone, (Boolean) visible, current.temLogo(), version));
        identities.auditar(identity.id(), identity.oficinaId(), "OFICINA_EDITADA");
        return find(identity);
    }
    @Transactional
    public ShopProfile updateLogo(Identidade identity, long version, MultipartFile file) {
        repository.saveLogo(identity.oficinaId(), version, logos.normalize(file));
        identities.auditar(identity.id(), identity.oficinaId(), "LOGO_ALTERADA");
        return find(identity);
    }
    @Transactional
    public ShopProfile removeLogo(Identidade identity, long version) {
        repository.saveLogo(identity.oficinaId(), version, null);
        identities.auditar(identity.id(), identity.oficinaId(), "LOGO_REMOVIDA");
        return find(identity);
    }
    private String text(Map<String, Object> fields, String key, String fallback, int limit) {
        if (!fields.containsKey(key)) return fallback;
        if (!(fields.get(key) instanceof String value) || value.length() > limit) throw invalid("Confira o campo " + key + ".");
        return value.strip();
    }
    private long version(Object value) {
        if (!(value instanceof Number number) || number.longValue() < 0 || number.doubleValue() != number.longValue())
            throw invalid("Informe a versão dos dados.");
        return number.longValue();
    }
    private ApiException invalid(String detail) { return new ApiException(400, "DADOS_INVALIDOS", detail); }
}
