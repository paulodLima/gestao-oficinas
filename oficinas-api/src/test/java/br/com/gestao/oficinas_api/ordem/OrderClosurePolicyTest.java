package br.com.gestao.oficinas_api.ordem;

import br.com.gestao.oficinas_api.identidade.ApiException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderClosurePolicyTest {
    final OrderClosurePolicy policy = new OrderClosurePolicy();
    @Test void readyIsActiveButDeliveredAndCanceledAreTerminal() {
        assertTrue(ServiceOrderStatus.PRONTO_PARA_RETIRADA.active());
        assertFalse(ServiceOrderStatus.ENTREGUE.active());
        assertFalse(ServiceOrderStatus.CANCELADO.active());
        assertThrows(ApiException.class, () -> policy.validate(new OrderClosurePolicy.Input(
            ServiceOrderStatus.PRONTO_PARA_RETIRADA, null, true, false, 0L)));
    }
    @Test void requiresExplicitConfirmationAndCancellationReason() {
        assertThrows(ApiException.class, () -> policy.validate(new OrderClosurePolicy.Input(
            ServiceOrderStatus.ENTREGUE, null, false, false, 0L)));
        assertThrows(ApiException.class, () -> policy.validate(new OrderClosurePolicy.Input(
            ServiceOrderStatus.CANCELADO, " ", true, false, 0L)));
        assertEquals("Solicitado pelo cliente", policy.validate(new OrderClosurePolicy.Input(
            ServiceOrderStatus.CANCELADO, " Solicitado pelo cliente ", true, false, 0L)));
        assertNull(policy.validate(new OrderClosurePolicy.Input(ServiceOrderStatus.ENTREGUE, null, true, false, 0L)));
    }
    @Test void requiresSeparatePendingConsent() {
        assertThrows(ApiException.class, () -> policy.requirePendingConsent(1, false));
        assertDoesNotThrow(() -> policy.requirePendingConsent(1, true));
        assertDoesNotThrow(() -> policy.requirePendingConsent(0, false));
    }
}
