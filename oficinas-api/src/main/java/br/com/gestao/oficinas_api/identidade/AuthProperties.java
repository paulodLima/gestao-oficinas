package br.com.gestao.oficinas_api.identidade;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.auth")
public record AuthProperties(URI publicUrl, boolean secureCookie) {
    public AuthProperties {
        if (publicUrl == null || publicUrl.getHost() == null
            || !java.util.Set.of("http", "https").contains(publicUrl.getScheme())) {
            throw new IllegalArgumentException("app.auth.public-url deve ser uma URL HTTP válida");
        }
        if (secureCookie && !"https".equals(publicUrl.getScheme())) {
            throw new IllegalArgumentException("HTTPS obrigatório quando secure-cookie=true");
        }
    }
}

