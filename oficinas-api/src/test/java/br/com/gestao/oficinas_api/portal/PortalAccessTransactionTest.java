package br.com.gestao.oficinas_api.portal;

import br.com.gestao.oficinas_api.identidade.ApiException;
import br.com.gestao.oficinas_api.identidade.AuthProperties;
import br.com.gestao.oficinas_api.identidade.ClientAddress;
import br.com.gestao.oficinas_api.identidade.RateLimit;
import br.com.gestao.oficinas_api.ordem.PhotoStorage;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PortalAccessTransactionTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final HttpSession session = mock(HttpSession.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final SimpleTransactionStatus transaction = new SimpleTransactionStatus();
    private final UUID challengeId = UUID.randomUUID();
    private PortalAccessController controller;

    @BeforeEach
    void setUp() throws Exception {
        Instant now = Instant.parse("2026-09-25T12:00:00Z");
        var target = new PortalAccessController(jdbc, mock(PortalChallengeIssuer.class),
            new AuthProperties(URI.create("http://localhost:8080"), false, "test-only-secret-at-least-24-characters"),
            Clock.fixed(now, ZoneOffset.UTC), mock(PhotoStorage.class), new PortalAccessPolicy(),
            mock(RateLimit.class), mock(ClientAddress.class));
        var advice = new TransactionInterceptor();
        advice.setTransactionManager(transactions);
        advice.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        var proxy = new ProxyFactory(target);
        proxy.addAdvice(advice);
        controller = (PortalAccessController) proxy.getProxy();
        when(transactions.getTransaction(any())).thenReturn(transaction);
        ResultSet row = mock(ResultSet.class);
        when(row.getObject(1, UUID.class)).thenReturn(UUID.randomUUID());
        when(row.getObject(2, UUID.class)).thenReturn(UUID.randomUUID());
        when(row.getString(3)).thenReturn("BRA1E23");
        when(row.getString(4)).thenReturn("0".repeat(64));
        when(row.getTimestamp(5)).thenReturn(Timestamp.from(now.plusSeconds(600)));
        doAnswer(invocation -> {
            RowMapper<?> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(row, 0));
        }).when(jdbc).query(anyString(), any(RowMapper.class), eq(challengeId));
    }

    @Test
    void commitsFailedAttemptWhileRejectingCodeAndLeavingSessionUnauthenticated() {
        ApiException error = assertThrows(ApiException.class, () -> controller.validateCode(
            new PortalAccessController.Validate(challengeId, "123456"), request));
        assertEquals("CODIGO_INVALIDO", error.code);
        verify(jdbc).update(contains("tentativas=tentativas+1"), eq(challengeId), eq(5));
        verify(transactions).commit(transaction);
        verify(transactions, never()).rollback(any());
        verifyNoInteractions(session);
    }

    @Test
    void rollsBackUnexpectedPersistenceFailure() {
        doThrow(new DataAccessResourceFailureException("database unavailable"))
            .when(jdbc).update(contains("tentativas=tentativas+1"), eq(challengeId), eq(5));
        assertThrows(DataAccessResourceFailureException.class, () -> controller.validateCode(
            new PortalAccessController.Validate(challengeId, "123456"), request));
        verify(transactions).rollback(transaction);
        verify(transactions, never()).commit(any());
        verifyNoInteractions(session);
    }
}
