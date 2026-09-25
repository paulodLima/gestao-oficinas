package br.com.gestao.oficinas_api.identidade;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionSecurity {
    public ProductionSecurity(AuthProperties properties) {
        if (!properties.secureCookie()) {
            throw new IllegalArgumentException("Produção exige cookie seguro e origem HTTPS");
        }
    }
}
