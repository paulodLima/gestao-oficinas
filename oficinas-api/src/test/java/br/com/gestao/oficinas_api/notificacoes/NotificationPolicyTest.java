package br.com.gestao.oficinas_api.notificacoes;

import br.com.gestao.oficinas_api.identidade.ApiException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationPolicyTest {
    @Test void selectsOnlyActiveVerifiedRecipients() {
        Timestamp verified = Timestamp.from(Instant.now());
        assertTrue(NotificationService.eligible("client@example.test", verified, true));
        assertFalse(NotificationService.eligible("client@example.test", null, true));
        assertFalse(NotificationService.eligible("client@example.test", verified, false));
        assertFalse(NotificationService.eligible(" ", verified, true));
        assertFalse(NotificationService.eligible(null, verified, true));
    }
    @Test void retriesAtDocumentedIntervalsAndStops() {
        assertEquals(Duration.ofMinutes(1), NotificationWorker.retryDelay(1));
        assertEquals(Duration.ofMinutes(5), NotificationWorker.retryDelay(2));
        assertEquals(Duration.ofMinutes(15), NotificationWorker.retryDelay(3));
        assertEquals(Duration.ofMinutes(60), NotificationWorker.retryDelay(4));
        assertThrows(IllegalArgumentException.class, () -> NotificationWorker.retryDelay(5));
    }
    @Test void missingProviderNeverPretendsSuccess() {
        JavaMailSender sender = mock(JavaMailSender.class);
        assertThrows(ApiException.class, () -> new TransactionalEmail(sender, "")
            .send("client@example.test", "Aviso", "Mensagem"));
        verifyNoInteractions(sender);
        doThrow(new MailSendException("private-provider-detail")).when(sender).send(any(SimpleMailMessage.class));
        ApiException error = assertThrows(ApiException.class, () -> new TransactionalEmail(sender, "office@example.test")
            .send("client@example.test", "Aviso", "Mensagem"));
        assertFalse(error.getMessage().contains("private-provider-detail"));
    }
    @Test void templatesContainOnlyAllowlistedContent() {
        assertEquals(7, NotificationEvent.values().length);
        for (NotificationEvent event : NotificationEvent.values()) {
            assertTrue(event.message(42).startsWith("OS-42 · "));
            assertFalse(event.message(42).contains("http"));
        }
    }
}
