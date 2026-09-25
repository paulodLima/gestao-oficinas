package br.com.gestao.oficinas_api.adicional;

import br.com.gestao.oficinas_api.identidade.ApiException;
import br.com.gestao.oficinas_api.identidade.Identidade;
import br.com.gestao.oficinas_api.ordem.ServiceOrder;
import br.com.gestao.oficinas_api.ordem.ServiceOrderService;
import br.com.gestao.oficinas_api.ordem.ServiceOrderStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdditionalRequestServiceTest {
    @Mock AdditionalRequestRepository repository;
    @Mock ServiceOrderService orders;
    private AdditionalRequestService service;
    private final UUID ownerId = UUID.randomUUID();
    private final UUID shopId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID requestId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final Identidade owner = new Identidade(ownerId, shopId, 1,
        Instant.parse("2026-09-25T12:00:00Z"));

    @BeforeEach void setUp() {
        service = new AdditionalRequestService(repository, orders, new AdditionalPolicy(),
            Clock.fixed(Instant.parse("2026-09-25T12:00:00Z"), ZoneOffset.UTC));
        when(orders.order(owner, orderId)).thenReturn(order(ServiceOrderStatus.EM_DIAGNOSTICO));
    }

    @Test void validatesAndCalculatesDraftBeforePersisting() {
        var input = draft(null);
        when(repository.create(eq(shopId), eq(ownerId), eq(orderId), any())).thenReturn(request());
        service.create(owner, orderId, input);
        verify(repository).create(eq(shopId), eq(ownerId), eq(orderId), argThat(data ->
            data.items().getFirst().total().equals(new BigDecimal("22.49"))));
    }

    @Test void sendsOnlyCurrentDraftAndRequiresPublishedPhotos() {
        UUID photoId = UUID.randomUUID();
        var current = request(photoId);
        when(repository.request(shopId, orderId, requestId)).thenReturn(current);
        when(repository.send(shopId, ownerId, orderId, requestId, 2, versionId)).thenReturn(current);
        service.send(owner, orderId, requestId, new AdditionalRequestService.VersionInput(2L));
        verify(repository).validatePhotos(shopId, orderId, List.of(photoId), true);
        verify(repository).send(shopId, ownerId, orderId, requestId, 2, versionId);
    }

    @Test void replacesSentVersionWithoutOverwritingHistory() {
        var sent = request(AdditionalRequest.Status.ENVIADA, AdditionalRequest.VersionStatus.ENVIADA);
        when(repository.request(shopId, orderId, requestId)).thenReturn(sent);
        when(repository.replace(eq(shopId), eq(ownerId), eq(orderId), eq(requestId), eq(2L),
            any(), eq("Preço atualizado"), any())).thenReturn(sent);
        var input = new AdditionalRequestService.ReplacementInput("Problema", "Justificativa", null,
            "Mais um dia", List.of(), draft(null).itens(), "Preço atualizado", 2L);
        service.replace(owner, orderId, requestId, input);
        verify(repository).replace(eq(shopId), eq(ownerId), eq(orderId), eq(requestId), eq(2L),
            argThat(version -> version.numero() == 1), eq("Preço atualizado"), any());
    }

    @Test void rejectsWritesWhenServiceOrderIsClosed() {
        when(orders.order(owner, orderId)).thenReturn(order(ServiceOrderStatus.ENTREGUE));
        ApiException error = assertThrows(ApiException.class,
            () -> service.create(owner, orderId, draft(null)));
        assertEquals("ORDEM_ENCERRADA", error.code);
        verifyNoInteractions(repository);
    }

    private AdditionalRequestService.DraftInput draft(Long expectedVersion) {
        return new AdditionalRequestService.DraftInput("Problema", "Justificativa",
            Instant.parse("2026-09-26T12:00:00Z"), "Mais um dia", List.of(),
            List.of(new AdditionalRequestService.ItemInput(AdditionalRequest.ItemType.PECA,
                "Disco", new BigDecimal("1.125"), new BigDecimal("19.99"), null)), expectedVersion);
    }

    private AdditionalRequest request(UUID... photos) {
        return request(AdditionalRequest.Status.RASCUNHO, AdditionalRequest.VersionStatus.RASCUNHO,
            List.of(photos));
    }

    private AdditionalRequest request(AdditionalRequest.Status status,
                                      AdditionalRequest.VersionStatus versionStatus) {
        return request(status, versionStatus, List.of());
    }

    private AdditionalRequest request(AdditionalRequest.Status status,
                                      AdditionalRequest.VersionStatus versionStatus,
                                      List<UUID> photos) {
        var item = new AdditionalRequest.Item(UUID.randomUUID(), AdditionalRequest.ItemType.PECA,
            "Disco", BigDecimal.ONE, new BigDecimal("100.00"), new BigDecimal("100.00"), null, 0);
        var version = new AdditionalRequest.Version(versionId, 1, versionStatus, "Problema",
            "Justificativa", null, "Mais um dia", null, new BigDecimal("100.00"), photos,
            List.of(item), null, null, Instant.now(), Instant.now());
        return new AdditionalRequest(requestId, orderId, status, 2, null, List.of(version),
            Instant.now(), Instant.now());
    }

    private ServiceOrder order(ServiceOrderStatus status) {
        return new ServiceOrder(orderId, 1, UUID.randomUUID(), "Ana", UUID.randomUUID(), "BRA1E23",
            "Volkswagen T-Cross", "Ruído", Instant.now(), 1000, status, null, false, false,
            0, Instant.now(), Instant.now());
    }
}
