package br.com.gestao.oficinas_api.oficina;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import br.com.gestao.oficinas_api.support.TestPostgres;
import org.testcontainers.junit.jupiter.*;
import tools.jackson.databind.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ShopIntegrationTest {
    @Container static final TestPostgres postgres = new TestPostgres();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();
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
        HttpResponse<String> upload(byte[] image, long version) throws Exception {
            var output = new ByteArrayOutputStream();
            output.write("--testBoundary\r\nContent-Disposition: form-data; name=\"arquivo\"; filename=\"logo.png\"\r\nContent-Type: image/png\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            output.write(image);
            output.write("\r\n--testBoundary--\r\n".getBytes(StandardCharsets.UTF_8));
            return client.send(HttpRequest.newBuilder(uri("/api/oficina/logo?versao=" + version))
                .header("Content-Type", "multipart/form-data; boundary=testBoundary").header("X-CSRF-TOKEN", csrf())
                .PUT(HttpRequest.BodyPublishers.ofByteArray(output.toByteArray())).build(), HttpResponse.BodyHandlers.ofString());
        }
        JsonNode profile() throws Exception { return mapper.readTree(get("/api/oficina").body()); }
    }
    private URI uri(String path) { return URI.create("http://localhost:" + port + path); }
    private Browser owner() throws Exception {
        var browser = new Browser();
        String email = UUID.randomUUID() + "@example.test";
        var credentials = Map.of("email", email, "senha", "Oficina-segura-123");
        assertEquals(201, browser.send("POST", "/api/auth/cadastro", Map.of("nome", "Teste", "nomeOficina", "Oficina teste", "email", email, "senha", "Oficina-segura-123")).statusCode());
        assertEquals(200, browser.send("POST", "/api/auth/login", credentials).statusCode());
        return browser;
    }
    @Test void savesPartialFieldsAndAuditsWithVersionConflict() throws Exception {
        var browser = owner();
        var result = browser.send("PATCH", "/api/oficina", Map.of("versao", 0, "nome", "Oficina Horizonte", "telefone", "(61) 3333-4444", "horario", "Seg a sex: 8h–18h", "emailContato", "contato@example.test"));
        assertEquals(200, result.statusCode(), result.body());
        assertEquals("Oficina Horizonte", browser.profile().get("nome").asText());
        assertEquals("America/Sao_Paulo", browser.profile().get("fuso").asText());
        assertEquals(409, browser.send("PATCH", "/api/oficina", Map.of("versao", 0, "nome", "Antigo")).statusCode());
        assertEquals(200, browser.send("PATCH", "/api/oficina", Map.of("versao", 1, "telefone", "")).statusCode());
        assertEquals("", browser.profile().get("telefone").asText());
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM identidade_auditoria WHERE oficina_id=? AND acao='OFICINA_EDITADA'", Integer.class, UUID.fromString(browser.profile().get("id").asText())));
    }
    @Test void rejectsInvalidFieldsAndCrossTenantWrites() throws Exception {
        var first = owner();
        var second = owner();
        String other = second.profile().get("id").asText();
        for (var change : List.of(Map.of("nome", " "), Map.of("nome", "x".repeat(121)), Map.of("fuso", "Mars/Olympus"), Map.of("emailContato", "invalid"), Map.of("telefone", "javascript:test"), Map.of("oficinaId", other))) {
            var body = new HashMap<String, Object>(change);
            body.put("versao", 0);
            assertEquals(400, first.send("PATCH", "/api/oficina", body).statusCode());
        }
        assertEquals(200, first.send("PATCH", "/api/oficina?oficinaId=" + other, Map.of("versao", 0, "nome", "Somente primeira")).statusCode());
        assertEquals("Oficina teste", second.profile().get("nome").asText());
        assertEquals(0, second.profile().get("versao").asLong());
        assertEquals(403, first.send("PATCH", "/api/oficina/" + other, Map.of("versao", 0, "nome", "Invasão")).statusCode());
    }
    @Test void publicProfileIsOptInAndContainsOnlyReleasedData() throws Exception {
        var browser = owner();
        var publicBrowser = new Browser();
        String path = "/api/publico/oficinas/" + browser.profile().get("slug").asText();
        assertEquals(404, publicBrowser.get(path).statusCode());
        assertEquals(200, browser.send("PATCH", "/api/oficina", Map.of("versao", 0, "perfilPublico", true, "endereco", "Rua de teste, 123")).statusCode());
        var response = publicBrowser.get(path);
        assertEquals(200, response.statusCode());
        var data = mapper.readTree(response.body());
        assertEquals(7, data.size());
        assertFalse(data.has("id"));
        assertFalse(data.has("versao"));
        assertFalse(data.has("perfilPublico"));
        assertEquals("Rua de teste, 123", data.get("endereco").asText());
        assertTrue(response.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
        assertEquals(200, browser.send("PATCH", "/api/oficina", Map.of("versao", 1, "perfilPublico", false)).statusCode());
        assertEquals(404, publicBrowser.get(path).statusCode());
    }
    @Test void logoUploadIsolationPublicationRemovalAndBadFiles() throws Exception {
        var first = owner();
        var second = owner();
        assertEquals(200, first.upload(LogoValidatorTest.png(10, 10), 0).statusCode());
        assertTrue(first.profile().get("temLogo").asBoolean());
        assertEquals(404, second.get("/api/oficina/logo").statusCode());
        assertEquals(200, first.get("/api/oficina/logo").statusCode());
        assertEquals(409, first.upload(LogoValidatorTest.png(10, 10), 0).statusCode());
        assertEquals(415, first.upload("not an image".getBytes(), 1).statusCode());
        assertEquals(413, first.upload(new byte[LogoValidator.MAX_BYTES + 1], 1).statusCode());
        String publicLogo = "/api/publico/oficinas/" + first.profile().get("slug").asText() + "/logo";
        assertEquals(404, new Browser().get(publicLogo).statusCode());
        first.send("PATCH", "/api/oficina", Map.of("versao", 1, "perfilPublico", true));
        assertEquals(200, new Browser().get(publicLogo).statusCode());
        assertEquals(200, first.send("DELETE", "/api/oficina/logo?versao=2", Map.of()).statusCode());
        assertEquals(404, new Browser().get(publicLogo).statusCode());
        assertFalse(first.profile().get("temLogo").asBoolean());
    }
    @Test void requiresSessionAndCsrf() throws Exception {
        var anonymous = new Browser();
        assertEquals(401, anonymous.get("/api/oficina").statusCode());
        assertEquals(401, anonymous.get("/api/oficina/logo").statusCode());
        assertEquals(401, anonymous.send("PATCH", "/api/oficina", Map.of("versao", 0)).statusCode());
        var browser = owner();
        var response = browser.client.send(HttpRequest.newBuilder(uri("/api/oficina"))
            .header("Content-Type", "application/json").method("PATCH", HttpRequest.BodyPublishers.ofString("{\"versao\":0}"))
            .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode());
    }
    @Test void concurrentChangesHaveOneWinnerAndReturnOwnVersion() throws Exception {
        var browser = owner();
        String token = browser.csrf();
        try (var pool = Executors.newFixedThreadPool(2)) {
            var barrier = new CyclicBarrier(2);
            Callable<HttpResponse<String>> change = () -> {
                barrier.await();
                return browser.client.send(HttpRequest.newBuilder(uri("/api/oficina"))
                    .header("Content-Type", "application/json").header("X-CSRF-TOKEN", token)
                    .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"versao\":0,\"nome\":\"Concorrente\"}"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            };
            var first = pool.submit(change);
            var second = pool.submit(change);
            var responses = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertEquals(Set.of(200, 409), Set.of(responses.get(0).statusCode(), responses.get(1).statusCode()));
            var winner = responses.stream().filter(result -> result.statusCode() == 200).findFirst().orElseThrow();
            assertEquals(1, mapper.readTree(winner.body()).get("versao").asLong());
        }
    }
    @Test void unpublishedReplacementIsNeverReturnedByPublicLogo() throws Exception {
        var browser = owner();
        browser.upload(LogoValidatorTest.png(10, 10), 0);
        browser.send("PATCH", "/api/oficina", Map.of("versao", 1, "perfilPublico", true));
        String path = "/api/publico/oficinas/" + browser.profile().get("slug").asText() + "/logo";
        var anonymous = new Browser();
        assertEquals(200, anonymous.get(path).statusCode());
        assertEquals(200, browser.send("PATCH", "/api/oficina", Map.of("versao", 2, "perfilPublico", false)).statusCode());
        assertEquals(404, anonymous.get(path).statusCode());
        assertEquals(200, browser.upload(LogoValidatorTest.png(20, 20), 3).statusCode());
        assertEquals(404, anonymous.get(path).statusCode());
        assertEquals(200, browser.get("/api/oficina/logo").statusCode());
    }
}
