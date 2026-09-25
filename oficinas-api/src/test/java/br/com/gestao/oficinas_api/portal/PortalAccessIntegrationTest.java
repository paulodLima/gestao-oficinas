package br.com.gestao.oficinas_api.portal;

import br.com.gestao.oficinas_api.notificacoes.TransactionalEmail;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PortalAccessIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
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
        final HttpClient client = HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();

        HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> send(String method, String path, Map<String, ?> body) throws Exception {
            String csrf = mapper.readTree(get("/api/auth/csrf").body()).get("token").asText();
            return client.send(HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .header("X-CSRF-TOKEN", csrf)
                .method(method, HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    record Fixture(Browser owner, UUID shopId, String slug, UUID customerId, UUID vehicleId,
                   UUID orderId, String email) {}
    record Challenge(UUID id, String code) {}

    @Test
    void keepsCodeRequestNonEnumerableAndEnforcesExpiryAttemptsAndSingleUse() throws Exception {
        Fixture fixture = fixture("LIM1A23", "52998224725");
        reset(mail);
        Browser anonymous = new Browser();
        var unknown = anonymous.send("POST", "/api/portal/acesso/codigo",
            Map.of("oficinaSlug", fixture.slug(), "placa", "XXX9X99"));
        var known = anonymous.send("POST", "/api/portal/acesso/codigo",
            Map.of("oficinaSlug", fixture.slug(), "placa", "LIM-1A23"));
        assertEquals(202, unknown.statusCode());
        assertEquals(202, known.statusCode());
        assertTrue(mapper.readTree(unknown.body()).has("desafioId"));
        assertTrue(mapper.readTree(known.body()).has("desafioId"));
        verify(mail).send(eq(fixture.email()), anyString(), anyString());

        Challenge limited = challenge(fixture, anonymous);
        for (int attempt = 0; attempt < 5; attempt++) {
            assertEquals(400, anonymous.send("POST", "/api/portal/acesso/validacao",
                Map.of("desafioId", limited.id(), "codigo", "999999")).statusCode());
        }
        assertEquals(400, anonymous.send("POST", "/api/portal/acesso/validacao",
            Map.of("desafioId", limited.id(), "codigo", limited.code())).statusCode());
        assertEquals(5, jdbc.queryForObject("SELECT tentativas FROM portal_desafio WHERE id=?",
            Integer.class, limited.id()));

        Challenge expired = challenge(fixture, anonymous);
        jdbc.update("UPDATE portal_desafio SET expira_em=now()-interval '1 second' WHERE id=?", expired.id());
        assertEquals(400, anonymous.send("POST", "/api/portal/acesso/validacao",
            Map.of("desafioId", expired.id(), "codigo", expired.code())).statusCode());

        Challenge singleUse = challenge(fixture, anonymous);
        assertEquals(204, anonymous.send("POST", "/api/portal/acesso/validacao",
            Map.of("desafioId", singleUse.id(), "codigo", singleUse.code())).statusCode());
        assertEquals(400, new Browser().send("POST", "/api/portal/acesso/validacao",
            Map.of("desafioId", singleUse.id(), "codigo", singleUse.code())).statusCode());
    }

    @Test
    void isolatesShopAndVehiclesAndRevalidatesCurrentRelationship() throws Exception {
        Fixture first = fixture("ISO1A23", "52998224725");
        Fixture second = fixture("ISO1A23", "16899535009");
        Browser customer = new Browser();
        Challenge challenge = challenge(first, customer);
        assertEquals(204, customer.send("POST", "/api/portal/acesso/validacao",
            Map.of("desafioId", challenge.id(), "codigo", challenge.code())).statusCode());

        JsonNode vehicles = mapper.readTree(customer.get("/api/portal/veiculos").body());
        assertEquals(1, vehicles.size());
        assertEquals(first.vehicleId().toString(), vehicles.get(0).get("id").asText());
        assertEquals(200, customer.get("/api/portal/servico-atual?veiculoId=" + first.vehicleId()).statusCode());
        assertTrue(mapper.readTree(customer.get("/api/portal/servico-atual?veiculoId=" + second.vehicleId()).body())
            .get("servico").isNull());
        assertEquals(404, customer.get("/api/portal/ordens-servico/" + second.orderId() + "/atualizacoes").statusCode());

        jdbc.update("UPDATE vinculo_cliente_veiculo SET fim_em=now() WHERE oficina_id=? AND veiculo_id=? AND fim_em IS NULL",
            first.shopId(), first.vehicleId());
        assertEquals(404, customer.get("/api/portal/ordens-servico/" + first.orderId() + "/atualizacoes").statusCode());

        Fixture noLongerLinked = fixture("SEM1A23", "11144477735");
        Browser pending = new Browser();
        Challenge pendingChallenge = challenge(noLongerLinked, pending);
        jdbc.update("UPDATE vinculo_cliente_veiculo SET fim_em=now() WHERE oficina_id=? AND veiculo_id=? AND fim_em IS NULL",
            noLongerLinked.shopId(), noLongerLinked.vehicleId());
        assertEquals(400, pending.send("POST", "/api/portal/acesso/validacao",
            Map.of("desafioId", pendingChallenge.id(), "codigo", pendingChallenge.code())).statusCode());
    }

    @Test
    void restrictsExclusiveLinkToReadOnlyOrderAndRevalidatesRevocationAndExpiry() throws Exception {
        Fixture fixture = fixture("LNK1A23", "52998224725");
        Fixture outsider = fixture("OUT1A23", "16899535009");
        var created = fixture.owner().send("POST", "/api/ordens-servico/" + fixture.orderId() + "/acesso", Map.of());
        assertEquals(200, created.statusCode(), created.body());
        JsonNode link = mapper.readTree(created.body());
        assertEquals(43, link.get("token").asText().length());
        assertEquals(404, outsider.owner().send("DELETE",
            "/api/ordens-servico/" + fixture.orderId() + "/acesso", Map.of()).statusCode());

        Browser visitor = new Browser();
        assertEquals(204, visitor.send("POST", "/api/portal/acesso/link",
            Map.of("token", link.get("token").asText())).statusCode());
        assertEquals(fixture.orderId().toString(), mapper.readTree(visitor.get("/api/portal/servico-atual").body())
            .get("servico").get("id").asText());
        assertEquals(401, visitor.send("POST", "/api/ordens-servico/" + fixture.orderId() + "/status",
            Map.of("status", "EM_DIAGNOSTICO", "expectedVersion", 0)).statusCode());

        assertEquals(204, fixture.owner().send("DELETE",
            "/api/ordens-servico/" + fixture.orderId() + "/acesso", Map.of()).statusCode());
        assertEquals(401, visitor.get("/api/portal/servico-atual").statusCode());

        JsonNode expiring = mapper.readTree(fixture.owner().send("POST",
            "/api/ordens-servico/" + fixture.orderId() + "/acesso", Map.of()).body());
        jdbc.update("UPDATE portal_link_os SET expira_em=now()-interval '1 second' WHERE oficina_id=? AND ordem_servico_id=? AND revogado_em IS NULL",
            fixture.shopId(), fixture.orderId());
        assertEquals(400, new Browser().send("POST", "/api/portal/acesso/link",
            Map.of("token", expiring.get("token").asText())).statusCode());
    }

    private Fixture fixture(String plate, String cpf) throws Exception {
        String ownerEmail = UUID.randomUUID() + "@owner.test";
        Browser owner = new Browser();
        assertEquals(201, owner.send("POST", "/api/auth/cadastro", Map.of(
            "nome", "Dono", "nomeOficina", "Oficina " + plate, "email", ownerEmail, "senha", PASSWORD)).statusCode());
        assertEquals(200, owner.send("POST", "/api/auth/login",
            Map.of("email", ownerEmail, "senha", PASSWORD)).statusCode());
        JsonNode me = mapper.readTree(owner.get("/api/auth/me").body());
        UUID shopId = UUID.fromString(me.get("oficina").get("id").asText());
        String slug = me.get("oficina").get("slug").asText();
        String customerEmail = UUID.randomUUID() + "@customer.test";
        JsonNode customer = mapper.readTree(owner.send("POST", "/api/clientes", Map.of(
            "nome", "Cliente Portal", "cpf", cpf, "telefone", "(61) 99999-0000", "email", customerEmail)).body());
        UUID customerId = UUID.fromString(customer.get("id").asText());
        jdbc.update("UPDATE cliente SET email_verificado_em=now() WHERE oficina_id=? AND id=?", shopId, customerId);
        JsonNode vehicle = mapper.readTree(owner.send("POST", "/api/veiculos", Map.of(
            "placa", plate, "marca", "Volkswagen", "modelo", "T-Cross", "ano", 2024,
            "cor", "Cinza", "clienteId", customerId.toString())).body());
        UUID vehicleId = UUID.fromString(vehicle.get("id").asText());
        JsonNode order = mapper.readTree(owner.send("POST", "/api/ordens-servico", Map.of(
            "clienteId", customerId.toString(), "veiculoId", vehicleId.toString(),
            "relatoInicial", "Ruído na suspensão dianteira ao passar em desníveis.",
            "entradaEm", Instant.now().minusSeconds(60).toString(), "kmEntrada", 12000,
            "previsaoEm", Instant.now().plusSeconds(86_400).toString())).body());
        return new Fixture(owner, shopId, slug, customerId, vehicleId,
            UUID.fromString(order.get("id").asText()), customerEmail);
    }

    private Challenge challenge(Fixture fixture, Browser browser) throws Exception {
        reset(mail);
        var response = browser.send("POST", "/api/portal/acesso/codigo",
            Map.of("oficinaSlug", fixture.slug(), "placa", fixture.vehicleId() == null ? "" : plate(fixture.vehicleId())));
        // The plate is read from persistence so helpers cannot accidentally bypass office scoping.
        UUID id = UUID.fromString(mapper.readTree(response.body()).get("desafioId").asText());
        var content = ArgumentCaptor.forClass(String.class);
        verify(mail).send(eq(fixture.email()), anyString(), content.capture());
        var matcher = Pattern.compile("Código é: ([0-9]{6})", Pattern.CASE_INSENSITIVE).matcher(content.getValue());
        assertTrue(matcher.find());
        return new Challenge(id, matcher.group(1));
    }

    private String plate(UUID vehicleId) {
        return jdbc.queryForObject("SELECT placa FROM veiculo WHERE id=?", String.class, vehicleId);
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
