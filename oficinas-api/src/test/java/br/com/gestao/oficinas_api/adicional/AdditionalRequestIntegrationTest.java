package br.com.gestao.oficinas_api.adicional;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import br.com.gestao.oficinas_api.support.TestPostgres;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AdditionalRequestIntegrationTest {
    @Container static final TestPostgres postgres = new TestPostgres();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String PASSWORD = "Oficina-segura-123";

    @Test void freezesSentVersionReplacesWithoutErasingHistoryAndIsolatesShop() throws Exception {
        Browser owner = account();
        Browser outsider = account();
        JsonNode order = openedOrder(owner);
        String base = "/api/ordens-servico/" + order.get("id").asText() + "/adicionais";

        var createdResponse = owner.send("POST", base, draft("Discos dianteiros", "kit-freio"));
        assertEquals(201, createdResponse.statusCode(), createdResponse.body());
        JsonNode created = mapper.readTree(createdResponse.body());
        String requestPath = base + "/" + created.get("id").asText();
        assertEquals("RASCUNHO", created.get("estado").asText());
        assertEquals("44.98", created.get("versoes").get(0).get("total").decimalValue().toPlainString());

        var sent = owner.send("POST", requestPath + "/envio", Map.of("expectedVersion", 0));
        assertEquals(200, sent.statusCode(), sent.body());
        assertEquals("ENVIADA", mapper.readTree(sent.body()).get("estado").asText());
        assertEquals(409, owner.send("PATCH", requestPath,
            withVersion(draft("Tentativa de edição", "kit-freio"), 1)).statusCode());

        Map<String, Object> replacement = withVersion(draft("Discos e pastilhas", "kit-completo"), 1);
        replacement.put("motivoSubstituicao", "Diagnóstico ampliado após desmontagem");
        var replacedResponse = owner.send("POST", requestPath + "/substituicoes", replacement);
        assertEquals(200, replacedResponse.statusCode(), replacedResponse.body());
        JsonNode replaced = mapper.readTree(replacedResponse.body());
        assertEquals("RASCUNHO", replaced.get("estado").asText());
        assertEquals(2, replaced.get("versoes").size());
        assertEquals("SUBSTITUIDA", replaced.get("versoes").get(0).get("estado").asText());
        assertEquals("RASCUNHO", replaced.get("versoes").get(1).get("estado").asText());
        assertEquals("Discos dianteiros", replaced.get("versoes").get(0).get("problema").asText());
        assertEquals(404, outsider.get(base).statusCode());
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM cadastro_auditoria WHERE recurso='SOLICITACAO_ADICIONAL'", Integer.class));
    }

    private Map<String, Object> draft(String problem, String group) {
        return new java.util.HashMap<>(Map.of(
            "problema", problem, "justificativa", "Substituição necessária para segurança",
            "previsaoProposta", Instant.now().plusSeconds(86_400).toString(),
            "impactoPrazo", "Acrescenta um dia útil", "fotoIds", List.of(),
            "itens", List.of(
                Map.of("tipo", "PECA", "descricao", "Disco esquerdo", "quantidade", 1,
                    "valorUnitario", 22.49, "grupoDependencia", group),
                Map.of("tipo", "PECA", "descricao", "Disco direito", "quantidade", 1,
                    "valorUnitario", 22.49, "grupoDependencia", group))));
    }

    private Map<String, Object> withVersion(Map<String, Object> values, long version) {
        Map<String, Object> result = new java.util.HashMap<>(values);
        result.put("expectedVersion", version);
        return result;
    }

    private Browser account() throws Exception {
        Browser browser = new Browser();
        String email = UUID.randomUUID() + "@example.test";
        assertEquals(201, browser.send("POST", "/api/auth/cadastro", Map.of(
            "nome", "Dono", "nomeOficina", "Oficina", "email", email, "senha", PASSWORD)).statusCode());
        assertEquals(200, browser.send("POST", "/api/auth/login", Map.of(
            "email", email, "senha", PASSWORD)).statusCode());
        return browser;
    }

    private JsonNode openedOrder(Browser browser) throws Exception {
        JsonNode customer = mapper.readTree(browser.send("POST", "/api/clientes", Map.of(
            "nome", "Ana", "cpf", "52998224725", "telefone", "(61) 99999-0000", "email", "")).body());
        JsonNode vehicle = mapper.readTree(browser.send("POST", "/api/veiculos", Map.of(
            "placa", "BRA1E23", "marca", "Volkswagen", "modelo", "T-Cross", "ano", 2024,
            "cor", "Cinza", "clienteId", customer.get("id").asText())).body());
        var response = browser.send("POST", "/api/ordens-servico", Map.of(
            "clienteId", customer.get("id").asText(), "veiculoId", vehicle.get("id").asText(),
            "relatoInicial", "Ruído na suspensão dianteira ao passar em desníveis.",
            "entradaEm", Instant.now().minusSeconds(60).toString(), "kmEntrada", 48210,
            "previsaoEm", Instant.now().plusSeconds(172_800).toString()));
        assertEquals(201, response.statusCode(), response.body());
        return mapper.readTree(response.body());
    }

    class Browser {
        final HttpClient client = HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        }
        HttpResponse<String> send(String method, String path, Map<String, ?> body) throws Exception {
            String csrf = mapper.readTree(get("/api/auth/csrf").body()).get("token").asText();
            return client.send(HttpRequest.newBuilder(uri(path)).header("Content-Type", "application/json")
                .header("X-CSRF-TOKEN", csrf).method(method,
                    HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(),
                HttpResponse.BodyHandlers.ofString());
        }
    }

    private URI uri(String path) { return URI.create("http://localhost:" + port + path); }
}
