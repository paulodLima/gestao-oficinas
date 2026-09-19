package br.com.gestao.oficinas_api.ordem;
import java.net.*;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import tools.jackson.databind.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ServiceOrderIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String PASSWORD = "Oficina-segura-123";
    class Browser {
        final HttpClient client = HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        }
        String csrf() throws Exception {
            return mapper.readTree(get("/api/auth/csrf").body()).get("token").asText();
        }
        HttpResponse<String> send(String method, String path, Map<String, ?> body) throws Exception {
            return send(method, path, body, csrf());
        }
        HttpResponse<String> send(String method, String path, Map<String, ?> body, String csrf) throws Exception {
            return send(method, path, body, csrf, null);
        }
        HttpResponse<String> send(String method, String path, Map<String, ?> body, String csrf,
                                  UUID idempotencyKey) throws Exception {
            HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).header("Content-Type", "application/json")
                .header("X-CSRF-TOKEN", csrf);
            if (idempotencyKey != null) request.header("Idempotency-Key", idempotencyKey.toString());
            return client.send(request.method(method,
                    HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(),
                HttpResponse.BodyHandlers.ofString());
        }
    }
    record Account(Browser primary, Browser secondary) {}
    private URI uri(String path) { return URI.create("http://localhost:" + port + path); }
    private Account account() throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        Browser primary = new Browser();
        assertEquals(201, primary.send("POST", "/api/auth/cadastro", Map.of(
            "nome", "Dono", "nomeOficina", "Oficina", "email", email, "senha", PASSWORD)).statusCode());
        assertEquals(200, primary.send("POST", "/api/auth/login", Map.of("email", email, "senha", PASSWORD)).statusCode());
        Browser secondary = new Browser();
        assertEquals(200, secondary.send("POST", "/api/auth/login", Map.of("email", email, "senha", PASSWORD)).statusCode());
        return new Account(primary, secondary);
    }
    private JsonNode customer(Browser browser, String name, String cpf) throws Exception {
        var response = browser.send("POST", "/api/clientes", Map.of(
            "nome", name, "cpf", cpf, "telefone", "(61) 99999-0000", "email", ""));
        assertEquals(201, response.statusCode(), response.body());
        return mapper.readTree(response.body());
    }
    private JsonNode vehicle(Browser browser, JsonNode customer, String plate) throws Exception {
        var response = browser.send("POST", "/api/veiculos", Map.of(
            "placa", plate, "marca", "Volkswagen", "modelo", "T-Cross", "ano", 2024,
            "cor", "Cinza", "clienteId", customer.get("id").asText()));
        assertEquals(201, response.statusCode(), response.body());
        return mapper.readTree(response.body());
    }
    private Map<String, Object> orderInput(JsonNode customer, JsonNode vehicle, int mileage) {
        return Map.of("clienteId", customer.get("id").asText(), "veiculoId", vehicle.get("id").asText(),
            "relatoInicial", "Ruído na suspensão dianteira ao passar em desníveis.",
            "entradaEm", Instant.now().minusSeconds(60).toString(), "kmEntrada", mileage,
            "previsaoEm", Instant.now().plusSeconds(86_400).toString());
    }
    @Test void opensNumberedOrdersAndSearchesByNumberCustomerAndFormattedPlate() throws Exception {
        Browser browser = account().primary();
        JsonNode customer = customer(browser, "Ana Souza", "52998224725");
        JsonNode firstVehicle = vehicle(browser, customer, "BRA-1E23");
        JsonNode secondVehicle = vehicle(browser, customer, "ABC-1234");
        var first = browser.send("POST", "/api/ordens-servico", orderInput(customer, firstVehicle, 48210));
        var second = browser.send("POST", "/api/ordens-servico", orderInput(customer, secondVehicle, 99120));
        assertEquals(201, first.statusCode(), first.body());
        assertEquals(201, second.statusCode(), second.body());
        JsonNode opened = mapper.readTree(first.body());
        assertEquals(1, opened.get("numero").asLong());
        assertEquals("RECEBIDO", opened.get("status").asText());
        assertEquals(2, mapper.readTree(second.body()).get("numero").asLong());
        assertEquals(2, search(browser, "Ana").get("totalElements").asInt());
        assertEquals(1, search(browser, "BRA-1E23").get("totalElements").asInt());
        assertEquals(1, search(browser, "OS-1").get("totalElements").asInt());
        assertEquals(200, browser.get("/api/ordens-servico/" + opened.get("id").asText()).statusCode());
    }
    @Test void allowsOnlyOneConcurrentActiveOrderForVehicle() throws Exception {
        Account account = account();
        JsonNode customer = customer(account.primary(), "Cliente Concorrente", "52998224725");
        JsonNode vehicle = vehicle(account.primary(), customer, "BRA1E23");
        String firstToken = account.primary().csrf();
        String secondToken = account.secondary().csrf();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(() -> openTogether(account.primary(), firstToken, customer, vehicle, ready, start));
            Future<Integer> second = executor.submit(() -> openTogether(account.secondary(), secondToken, customer, vehicle, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            List<Integer> statuses = new ArrayList<>(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)));
            Collections.sort(statuses);
            assertEquals(List.of(201, 409), statuses);
        }
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM ordem_servico WHERE veiculo_id=? AND encerrada_em IS NULL",
            Integer.class, UUID.fromString(vehicle.get("id").asText())));
    }
    @Test void blocksTransferWhileActiveAndKeepsHistoricalCustomerAfterClosing() throws Exception {
        Browser browser = account().primary();
        JsonNode oldCustomer = customer(browser, "Responsável original", "52998224725");
        JsonNode newCustomer = customer(browser, "Responsável novo", "16899535009");
        JsonNode vehicle = vehicle(browser, oldCustomer, "BRA1E23");
        JsonNode order = mapper.readTree(browser.send("POST", "/api/ordens-servico",
            orderInput(oldCustomer, vehicle, 1000)).body());
        String transferPath = "/api/veiculos/" + vehicle.get("id").asText() + "/transferencias";
        Map<String, Object> transfer = Map.of("novoClienteId", newCustomer.get("id").asText(), "expectedVersion", 0);
        assertEquals(409, browser.send("POST", transferPath, transfer).statusCode());
        jdbc.update("UPDATE ordem_servico SET status='ENTREGUE',encerrada_em=now() WHERE id=?",
            UUID.fromString(order.get("id").asText()));
        assertEquals(200, browser.send("POST", transferPath, transfer).statusCode());
        JsonNode detail = mapper.readTree(browser.get("/api/ordens-servico/" + order.get("id").asText()).body());
        assertEquals(oldCustomer.get("id").asText(), detail.get("clienteId").asText());
        assertEquals("Responsável original", detail.get("clienteNome").asText());
    }
    @Test void validatesOwnershipFieldsPaginationAndShopIsolation() throws Exception {
        Browser first = account().primary();
        Browser second = account().primary();
        JsonNode customer = customer(first, "Cliente Protegido", "52998224725");
        JsonNode otherCustomer = customer(first, "Outro Cliente", "16899535009");
        JsonNode vehicle = vehicle(first, customer, "ABC1234");
        Map<String, Object> wrongOwner = new HashMap<>(orderInput(otherCustomer, vehicle, 2000));
        assertEquals(409, first.send("POST", "/api/ordens-servico", wrongOwner).statusCode());
        Map<String, Object> invalid = new HashMap<>(orderInput(customer, vehicle, 2000));
        invalid.put("relatoInicial", "curto");
        assertEquals(400, first.send("POST", "/api/ordens-servico", invalid).statusCode());
        JsonNode order = mapper.readTree(first.send("POST", "/api/ordens-servico", orderInput(customer, vehicle, 2000)).body());
        assertEquals(404, second.get("/api/ordens-servico/" + order.get("id").asText()).statusCode());
        assertEquals(0, mapper.readTree(second.get("/api/ordens-servico").body()).get("totalElements").asInt());
        assertEquals(400, first.get("/api/ordens-servico?size=101").statusCode());
        assertEquals(401, new Browser().get("/api/ordens-servico").statusCode());
    }
    @Test void replaysSameIdempotentOpeningAndRejectsDifferentPayload() throws Exception {
        Browser browser = account().primary();
        JsonNode customer = customer(browser, "Cliente Idempotente", "52998224725");
        JsonNode vehicle = vehicle(browser, customer, "BRA1E23");
        Map<String, Object> input = orderInput(customer, vehicle, 45000);
        UUID key = UUID.randomUUID();
        String csrf = browser.csrf();
        var first = browser.send("POST", "/api/ordens-servico", input, csrf, key);
        var replay = browser.send("POST", "/api/ordens-servico", input, csrf, key);
        Map<String, Object> changed = new HashMap<>(input);
        changed.put("kmEntrada", 45001);
        var conflict = browser.send("POST", "/api/ordens-servico", changed, csrf, key);
        assertEquals(201, first.statusCode(), first.body());
        assertEquals(201, replay.statusCode(), replay.body());
        assertEquals(mapper.readTree(first.body()).get("id"), mapper.readTree(replay.body()).get("id"));
        assertEquals(409, conflict.statusCode(), conflict.body());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM ordem_servico WHERE veiculo_id=?",
            Integer.class, UUID.fromString(vehicle.get("id").asText())));
    }
    @Test void skipsStagesAndRequiresReasonWhenReturning() throws Exception {
        Browser browser = account().primary();
        JsonNode order = openedOrder(browser, "Cliente Fluxo", "52998224725", "FLX1A23");
        String path = "/api/ordens-servico/" + order.get("id").asText() + "/status";
        var skipped = browser.send("POST", path, Map.of("status", "EM_TESTES", "expectedVersion", 0));
        assertEquals(200, skipped.statusCode(), skipped.body());
        assertEquals("EM_TESTES", mapper.readTree(skipped.body()).get("status").asText());
        assertEquals(400, browser.send("POST", path,
            Map.of("status", "EM_DIAGNOSTICO", "expectedVersion", 1)).statusCode());
        var returned = browser.send("POST", path, Map.of("status", "EM_DIAGNOSTICO", "expectedVersion", 1,
            "motivo", "Sintoma reapareceu durante o teste.", "textoPublico", "Retornamos ao diagnóstico.",
            "textoInterno", "Rever fixação do agregado."));
        assertEquals(200, returned.statusCode(), returned.body());
        JsonNode timeline = mapper.readTree(browser.get(path.replace("/status", "/atualizacoes")).body());
        assertEquals(3, timeline.size());
        assertEquals("Sintoma reapareceu durante o teste.", timeline.get(0).get("motivo").asText());
        assertEquals("Dono", timeline.get(0).get("autorNome").asText());
        assertFalse(timeline.get(0).get("createdAt").asText().isBlank());
    }
    @Test void publishesWithoutChangingStatusAndNeverLeaksInternalText() throws Exception {
        Browser owner = account().primary();
        Browser outsider = account().primary();
        JsonNode order = openedOrder(owner, "Cliente Publicação", "52998224725", "PUB1A23");
        String updates = "/api/ordens-servico/" + order.get("id").asText() + "/atualizacoes";
        var published = owner.send("POST", updates, Map.of("textoPublico", "Diagnóstico iniciado.",
            "textoInterno", "Cliente relatou tentativa anterior em outra oficina.",
            "publicada", true, "expectedVersion", 0));
        assertEquals(200, published.statusCode(), published.body());
        assertEquals("RECEBIDO", mapper.readTree(owner.get("/api/ordens-servico/" + order.get("id").asText()).body())
            .get("status").asText());
        String ownerTimeline = owner.get(updates).body();
        assertTrue(ownerTimeline.contains("tentativa anterior"));
        String publicTimeline = owner.get(updates + "/publicas").body();
        assertTrue(publicTimeline.contains("Diagnóstico iniciado"));
        assertFalse(publicTimeline.contains("tentativa anterior"));
        assertFalse(publicTimeline.contains("textoInterno"));
        assertEquals(404, outsider.get(updates + "/publicas").statusCode());
        assertEquals(200, owner.send("POST", updates, Map.of("textoInterno", "Conferir torque.",
            "publicada", false, "expectedVersion", 1)).statusCode());
        assertEquals(1, mapper.readTree(owner.get(updates + "/publicas").body()).size());
    }
    @Test void preservesForecastHistoryAndPublicProjectionWithoutOwnerData() throws Exception {
        Browser owner = account().primary();
        Browser outsider = account().primary();
        JsonNode order = openedOrder(owner, "Cliente Previsão", "52998224725", "PRV1A23");
        String base = "/api/ordens-servico/" + order.get("id").asText();
        assertEquals(400, owner.send("POST", base + "/previsao", forecastInput("2020-01-01T10:00:00-03:00", "Data inválida", 0)).statusCode());
        assertEquals(400, owner.send("POST", base + "/previsao", forecastInput("2026-09-20T10:00:00", "Sem fuso", 0)).statusCode());
        String next = Instant.now().plusSeconds(172_800).toString();
        var changed = owner.send("POST", base + "/previsao", forecastInput(next, "Atraso do fornecedor", 0));
        assertEquals(200, changed.statusCode(), changed.body());
        assertEquals(Instant.parse(next).getEpochSecond(), Instant.parse(mapper.readTree(changed.body()).get("previsaoEm").asText()).getEpochSecond());
        JsonNode history = mapper.readTree(owner.get(base + "/previsoes").body());
        assertEquals(1, history.size());
        assertEquals(order.get("previsaoEm").asText(), history.get(0).get("previsaoAnterior").asText());
        assertEquals("Dono", history.get(0).get("autorNome").asText());
        String publicHistory = owner.get(base + "/previsoes/publicas").body();
        assertTrue(publicHistory.contains("Confirmar entrega da peça"));
        assertFalse(publicHistory.contains("autorId") || publicHistory.contains("autorNome") || publicHistory.contains("textoInterno"));
        assertEquals(404, outsider.get(base + "/previsoes/publicas").statusCode());
        Map<String, Object> withoutDate = new HashMap<>();
        withoutDate.put("previsao", null); withoutDate.put("motivoPublico", "Peça incompatível");
        withoutDate.put("proximaAcao", "Localizar fornecedor alternativo."); withoutDate.put("expectedVersion", 1);
        JsonNode cleared = mapper.readTree(owner.send("POST", base + "/previsao", withoutDate).body());
        assertTrue(cleared.get("previsaoEm").isNull() && !cleared.get("atrasada").asBoolean() && mapper.readTree(owner.get(base + "/previsoes").body()).size() == 2);
    }
    @Test void marksOverdueExecutionButSeparatesReadyForPickup() throws Exception {
        Browser owner = account().primary();
        JsonNode order = openedOrder(owner, "Cliente Atraso", "52998224725", "ATR1A23");
        UUID orderId = UUID.fromString(order.get("id").asText());
        jdbc.update("UPDATE ordem_servico SET previsao_em=now()-interval '30 seconds' WHERE id=?", orderId);
        String base = "/api/ordens-servico/" + orderId;
        JsonNode overdue = mapper.readTree(owner.get(base).body());
        assertTrue(overdue.get("atrasada").asBoolean() && !overdue.get("aguardandoRetirada").asBoolean());
        JsonNode ready = mapper.readTree(owner.send("POST", base + "/status", Map.of("status", "PRONTO_PARA_RETIRADA", "expectedVersion", 0)).body());
        assertTrue(!ready.get("atrasada").asBoolean() && ready.get("aguardandoRetirada").asBoolean());
    }
    @Test void rejectsConcurrentStatusChangesWithTheSameVersion() throws Exception {
        Account account = account();
        JsonNode order = openedOrder(account.primary(), "Cliente Versão", "52998224725", "VER1A23");
        String path = "/api/ordens-servico/" + order.get("id").asText() + "/status";
        String firstToken = account.primary().csrf();
        String secondToken = account.secondary().csrf();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(() -> changeTogether(account.primary(), firstToken, path,
                "EM_DIAGNOSTICO", ready, start));
            Future<Integer> second = executor.submit(() -> changeTogether(account.secondary(), secondToken, path,
                "EM_MANUTENCAO", ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            List<Integer> statuses = new ArrayList<>(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)));
            Collections.sort(statuses);
            assertEquals(List.of(200, 409), statuses);
        }
        assertEquals(2, mapper.readTree(account.primary().get(path.replace("/status", "/atualizacoes")).body()).size());
    }
    private JsonNode search(Browser browser, String query) throws Exception {
        return mapper.readTree(browser.get("/api/ordens-servico?q=" + URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8)).body());
    }
    private Map<String, Object> forecastInput(String forecast, String reason, int version) { return Map.of(
        "previsao", forecast, "motivoPublico", reason, "proximaAcao", "Confirmar entrega da peça.", "expectedVersion", version); }
    private int openTogether(Browser browser, String csrf, JsonNode customer, JsonNode vehicle,
                             CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        assertTrue(start.await(5, TimeUnit.SECONDS));
        return browser.send("POST", "/api/ordens-servico", orderInput(customer, vehicle, 3000), csrf).statusCode();
    }
    private JsonNode openedOrder(Browser browser, String name, String cpf, String plate) throws Exception {
        JsonNode customer = customer(browser, name, cpf);
        JsonNode vehicle = vehicle(browser, customer, plate);
        var response = browser.send("POST", "/api/ordens-servico", orderInput(customer, vehicle, 3000));
        assertEquals(201, response.statusCode(), response.body());
        return mapper.readTree(response.body());
    }
    private int changeTogether(Browser browser, String csrf, String path, String status,
                               CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        assertTrue(start.await(5, TimeUnit.SECONDS));
        return browser.send("POST", path, Map.of("status", status, "expectedVersion", 0), csrf).statusCode();
    }
}
