package br.com.gestao.oficinas_api.ordem;

import br.com.gestao.oficinas_api.cadastro.PageResult;
import br.com.gestao.oficinas_api.identidade.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceOrderService {
    private static final Instant MINIMUM_ENTRY = Instant.parse("2000-01-01T00:00:00Z");
    private final ServiceOrderRepository repository;
    private final Clock clock;
    public ServiceOrderService(ServiceOrderRepository repository, Clock clock) {
        this.repository = repository; this.clock = clock;
    }

    public PageResult<ServiceOrder> orders(Identidade owner, String query, int page, int size) {
        return repository.orders(owner.oficinaId(), query(query), page(page), size(size));
    }
    public ServiceOrder order(Identidade owner, UUID id) {
        return repository.order(owner.oficinaId(), id);
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
        return repository.create(owner.oficinaId(), owner.id(), idempotencyKey, hash(data), data);
    }

    private String query(String value) {
        String result = value == null ? "" : value.strip();
        if (result.length() > 100) throw invalid("A busca deve ter até 100 caracteres.");
        return result;
    }
    private int page(int value) { if (value < 0) throw invalid("Página inválida."); return value; }
    private int size(int value) { if (value < 1 || value > 100) throw invalid("Tamanho de página inválido."); return value; }
    private ApiException invalid(String message) { return new ApiException(400, "DADOS_INVALIDOS", message); }
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
}
