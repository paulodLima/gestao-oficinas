package br.com.gestao.oficinas_api.adicional;

import br.com.gestao.oficinas_api.identidade.ApiException;
import br.com.gestao.oficinas_api.identidade.Identidade;
import br.com.gestao.oficinas_api.ordem.ServiceOrder;
import br.com.gestao.oficinas_api.ordem.ServiceOrderService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdditionalRequestService {
    private static final int MAX_ITEMS = 50;
    private static final int MAX_PHOTOS = 10;
    private final AdditionalRequestRepository repository;
    private final ServiceOrderService orders;
    private final AdditionalPolicy policy;
    private final Clock clock;

    public AdditionalRequestService(AdditionalRequestRepository repository, ServiceOrderService orders,
                                    AdditionalPolicy policy, Clock clock) {
        this.repository = repository;
        this.orders = orders;
        this.policy = policy;
        this.clock = clock;
    }

    public List<AdditionalRequest> list(Identidade owner, UUID orderId) {
        orders.order(owner, orderId);
        return repository.list(owner.oficinaId(), orderId);
    }

    @Transactional
    public AdditionalRequest create(Identidade owner, UUID orderId, DraftInput input) {
        requireActive(owner, orderId);
        AdditionalRequestRepository.DraftData data = validate(input);
        repository.validatePhotos(owner.oficinaId(), orderId, data.photoIds(), false);
        return repository.create(owner.oficinaId(), owner.id(), orderId, data);
    }

    @Transactional
    public AdditionalRequest edit(Identidade owner, UUID orderId, UUID requestId, DraftInput input) {
        requireActive(owner, orderId);
        if (input == null || input.expectedVersion() == null) throw invalid("Informe a versão atual.");
        AdditionalRequest current = repository.request(owner.oficinaId(), orderId, requestId);
        requireState(current, AdditionalRequest.Status.RASCUNHO, "Somente rascunhos podem ser editados.");
        AdditionalRequest.Version version = currentVersion(current);
        AdditionalRequestRepository.DraftData data = validate(input);
        repository.validatePhotos(owner.oficinaId(), orderId, data.photoIds(), false);
        return repository.edit(owner.oficinaId(), owner.id(), orderId, requestId,
            input.expectedVersion(), version.id(), data);
    }

    @Transactional
    public AdditionalRequest send(Identidade owner, UUID orderId, UUID requestId, VersionInput input) {
        requireActive(owner, orderId);
        if (input == null || input.expectedVersion() == null) throw invalid("Informe a versão atual.");
        AdditionalRequest current = repository.request(owner.oficinaId(), orderId, requestId);
        requireState(current, AdditionalRequest.Status.RASCUNHO, "A solicitação já foi enviada.");
        AdditionalRequest.Version version = currentVersion(current);
        repository.validatePhotos(owner.oficinaId(), orderId, version.fotoIds(), true);
        return repository.send(owner.oficinaId(), owner.id(), orderId, requestId,
            input.expectedVersion(), version.id());
    }

    @Transactional
    public AdditionalRequest replace(Identidade owner, UUID orderId, UUID requestId,
                                     ReplacementInput input) {
        requireActive(owner, orderId);
        if (input == null || input.expectedVersion() == null) throw invalid("Informe a versão atual.");
        AdditionalRequest current = repository.request(owner.oficinaId(), orderId, requestId);
        requireState(current, AdditionalRequest.Status.ENVIADA,
            "Somente a versão enviada atual pode ser substituída.");
        AdditionalRequestRepository.DraftData data = validate(input.asDraft());
        repository.validatePhotos(owner.oficinaId(), orderId, data.photoIds(), false);
        String reason = required(input.motivoSubstituicao(), 1000,
            "Informe o motivo da substituição.");
        return repository.replace(owner.oficinaId(), owner.id(), orderId, requestId,
            input.expectedVersion(), currentVersion(current), reason, data);
    }

    @Transactional
    public AdditionalRequest cancel(Identidade owner, UUID orderId, UUID requestId, CancelInput input) {
        requireActive(owner, orderId);
        if (input == null || input.expectedVersion() == null) throw invalid("Informe a versão atual.");
        repository.request(owner.oficinaId(), orderId, requestId);
        String reason = required(input.motivo(), 1000, "Informe o motivo do cancelamento.");
        return repository.cancel(owner.oficinaId(), owner.id(), orderId, requestId,
            input.expectedVersion(), reason);
    }

    private AdditionalRequestRepository.DraftData validate(DraftInput input) {
        if (input == null) throw invalid("Informe os dados da solicitação.");
        String problem = required(input.problema(), 2000, "Descreva o problema encontrado.");
        String justification = required(input.justificativa(), 2000, "Informe a justificativa.");
        String impact = required(input.impactoPrazo(), 1000, "Informe o impacto no prazo.");
        if (input.previsaoProposta() != null && input.previsaoProposta().isBefore(clock.instant())) {
            throw invalid("A previsão proposta não pode estar no passado.");
        }
        List<ItemInput> sourceItems = input.itens() == null ? List.of() : input.itens();
        if (sourceItems.isEmpty() || sourceItems.size() > MAX_ITEMS) {
            throw invalid("Inclua entre 1 e 50 itens na solicitação.");
        }
        List<AdditionalRequestRepository.ItemData> items = sourceItems.stream()
            .map(this::validateItem).toList();
        policy.validateGroups(items.stream().map(AdditionalRequestRepository.ItemData::dependencyGroup).toList());
        List<UUID> photos = input.fotoIds() == null ? List.of() : List.copyOf(input.fotoIds());
        if (photos.size() > MAX_PHOTOS || new HashSet<>(photos).size() != photos.size()) {
            throw invalid("Selecione até 10 fotos diferentes.");
        }
        return new AdditionalRequestRepository.DraftData(problem, justification,
            input.previsaoProposta(), impact, photos, items);
    }

    private AdditionalRequestRepository.ItemData validateItem(ItemInput input) {
        if (input == null || input.tipo() == null) throw invalid("Informe o tipo de cada item.");
        String description = required(input.descricao(), 500, "Descreva cada item adicional.");
        BigDecimal quantity = policy.quantity(input.quantidade());
        BigDecimal unitValue = policy.unitValue(input.valorUnitario());
        String group = optional(input.grupoDependencia(), 60, "O grupo deve ter até 60 caracteres.");
        return new AdditionalRequestRepository.ItemData(input.tipo(), description, quantity,
            unitValue, policy.itemTotal(quantity, unitValue), group);
    }

    private void requireActive(Identidade owner, UUID orderId) {
        ServiceOrder order = orders.order(owner, orderId);
        if (!order.status().active()) {
            throw new ApiException(409, "ORDEM_ENCERRADA", "A ordem de serviço já foi encerrada.");
        }
    }

    private AdditionalRequest.Version currentVersion(AdditionalRequest request) {
        if (request.versoes().isEmpty()) throw new IllegalStateException("Solicitação sem versão.");
        return request.versoes().getLast();
    }

    private void requireState(AdditionalRequest request, AdditionalRequest.Status expected, String detail) {
        if (request.estado() != expected) throw new ApiException(409, "ESTADO_ADICIONAL_INVALIDO", detail);
    }

    private String required(String value, int max, String detail) {
        String normalized = optional(value, max, detail);
        if (normalized == null) throw invalid(detail);
        return normalized;
    }

    private String optional(String value, int max, String detail) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.strip();
        if (normalized.length() > max) throw invalid(detail);
        return normalized;
    }

    private ApiException invalid(String detail) {
        return new ApiException(400, "DADOS_INVALIDOS", detail);
    }

    public record DraftInput(String problema, String justificativa, Instant previsaoProposta,
                             String impactoPrazo, List<UUID> fotoIds, List<ItemInput> itens,
                             Long expectedVersion) {}
    public record ItemInput(AdditionalRequest.ItemType tipo, String descricao, BigDecimal quantidade,
                            BigDecimal valorUnitario, String grupoDependencia) {}
    public record VersionInput(Long expectedVersion) {}
    public record CancelInput(String motivo, Long expectedVersion) {}
    public record ReplacementInput(String problema, String justificativa, Instant previsaoProposta,
                                   String impactoPrazo, List<UUID> fotoIds, List<ItemInput> itens,
                                   String motivoSubstituicao, Long expectedVersion) {
        DraftInput asDraft() {
            return new DraftInput(problema, justificativa, previsaoProposta, impactoPrazo,
                fotoIds, itens, expectedVersion);
        }
    }
}
