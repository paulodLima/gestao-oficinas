package br.com.gestao.oficinas_api.identidade;

import java.net.URI;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductionSecurityTest {
    private static final String SECRET = "test-only-exclusive-secret-longer-than-32-characters";
    @Test void rejectsInsecureProductionAndDevelopmentSecrets() {
        assertThrows(IllegalArgumentException.class,
            () -> new AuthProperties(URI.create("http://example.test"), true, SECRET));
        assertThrows(IllegalArgumentException.class,
            () -> new AuthProperties(URI.create("https://example.test"), true, "development-only-secret-change-me"));
        assertThrows(IllegalArgumentException.class,
            () -> new ProductionSecurity(new AuthProperties(URI.create("https://example.test"), false, SECRET)));
        assertDoesNotThrow(() -> new ProductionSecurity(new AuthProperties(URI.create("https://example.test"), true, SECRET)));
    }
    @Test void rejectsSensitiveOrAmbiguousOriginComponents() {
        for (String origin : new String[]{"https://user:password@example.test", "https://example.test?token=secret",
            "https://example.test#token=secret", "https://example.test/path"}) {
            var error = assertThrows(IllegalArgumentException.class,
                () -> new AuthProperties(URI.create(origin), true, SECRET));
            assertFalse(error.getMessage().contains(origin));
            assertFalse(error.getMessage().contains(SECRET));
        }
    }
}
