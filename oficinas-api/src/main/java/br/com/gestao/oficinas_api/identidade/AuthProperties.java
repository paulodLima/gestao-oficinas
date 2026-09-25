package br.com.gestao.oficinas_api.identidade;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.auth")
public record AuthProperties(URI publicUrl, boolean secureCookie, String codeSecret) {
    public AuthProperties {
        if (publicUrl == null || publicUrl.getHost() == null
            || publicUrl.getUserInfo() != null || publicUrl.getQuery() != null || publicUrl.getFragment() != null
            || !(publicUrl.getPath().isEmpty() || "/".equals(publicUrl.getPath()))
            || !java.util.Set.of("http", "https").contains(publicUrl.getScheme())) {
            throw new IllegalArgumentException("app.auth.public-url deve ser uma URL HTTP válida");
        }
        if (secureCookie && !"https".equals(publicUrl.getScheme())) {
            throw new IllegalArgumentException("HTTPS obrigatório quando secure-cookie=true");
        }
        if (codeSecret == null || codeSecret.length() < 24) {
            throw new IllegalArgumentException("app.auth.code-secret deve ter ao menos 24 caracteres");
        }
        if (secureCookie && (codeSecret.length() < 32 || codeSecret.equals("development-only-secret-change-me"))) {
            throw new IllegalArgumentException("Produção exige segredo exclusivo com ao menos 32 caracteres");
        }
    }
}

