package br.com.gestao.oficinas_api.ordem;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServiceOrderStatusTest {
    @Test void classifiesOnlyDeliveredAndCanceledAsTerminal() {
        assertTrue(ServiceOrderStatus.RECEBIDO.active());
        assertTrue(ServiceOrderStatus.PRONTO_PARA_RETIRADA.active());
        assertFalse(ServiceOrderStatus.ENTREGUE.active());
        assertFalse(ServiceOrderStatus.CANCELADO.active());
    }
}
