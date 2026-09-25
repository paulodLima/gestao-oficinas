package br.com.gestao.oficinas_api.adicional;

import br.com.gestao.oficinas_api.identidade.ApiException;
import br.com.gestao.oficinas_api.identidade.AuthProperties;
import br.com.gestao.oficinas_api.notificacoes.TransactionalEmail;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AdditionalDecisionServiceTest {
    @Mock JdbcTemplate jdbc;
    @Mock TransactionalEmail email;
    @Mock br.com.gestao.oficinas_api.notificacoes.NotificationService notifications;

    @Test void rejectsConfirmationWithoutIdempotencyKeyBeforeDatabaseAccess() {
        AdditionalDecisionService service = service();
        var input = new AdditionalDecisionService.Confirmation(UUID.randomUUID(), "123456", 1,
            List.of(new AdditionalDecisionService.BlockDecision("item:1",
                AdditionalDecisionService.Decision.APROVADO)), null);
        ApiException error = assertThrows(ApiException.class, () -> service.confirm(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "", input));
        assertEquals("DECISAO_INVALIDA", error.code);
        verifyNoInteractions(jdbc, email);
    }

    @Test void rejectsMalformedConfirmationBeforeDatabaseAccess() {
        AdditionalDecisionService service = service();
        var invalid = new AdditionalDecisionService.Confirmation(null, "12", 1, List.of(), null);
        ApiException error = assertThrows(ApiException.class, () -> service.confirm(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "attempt", invalid));
        assertEquals("DECISAO_INVALIDA", error.code);
        verifyNoInteractions(jdbc, email);
    }

    private AdditionalDecisionService service() {
        return new AdditionalDecisionService(jdbc, email,
            new AuthProperties(URI.create("http://localhost:8080"), false,
                "segredo-de-testes-com-tamanho-suficiente"),
            new AdditionalDecisionPolicy(),
            Clock.fixed(Instant.parse("2026-09-25T12:00:00Z"), ZoneOffset.UTC), notifications,
            org.mockito.Mockito.mock(br.com.gestao.oficinas_api.identidade.RateLimit.class));
    }
}
