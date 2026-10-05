package br.com.gestao.oficinas_api.oficina;

import br.com.gestao.oficinas_api.identidade.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Consulta leve de endereços brasileiros sem exigir chave de provedor comercial. */
@Service
public class AddressLookupService {
    private static final String SEARCH_URL = "https://nominatim.openstreetmap.org/search";
    private static final Logger log = LoggerFactory.getLogger(AddressLookupService.class);
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private final ObjectMapper json = new ObjectMapper();
    private Instant lastRequest = Instant.EPOCH;

    public synchronized List<Suggestion> search(String query) {
        String normalized = query == null ? "" : query.strip();
        if (normalized.length() < 3 || normalized.length() > 180) {
            throw new ApiException(400, "BUSCA_ENDERECO_INVALIDA", "Informe pelo menos 3 caracteres para buscar o endereço.");
        }
        if (Duration.between(lastRequest, Instant.now()).compareTo(Duration.ofSeconds(1)) < 0) {
            throw new ApiException(429, "BUSCA_ENDERECO_AGUARDE", "Aguarde um instante e tente novamente.");
        }
        lastRequest = Instant.now();
        try {
            String url = SEARCH_URL + "?format=jsonv2&addressdetails=1&limit=6&countrycodes=br&q="
                + URLEncoder.encode(normalized, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(6))
                .header("Accept", "application/json")
                .header("User-Agent", "GestaoOficinas/1.0 (address lookup)").GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Consulta de endereço indisponível: HTTP {}.", response.statusCode());
                throw unavailable();
            }
            List<Suggestion> suggestions = new ArrayList<>();
            for (JsonNode item : json.readTree(response.body())) {
                String address = item.path("display_name").asText("").strip();
                if (!address.isEmpty() && address.length() <= 500) suggestions.add(new Suggestion(address));
            }
            return suggestions;
        } catch (Exception exception) {
            if (exception instanceof ApiException apiException) throw apiException;
            log.warn("Falha ao consultar o serviço de endereços.", exception);
            throw unavailable();
        }
    }

    private ApiException unavailable() {
        return new ApiException(503, "BUSCA_ENDERECO_INDISPONIVEL",
            "Não foi possível consultar endereços agora. Você pode preencher o endereço manualmente.");
    }

    public record Suggestion(String endereco) {}
}
