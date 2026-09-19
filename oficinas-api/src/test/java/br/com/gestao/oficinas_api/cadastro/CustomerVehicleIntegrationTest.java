package br.com.gestao.oficinas_api.cadastro;

import br.com.gestao.oficinas_api.notificacoes.TransactionalEmail;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import tools.jackson.databind.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CustomerVehicleIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean TransactionalEmail mail;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String PASSWORD = "Oficina-segura-123";

    class Browser {
        final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        }
        String csrf() throws Exception { return mapper.readTree(get("/api/auth/csrf").body()).get("token").asText(); }
        HttpResponse<String> send(String method, String path, Map<String, ?> body) throws Exception {
            return client.send(HttpRequest.newBuilder(uri(path)).header("Content-Type", "application/json")
                .header("X-CSRF-TOKEN", csrf()).method(method, HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build(), HttpResponse.BodyHandlers.ofString());
        }
    }
    private URI uri(String path) { return URI.create("http://localhost:" + port + path); }
    private Browser owner() throws Exception {
        Browser browser = new Browser();
        String email = UUID.randomUUID() + "@example.test";
        assertEquals(201, browser.send("POST", "/api/auth/cadastro", Map.of("nome", "Dono", "nomeOficina", "Oficina", "email", email, "senha", PASSWORD)).statusCode());
        assertEquals(200, browser.send("POST", "/api/auth/login", Map.of("email", email, "senha", PASSWORD)).statusCode());
        return browser;
    }
    private JsonNode customer(Browser browser, String name, String cpf, String email) throws Exception {
        var response = browser.send("POST", "/api/clientes", Map.of("nome", name, "cpf", cpf, "telefone", "(61) 99999-0000", "email", email));
        assertEquals(201, response.statusCode(), response.body());
        return mapper.readTree(response.body());
    }
    private JsonNode vehicle(Browser browser, JsonNode customer, String plate) throws Exception {
        var response = browser.send("POST", "/api/veiculos", Map.of("placa", plate, "marca", "Volkswagen", "modelo", "T-Cross", "ano", 2024, "cor", "Cinza", "clienteId", customer.get("id").asText()));
        assertEquals(201, response.statusCode(), response.body());
        return mapper.readTree(response.body());
    }

    @Test void createsCustomerWithSeveralNormalizedVehiclesAndSearches() throws Exception {
        Browser browser = owner();
        JsonNode client = customer(browser, "Ana Souza", "529.982.247-25", "ana@example.test");
        assertEquals("52998224725", client.get("cpf").asText());
        JsonNode oldPlate = vehicle(browser, client, "abc-1234");
        JsonNode mercosul = vehicle(browser, client, "bra 1e23");
        assertEquals("ABC1234", oldPlate.get("placa").asText());
        assertEquals("BRA1E23", mercosul.get("placa").asText());
        var result = mapper.readTree(browser.get("/api/veiculos?q=Ana&size=100").body());
        assertEquals(2, result.get("totalElements").asInt());
        assertEquals(client.get("id").asText(), result.get("items").get(0).get("clienteId").asText());
        assertEquals(1, mapper.readTree(browser.get("/api/clientes?q=529.982.247-25").body()).get("totalElements").asInt());
        assertEquals(1, mapper.readTree(browser.get("/api/veiculos?q=BRA-1E23").body()).get("totalElements").asInt());
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM vinculo_cliente_veiculo WHERE cliente_id=? AND fim_em IS NULL", Integer.class, UUID.fromString(client.get("id").asText())));
    }

    @Test void rejectsDuplicatesInsideShopButAllowsSameDataInAnotherShop() throws Exception {
        Browser first = owner(), second = owner();
        JsonNode a = customer(first, "Cliente A", "52998224725", "a@example.test");
        assertEquals(409, first.send("POST", "/api/clientes", Map.of("nome", "Duplicado", "cpf", "52998224725", "telefone", "", "email", "")).statusCode());
        JsonNode b = customer(second, "Cliente B", "52998224725", "b@example.test");
        vehicle(first, a, "ABC-1234");
        assertEquals(409, first.send("POST", "/api/veiculos", Map.of("placa", "abc1234", "marca", "Fiat", "modelo", "Uno", "ano", 2015, "cor", "", "clienteId", a.get("id").asText())).statusCode());
        assertEquals(201, second.send("POST", "/api/veiculos", Map.of("placa", "ABC1234", "marca", "Fiat", "modelo", "Uno", "ano", 2015, "cor", "", "clienteId", b.get("id").asText())).statusCode());
    }

    @Test void hidesAndRejectsResourcesFromAnotherShop() throws Exception {
        Browser first = owner(), second = owner();
        JsonNode client = customer(first, "Cliente Protegido", "52998224725", "safe@example.test");
        JsonNode car = vehicle(first, client, "ABC1234");
        assertEquals(404, second.get("/api/clientes/" + client.get("id").asText()).statusCode());
        assertEquals(404, second.send("PATCH", "/api/clientes/" + client.get("id").asText(), Map.of("versao", 0, "nome", "Invasão")).statusCode());
        assertEquals(404, second.get("/api/veiculos/" + car.get("id").asText()).statusCode());
        assertEquals(404, second.send("POST", "/api/veiculos/" + car.get("id").asText() + "/transferencias",
            Map.of("novoClienteId", client.get("id").asText(), "expectedVersion", 0)).statusCode());
    }

    @Test void transfersResponsibilityAndKeepsImmutableLinkHistory() throws Exception {
        Browser browser = owner();
        JsonNode oldCustomer = customer(browser, "Responsável antigo", "52998224725", "old@example.test");
        JsonNode newCustomer = customer(browser, "Responsável novo", "16899535009", "new@example.test");
        JsonNode car = vehicle(browser, oldCustomer, "BRA1E23");
        var response = browser.send("POST", "/api/veiculos/" + car.get("id").asText() + "/transferencias",
            Map.of("novoClienteId", newCustomer.get("id").asText(), "expectedVersion", 0));
        assertEquals(200, response.statusCode(), response.body());
        assertEquals(newCustomer.get("id").asText(), mapper.readTree(response.body()).get("clienteId").asText());
        UUID vehicleId = UUID.fromString(car.get("id").asText());
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM vinculo_cliente_veiculo WHERE veiculo_id=?", Integer.class, vehicleId));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM vinculo_cliente_veiculo WHERE veiculo_id=? AND fim_em IS NOT NULL", Integer.class, vehicleId));
        assertEquals(409, browser.send("POST", "/api/veiculos/" + vehicleId + "/transferencias",
            Map.of("novoClienteId", oldCustomer.get("id").asText(), "expectedVersion", 0)).statusCode());
    }

    @Test void verifiesEmailWithSingleUseCodeAndClearsVerificationWhenAddressChanges() throws Exception {
        reset(mail);
        Browser browser = owner();
        JsonNode client = customer(browser, "Cliente Verificado", "52998224725", "verify@example.test");
        var issued = browser.send("POST", "/api/clientes/" + client.get("id").asText() + "/verificacao", Map.of());
        assertEquals(202, issued.statusCode(), issued.body());
        var text = ArgumentCaptor.forClass(String.class);
        verify(mail).send(eq("verify@example.test"), anyString(), text.capture());
        var matcher = Pattern.compile("([0-9]{6})").matcher(text.getValue());
        assertTrue(matcher.find());
        String challenge = mapper.readTree(issued.body()).get("desafioId").asText();
        String path = "/api/clientes/" + client.get("id").asText() + "/verificacao/confirmacao";
        var confirmed = browser.send("POST", path, Map.of("desafioId", challenge, "codigo", matcher.group(1)));
        assertEquals(200, confirmed.statusCode(), confirmed.body());
        JsonNode verified = mapper.readTree(confirmed.body());
        assertFalse(verified.get("emailVerificadoEm").isNull());
        assertEquals(400, browser.send("POST", path, Map.of("desafioId", challenge, "codigo", matcher.group(1))).statusCode());
        var changed = browser.send("PATCH", "/api/clientes/" + client.get("id").asText(),
            Map.of("versao", verified.get("versao").asLong(), "email", "new@example.test"));
        assertEquals(200, changed.statusCode(), changed.body());
        assertTrue(mapper.readTree(changed.body()).get("emailVerificadoEm").isNull());
    }

    @Test void validatesInputsVersionsPaginationAndCsrf() throws Exception {
        Browser browser = owner();
        assertEquals(400, browser.send("POST", "/api/clientes", Map.of("nome", "Inválido", "cpf", "11111111111", "telefone", "", "email", "")).statusCode());
        JsonNode client = customer(browser, "Cliente", "52998224725", "client@example.test");
        assertEquals(400, browser.send("POST", "/api/veiculos", Map.of("placa", "ABC12D3", "marca", "Marca", "modelo", "Modelo", "ano", 2024, "cor", "", "clienteId", client.get("id").asText())).statusCode());
        assertEquals(400, browser.get("/api/clientes?size=101").statusCode());
        assertEquals(409, browser.send("PATCH", "/api/clientes/" + client.get("id").asText(), Map.of("versao", 1, "nome", "Desatualizado")).statusCode());
        var withoutCsrf = browser.client.send(HttpRequest.newBuilder(uri("/api/clientes")).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(403, withoutCsrf.statusCode());
        assertEquals(401, new Browser().get("/api/clientes").statusCode());
    }
}
