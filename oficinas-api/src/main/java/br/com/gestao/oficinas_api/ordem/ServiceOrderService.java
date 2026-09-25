package br.com.gestao.oficinas_api.ordem;

import br.com.gestao.oficinas_api.cadastro.PageResult;
import br.com.gestao.oficinas_api.identidade.*;
import br.com.gestao.oficinas_api.notificacoes.NotificationService;
import br.com.gestao.oficinas_api.notificacoes.NotificationEvent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceOrderService {
    private static final Instant MINIMUM_ENTRY = Instant.parse("2000-01-01T00:00:00Z");
    private final ServiceOrderRepository repository;
    private final Clock clock;
    private final NotificationService notifications;
    public ServiceOrderService(ServiceOrderRepository repository, Clock clock, NotificationService notifications) {
        this.repository = repository; this.clock = clock; this.notifications = notifications;
    }

    public PageResult<ServiceOrder> orders(Identidade owner, String query, int page, int size) {
        return repository.orders(owner.oficinaId(), query(query), page(page), size(size));
    }
    public ServiceOrder order(Identidade owner, UUID id) {
        return repository.order(owner.oficinaId(), id);
    }
    public List<ServiceOrderEvent> timeline(Identidade owner, UUID id) {
        repository.order(owner.oficinaId(), id);
        return repository.timeline(owner.oficinaId(), id);
    }
    public List<PublicServiceOrderEvent> publicTimeline(Identidade owner, UUID id) {
        repository.order(owner.oficinaId(), id);
        return repository.publicTimeline(owner.oficinaId(), id);
    }
    public List<ServiceOrderForecast> forecasts(Identidade owner, UUID id) {
        repository.order(owner.oficinaId(), id);
        return repository.forecasts(owner.oficinaId(), id);
    }
    public List<PublicServiceOrderForecast> publicForecasts(Identidade owner, UUID id) {
        repository.order(owner.oficinaId(), id);
        return repository.publicForecasts(owner.oficinaId(), id);
    }

    @Transactional
    public ServiceOrder create(Identidade owner, UUID idempotencyKey, CreateInput input) {
        if (input == null || input.clienteId() == null || input.veiculoId() == null) {
            throw invalid("Selecione o cliente e o veículo.");
        }
        String report = input.relatoInicial() == null ? "" : input.relatoInicial().strip();
        if (report.length() < 10 || report.length() > 2000) {
            throw invalid("O relato inicial deve ter entre 10 e 2000 caracteres.");
        }
        if (input.entradaEm() == null || input.entradaEm().isBefore(MINIMUM_ENTRY)
            || input.entradaEm().isAfter(clock.instant().plus(Duration.ofMinutes(5)))) {
            throw invalid("Informe uma data de entrada válida.");
        }
        if (input.kmEntrada() == null || input.kmEntrada() < 0 || input.kmEntrada() > 9_999_999) {
            throw invalid("Informe uma quilometragem entre 0 e 9.999.999 km.");
        }
        if (input.previsaoEm() != null && input.previsaoEm().isBefore(input.entradaEm())) {
            throw invalid("A previsão não pode ser anterior à entrada.");
        }
        var data = new ServiceOrderRepository.CreateData(input.clienteId(), input.veiculoId(), report,
            input.entradaEm(), input.kmEntrada(), input.previsaoEm());
        ServiceOrder result = repository.create(owner.oficinaId(), owner.id(), idempotencyKey, hash(data), data);
        notifications.record(owner.oficinaId(), result.id(), NotificationEvent.ORDEM_ABERTA, result.id().toString());
        return result;
    }

    @Transactional
    public ServiceOrder changeStatus(Identidade owner, UUID id, StatusInput input) {
        if (input == null || input.status() == null || input.expectedVersion() == null) {
            throw invalid("Informe a nova etapa e a versão atual da ordem.");
        }
        if (!input.status().active()) {
            throw new ApiException(409, "ENCERRAMENTO_ESPECIFICO", "Entrega e cancelamento serão feitos pelo encerramento da ordem.");
        }
        ServiceOrder current = repository.order(owner.oficinaId(), id);
        requireVersion(current, input.expectedVersion());
        if (current.status() == input.status()) {
            throw invalid("Selecione uma etapa diferente da atual.");
        }
        String reason = text(input.motivo(), 1000, "O motivo deve ter até 1000 caracteres.");
        if (input.status().requiresReasonFrom(current.status()) && reason == null) {
            throw invalid("Informe o motivo para retornar ou colocar uma etapa em espera.");
        }
        var data = new ServiceOrderRepository.StatusData(input.status(), reason,
            text(input.textoPublico(), 2000, "O texto público deve ter até 2000 caracteres."),
            text(input.textoInterno(), 2000, "A observação interna deve ter até 2000 caracteres."),
            input.expectedVersion());
        return repository.changeStatus(owner.oficinaId(), owner.id(), id, data);
    }

    @Transactional
    public ServiceOrderEvent publish(Identidade owner, UUID id, UpdateInput input) {
        if (input == null || input.expectedVersion() == null) {
            throw invalid("Informe a versão atual da ordem.");
        }
        requireVersion(repository.order(owner.oficinaId(), id), input.expectedVersion());
        String publicText = text(input.textoPublico(), 2000, "O texto público deve ter até 2000 caracteres.");
        String internalText = text(input.textoInterno(), 2000, "A observação interna deve ter até 2000 caracteres.");
        if (publicText == null && internalText == null) {
            throw invalid("Escreva um texto público ou uma observação interna.");
        }
        if (input.publicada() && publicText == null) {
            throw invalid("Escreva o texto público antes de publicar para o cliente.");
        }
        return repository.publish(owner.oficinaId(), owner.id(), id,
            new ServiceOrderRepository.UpdateData(publicText, internalText, input.publicada(), input.expectedVersion()));
    }

    @Transactional
    public ServiceOrder updateForecast(Identidade owner, UUID id, ForecastInput input) {
        if (input == null || input.expectedVersion() == null) {
            throw invalid("Informe a versão atual da ordem.");
        }
        ServiceOrder current = repository.order(owner.oficinaId(), id);
        requireVersion(current, input.expectedVersion());
        if (!current.status().active()) {
            throw new ApiException(409, "ORDEM_ENCERRADA", "A ordem de serviço já foi encerrada.");
        }
        if (input.previsao() != null && input.previsao().isBefore(clock.instant())) {
            throw invalid("A nova previsão não pode estar no passado.");
        }
        if (Objects.equals(current.previsaoEm(), input.previsao())) {
            throw invalid("Informe uma previsão diferente da atual.");
        }
        String reason = requiredText(input.motivoPublico(), 1000, "Informe o motivo público da alteração.");
        String nextAction = requiredText(input.proximaAcao(), 1000, "Informe a próxima ação.");
        var change = new ServiceOrderRepository.ForecastChange(owner.oficinaId(), owner.id(), id,
            current.previsaoEm(), input.previsao(), reason, nextAction, input.expectedVersion());
        ServiceOrder result = repository.updateForecast(change);
        notifications.record(owner.oficinaId(), id, NotificationEvent.PREVISAO_ALTERADA, id + ":" + result.versao());
        return result;
    }

    private String query(String value) {
        String result = value == null ? "" : value.strip();
        if (result.length() > 100) throw invalid("A busca deve ter até 100 caracteres.");
        return result;
    }
    private int page(int value) { if (value < 0) throw invalid("Página inválida."); return value; }
    private int size(int value) { if (value < 1 || value > 100) throw invalid("Tamanho de página inválido."); return value; }
    private ApiException invalid(String message) { return new ApiException(400, "DADOS_INVALIDOS", message); }
    private String text(String value, int maximum, String message) {
        String result = value == null ? "" : value.strip();
        if (result.length() > maximum) throw invalid(message);
        return result.isEmpty() ? null : result;
    }
    private String requiredText(String value, int maximum, String message) {
        String result = text(value, maximum, message);
        if (result == null) throw invalid(message);
        return result;
    }
    private void requireVersion(ServiceOrder order, long expectedVersion) {
        if (order.versao() != expectedVersion) {
            throw new ApiException(409, "ORDEM_DESATUALIZADA", "A ordem mudou. Recarregue antes de continuar.");
        }
    }
    private String hash(ServiceOrderRepository.CreateData data) {
        String payload = String.join("|", data.customerId().toString(), data.vehicleId().toString(), data.report(),
            data.entryAt().toString(), Integer.toString(data.mileage()), String.valueOf(data.forecastAt()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }
    public record CreateInput(UUID clienteId, UUID veiculoId, String relatoInicial,
                              Instant entradaEm, Integer kmEntrada, Instant previsaoEm) {}
    public record StatusInput(ServiceOrderStatus status, String motivo, String textoPublico,
                              String textoInterno, Long expectedVersion) {}
    public record UpdateInput(String textoPublico, String textoInterno,
                              boolean publicada, Long expectedVersion) {}
    public record ForecastInput(Instant previsao, String motivoPublico,
                                String proximaAcao, Long expectedVersion) {}
}
