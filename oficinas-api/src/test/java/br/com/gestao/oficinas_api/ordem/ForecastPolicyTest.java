package br.com.gestao.oficinas_api.ordem;

import java.time.Instant;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ForecastPolicyTest {
    private final Instant now = Instant.parse("2026-09-19T15:00:00Z");
    @Test void deadlineIsStrictAndNullIsNotLate() {
        assertFalse(ForecastPolicy.isLate(ServiceOrderStatus.EM_TESTES, null, now));
        assertFalse(ForecastPolicy.isLate(ServiceOrderStatus.EM_TESTES, now, now));
        assertFalse(ForecastPolicy.isLate(ServiceOrderStatus.EM_TESTES, now.plusSeconds(1), now));
        assertTrue(ForecastPolicy.isLate(ServiceOrderStatus.EM_TESTES, now.minusNanos(1), now));
    }
    @Test void readyAndClosedAreNotExecutionDelays() {
        for (var status : ServiceOrderStatus.values()) {
            boolean expected = status.active() && status != ServiceOrderStatus.PRONTO_PARA_RETIRADA;
            assertEquals(expected, ForecastPolicy.isLate(status, now.minusSeconds(1), now), status.name());
        }
    }
    @Test void timezoneOffsetsRepresentTheSameDeadline() {
        Instant local = OffsetDateTime.parse("2026-09-19T12:00:00-03:00").toInstant();
        assertEquals(now, local);
        assertFalse(ForecastPolicy.isLate(ServiceOrderStatus.RECEBIDO, local, now));
        assertTrue(ForecastPolicy.isLate(ServiceOrderStatus.RECEBIDO, local, now.plusSeconds(1)));
    }
}
