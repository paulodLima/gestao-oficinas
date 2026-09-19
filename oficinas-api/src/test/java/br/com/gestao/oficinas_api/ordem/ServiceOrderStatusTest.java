package br.com.gestao.oficinas_api.ordem;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServiceOrderStatusTest {
    @Test void classifiesOnlyDeliveredAndCanceledAsTerminal() {
        assertTrue(ServiceOrderStatus.RECEBIDO.active());
        assertTrue(ServiceOrderStatus.PRONTO_PARA_RETIRADA.active());
        assertFalse(ServiceOrderStatus.ENTREGUE.active());
        assertFalse(ServiceOrderStatus.CANCELADO.active());
    }

    @Test void allowsForwardJumpsWithoutReason() {
        assertFalse(ServiceOrderStatus.EM_TESTES.requiresReasonFrom(ServiceOrderStatus.RECEBIDO));
        assertFalse(ServiceOrderStatus.PINTURA.requiresReasonFrom(ServiceOrderStatus.EM_DIAGNOSTICO));
    }

    @Test void requiresReasonForReturnsAndWaitingFromExecution() {
        assertTrue(ServiceOrderStatus.EM_DIAGNOSTICO.requiresReasonFrom(ServiceOrderStatus.EM_TESTES));
        assertTrue(ServiceOrderStatus.AGUARDANDO_PECAS.requiresReasonFrom(ServiceOrderStatus.EM_MANUTENCAO));
        assertFalse(ServiceOrderStatus.AGUARDANDO_APROVACAO.requiresReasonFrom(ServiceOrderStatus.RECEBIDO));
    }

    @Test void classifiesDelayOnlyDuringExecution() {
        Instant reference = Instant.parse("2026-09-19T15:00:00Z");
        Instant overdue = reference.minusSeconds(1);
        assertTrue(ServiceOrderStatus.EM_MANUTENCAO.isDelayed(overdue, reference));
        assertFalse(ServiceOrderStatus.EM_MANUTENCAO.isDelayed(reference, reference));
        assertFalse(ServiceOrderStatus.PRONTO_PARA_RETIRADA.isDelayed(overdue, reference));
        assertFalse(ServiceOrderStatus.ENTREGUE.isDelayed(overdue, reference));
        assertFalse(ServiceOrderStatus.CANCELADO.isDelayed(overdue, reference));
        assertFalse(ServiceOrderStatus.EM_TESTES.isDelayed(null, reference));
        assertTrue(ServiceOrderStatus.PRONTO_PARA_RETIRADA.awaitingPickup());
        assertFalse(ServiceOrderStatus.EM_MANUTENCAO.awaitingPickup());
    }
}
