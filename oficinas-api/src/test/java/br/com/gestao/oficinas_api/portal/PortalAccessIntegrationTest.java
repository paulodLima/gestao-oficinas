package br.com.gestao.oficinas_api.portal;

import br.com.gestao.oficinas_api.notificacoes.TransactionalEmail;
import br.com.gestao.oficinas_api.ordem.PhotoStorage;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import br.com.gestao.oficinas_api.support.TestPostgres;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class PortalAccessIntegrationTest {
    @Container
    static final TestPostgres postgres = new TestPostgres();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired PhotoStorage storage;
    @MockitoBean TransactionalEmail mail;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String PASSWORD = "Oficina-segura-123";

    @BeforeEach void resetRateWindows() { jdbc.update("DELETE FROM auth_limite"); }

    class Browser {
        final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).build();
        String sessionId() {
            return cookies.getCookieStore().getCookies().stream().filter(cookie -> cookie.getName().equals("OFICINAS_SESSION"))
                .map(java.net.HttpCookie::getValue).findFirst().orElseThrow();
        }

        HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> send(String method, String path, Map<String, ?> body) throws Exception {
            return send(method, path, body, Map.of());
        }

        HttpResponse<String> send(String method, String path, Map<String, ?> body, Map<String, String> headers) throws Exception {
            String csrf = mapper.readTree(get("/api/auth/csrf").body()).get("token").asText();
            var builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .header("X-CSRF-TOKEN", csrf)
                .method(method, HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            headers.forEach(builder::header);
            return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    record Fixture(Browser owner, UUID shopId, String slug, UUID customerId, UUID vehicleId,
                   UUID orderId, String email) {}
    record Challenge(UUID id, String code) {}

    @Test
    void additionalApprovalRequiresCurrentVerifiedContactAndThrottlesResends() throws Exception {
        Fixture fixture = fixture("CAD1A23", "52998224725");
        String orderPath = "/api/ordens-servico/" + fixture.orderId();
        var draft = fixture.owner().send("POST", orderPath + "/adicionais", Map.of(
            "problema", "Filtro danificado", "justificativa", "Substituir para corrigir a filtragem",
            "impactoPrazo", "Sem impacto", "fotoIds", java.util.List.of(), "itens", java.util.List.of(
                Map.of("tipo", "PECA", "descricao", "Filtro novo", "quantidade", 1, "valorUnitario", 50, "grupoDependencia", ""))));
        assertEquals(201, draft.statusCode(), draft.body());
        UUID additional = UUID.fromString(mapper.readTree(draft.body()).get("id").asText());
        var sent = fixture.owner().send("POST", orderPath + "/adicionais/" + additional + "/envio", Map.of("expectedVersion", 0));
        assertEquals(200, sent.statusCode(), sent.body());
        JsonNode current = mapper.readTree(sent.body());
        long version = current.get("versao").asLong();
        String itemId = current.get("versoes").get(0).get("itens").get(0).get("id").asText();
        String token = mapper.readTree(fixture.owner().send("POST", orderPath + "/acesso", Map.of()).body()).get("token").asText();
        Browser customer = new Browser();
        assertEquals(204, customer.send("POST", "/api/portal/acesso/link", Map.of("token", token)).statusCode());
        String path = "/api/portal/ordens-servico/" + fixture.orderId() + "/adicionais/" + additional;
        reset(mail);
        var issued = customer.send("POST", path + "/codigo", Map.of());
        assertEquals(200, issued.statusCode(), issued.body());
        UUID id = UUID.fromString(mapper.readTree(issued.body()).get("desafioId").asText());
        var content = ArgumentCaptor.forClass(String.class);
        verify(mail).send(eq(fixture.email()), anyString(), content.capture());
        var matcher = Pattern.compile("código é: ([0-9]{6})").matcher(content.getValue());
        assertTrue(matcher.find());
        var input = Map.of("desafioId", id, "codigo", matcher.group(1), "versao", version,
            "decisoes", java.util.List.of(Map.of("bloco", "item:" + itemId, "decisao", "APROVADO")));
        assertEquals(429, customer.send("POST", path + "/codigo", Map.of()).statusCode());
        assertNull(jdbc.queryForObject("SELECT usado_em FROM adicional_desafio WHERE id=?", java.sql.Timestamp.class, id));
        verify(mail, times(1)).send(eq(fixture.email()), anyString(), anyString());
        assertEquals(200, fixture.owner().send("PATCH", "/api/clientes/" + fixture.customerId(),
            Map.of("versao", 0, "email", "new-contact@customer.test")).statusCode());
        assertEquals(400, customer.send("POST", path + "/decisoes", input,
            Map.of("Idempotency-Key", "unverified-contact")).statusCode());
        jdbc.update("UPDATE cliente SET email_verificado_em=now() WHERE id=?", fixture.customerId());
        var rejected = customer.send("POST", path + "/decisoes", input, Map.of("Idempotency-Key", "old-contact"));
        assertEquals(400, rejected.statusCode(), rejected.body());
        assertEquals(200, fixture.owner().send("PATCH", "/api/clientes/" + fixture.customerId(),
            Map.of("versao", 1, "email", fixture.email())).statusCode());
        jdbc.update("UPDATE cliente SET email_verificado_em=now() WHERE id=?", fixture.customerId());
        assertEquals(400, customer.send("POST", path + "/decisoes", input,
            Map.of("Idempotency-Key", "returned-contact")).statusCode());
        assertEquals(200, fixture.owner().send("PATCH", "/api/clientes/" + fixture.customerId(), Map.of("versao", 2, "ativo", false)).statusCode());
        assertEquals(200, fixture.owner().send("PATCH", "/api/clientes/" + fixture.customerId(), Map.of("versao", 3, "ativo", true)).statusCode());
        assertEquals(400, customer.send("POST", path + "/decisoes", input,
            Map.of("Idempotency-Key", "reactivated-contact")).statusCode());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM adicional_item_decisao WHERE solicitacao_id=?", Integer.class, additional));
        jdbc.update("UPDATE adicional_desafio SET created_at=now()-interval '61 seconds' WHERE id=?", id);
        reset(mail);
        var fresh = customer.send("POST", path + "/codigo", Map.of());
        assertEquals(200, fresh.statusCode(), fresh.body());
        var newContent = ArgumentCaptor.forClass(String.class);
        verify(mail).send(eq(fixture.email()), anyString(), newContent.capture());
        var newMatcher = Pattern.compile("código é: ([0-9]{6})").matcher(newContent.getValue());
        assertTrue(newMatcher.find());
        var accepted = customer.send("POST", path + "/decisoes", Map.of(
            "desafioId", mapper.readTree(fresh.body()).get("desafioId").asText(), "codigo", newMatcher.group(1),
            "versao", version, "decisoes", input.get("decisoes")), Map.of("Idempotency-Key", "new-contact"));
        assertEquals(200, accepted.statusCode(), accepted.body());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM adicional_item_decisao WHERE solicitacao_id=?", Integer.class, additional));
    }

    @Test
    void deliveryFailureRollsBackChallengeAndNeverLogsProviderSecrets(CapturedOutput output) throws Exception {
        Fixture fixture = fixture("LOG1A23", "52998224725");
        reset(mail);
        String secret = "private-provider-token-52998224725";
        doThrow(new IllegalStateException(secret)).when(mail).send(anyString(), anyString(), anyString());
        var response = new Browser().send("POST", "/api/portal/acesso/codigo",
            Map.of("oficinaSlug", fixture.slug(), "placa", "LOG1A23"));
        assertEquals(202, response.statusCode());
        UUID challengeId = UUID.fromString(mapper.readTree(response.body()).get("desafioId").asText());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM portal_desafio WHERE id=?", Integer.class, challengeId));
        assertFalse(response.body().contains(secret));
        assertFalse(output.getAll().contains(secret));
        assertFalse(output.getAll().contains(fixture.email()));
        reset(mail);
    }

    @Test
    void transferThroughApiRevokesPreviousCustomerAccessAndOldCodes() throws Exception {
        Fixture fixture = fixture("TRF1A23", "52998224725");
        Browser visitor = new Browser();
        Challenge used = challenge(fixture, visitor);
        assertEquals(204, visitor.send("POST", "/api/portal/acesso/validacao",
            Map.of("desafioId", used.id(), "codigo", used.code())).statusCode());
        Challenge pending = challenge(fixture, new Browser());
        var created = fixture.owner().send("POST", "/api/clientes", Map.of("nome", "Novo responsável",
            "cpf", "16899535009", "telefone", "", "email", UUID.randomUUID() + "@customer.test"));
        assertEquals(201, created.statusCode());
        UUID nextCustomer = UUID.fromString(mapper.readTree(created.body()).get("id").asText());
        String transfer = "/api/veiculos/" + fixture.vehicleId() + "/transferencias";
        assertEquals(409, fixture.owner().send("POST", transfer, Map.of("novoClienteId", nextCustomer, "expectedVersion", 0)).statusCode());
        assertEquals(200, fixture.owner().send("POST", "/api/ordens-servico/" + fixture.orderId() + "/encerramento",
            Map.of("tipo", "ENTREGUE", "confirmado", true, "cancelarPendencias", false, "expectedVersion", 0)).statusCode());
        assertEquals(200, fixture.owner().send("POST", transfer, Map.of("novoClienteId", nextCustomer, "expectedVersion", 0)).statusCode());
        var next = fixture.owner().send("POST", "/api/ordens-servico", Map.of("clienteId", nextCustomer,
            "veiculoId", fixture.vehicleId(), "relatoInicial", "Revisão do novo responsável",
            "entradaEm", Instant.now().minusSeconds(5).toString(), "kmEntrada", 15000));
        assertEquals(201, next.statusCode(), next.body());
        UUID nextOrder = UUID.fromString(mapper.readTree(next.body()).get("id").asText());
        assertTrue(mapper.readTree(visitor.get("/api/portal/veiculos").body()).isEmpty());
        assertTrue(mapper.readTree(visitor.get("/api/portal/servico-atual").body()).get("servico").isNull());
        assertEquals(404, visitor.get("/api/portal/ordens-servico/" + nextOrder + "/atualizacoes").statusCode());
        assertEquals(400, new Browser().send("POST", "/api/portal/acesso/validacao",
            Map.of("desafioId", pending.id(), "codigo", pending.code())).statusCode());
    }

    @Test
    void suppressesRepeatedDeliveryWithoutRevealingAccountsAndInvalidatesPreviousCodes() throws Exception {
        Fixture fixture = fixture("ENV1A23", "52998224725");
        Browser visitor = new Browser();
        Challenge first = challenge(fixture, visitor);
        reset(mail);
        var suppressed = visitor.send("POST", "/api/portal/acesso/codigo",
            Map.of("oficinaSlug", fixture.slug(), "placa", "ENV1A23"));
        assertEquals(202, suppressed.statusCode());
        verifyNoInteractions(mail);
        UUID hiddenId = UUID.fromString(mapper.readTree(suppressed.body()).get("desafioId").asText());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM portal_desafio WHERE id=?", Integer.class, hiddenId));
        Challenge next = challenge(fixture, visitor);
        assertEquals(400, visitor.send("POST", "/api/portal/acesso/validacao",
            Map.of("desafioId", first.id(), "codigo", first.code())).statusCode());
        assertEquals(204, visitor.send("POST", "/api/portal/acesso/validacao",
            Map.of("desafioId", next.id(), "codigo", next.code())).statusCode());
        jdbc.update("UPDATE portal_desafio SET created_at=now()-interval '61 seconds' WHERE cliente_id=?", fixture.customerId());
        reset(mail);
        for (int attempt = 0; attempt < 35; attempt++) {
            assertEquals(202, visitor.send("POST", "/api/portal/acesso/codigo",
                Map.of("oficinaSlug", fixture.slug(), "placa", "ZZZ9Z99")).statusCode());
        }
        assertEquals(202, visitor.send("POST", "/api/portal/acesso/codigo",
            Map.of("oficinaSlug", fixture.slug(), "placa", "ENV1A23")).statusCode());
        verifyNoInteractions(mail);
    }

    @Test
    void rotatesSessionForCodeAndLinkAndInvalidLinkClearsOnlyOperationalGrant() throws Exception {
        Fixture fixture = fixture("SES1A23", "52998224725");
        Browser owner = fixture.owner();
        Challenge challenge = challenge(fixture, owner);
        String beforeCode = owner.sessionId();
        assertEquals(204, owner.send("POST", "/api/portal/acesso/validacao",
            Map.of("desafioId", challenge.id(), "codigo", challenge.code())).statusCode());
        assertNotEquals(beforeCode, owner.sessionId());
        String token = mapper.readTree(owner.send("POST", "/api/ordens-servico/" + fixture.orderId() + "/acesso", Map.of()).body())
            .get("token").asText();
        String beforeLink = owner.sessionId();
        assertEquals(204, owner.send("POST", "/api/portal/acesso/link", Map.of("token", token)).statusCode());
        assertNotEquals(beforeLink, owner.sessionId());
        assertEquals(400, owner.send("POST", "/api/portal/acesso/link", Map.of("token", "")).statusCode());
        assertEquals(401, owner.get("/api/portal/servico-atual").statusCode());
        assertEquals(200, owner.get("/api/auth/me").statusCode());
        assertEquals(204, owner.send("POST", "/api/portal/acesso/link", Map.of("token", token)).statusCode());
        assertEquals(204, owner.send("DELETE", "/api/ordens-servico/" + fixture.orderId() + "/acesso", Map.of()).statusCode());
        assertEquals(401, owner.get("/api/portal/servico-atual").statusCode());
        assertEquals(200, owner.get("/api/auth/me").statusCode());
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM cadastro_auditoria WHERE recurso_id=? AND acao LIKE 'LINK_OPERACIONAL_%'",
            Integer.class, fixture.orderId()));
    }

    @Test
    void dormantSessionAndPendingCodeNeverReviveAfterContactChangeAndReverification() throws Exception {
        Fixture fixture = fixture("REV1A23", "52998224725");
        Browser visitor = new Browser();
        Challenge first = challenge(fixture, visitor);
        assertEquals(204, visitor.send("POST", "/api/portal/acesso/validacao",
            Map.of("desafioId", first.id(), "codigo", first.code())).statusCode());
        Challenge pending = challenge(fixture, new Browser());
        String path = "/api/clientes/" + fixture.customerId();
        assertEquals(200, fixture.owner().send("PATCH", path, Map.of("versao", 0, "email", "changed@customer.test")).statusCode());
        assertEquals(200, fixture.owner().send("PATCH", path, Map.of("versao", 1, "email", fixture.email())).statusCode());
        jdbc.update("UPDATE cliente SET email_verificado_em=now() WHERE id=?", fixture.customerId());
        assertEquals(401, visitor.get("/api/portal/veiculos").statusCode());
        assertEquals(400, new Browser().send("POST", "/api/portal/acesso/validacao",
            Map.of("desafioId", pending.id(), "codigo", pending.code())).statusCode());
        Challenge fresh = challenge(fixture, visitor);
        assertEquals(204, visitor.send("POST", "/api/portal/acesso/validacao",
            Map.of("desafioId", fresh.id(), "codigo", fresh.code())).statusCode());
        assertEquals(200, fixture.owner().send("PATCH", path, Map.of("versao", 2, "ativo", false)).statusCode());
        assertEquals(200, fixture.owner().send("PATCH", path, Map.of("versao", 3, "ativo", true)).statusCode());
        assertEquals(401, visitor.get("/api/portal/veiculos").statusCode());
    }

    @Test
    void privateDownloadsEnforceOfficeCustomerPublicationAndRevocation() throws Exception {
        Fixture fixture = fixture("FOT1A23", "52998224725");
        Fixture outsider = fixture("FOT2A23", "16899535009");
        Fixture neighbor = createVisit(fixture.owner(), fixture.shopId(), fixture.slug(), "FOT3A23", "16899535009");
        UUID ownerId = jdbc.queryForObject("SELECT criado_por FROM ordem_servico WHERE id=?", UUID.class, fixture.orderId());
        var bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(8, 8, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", bytes);
        var stored = storage.store(new org.springframework.mock.web.MockMultipartFile("file", "synthetic.png", "image/png", bytes.toByteArray()));
        try {
            UUID photo = insertPhoto(fixture, ownerId, "RECEBIDO", true, "synthetic.png", "1 minute");
            UUID privatePhoto = insertPhoto(fixture, ownerId, "RECEBIDO", false, "private.png", "1 minute");
            jdbc.update("UPDATE ordem_servico_foto SET chave_arquivo=?,chave_miniatura=?,tipo_conteudo=? WHERE id IN (?,?)",
                stored.key(), stored.thumbnailKey(), stored.contentType(), photo, privatePhoto);
            String publicPath = "/api/portal/ordens-servico/" + fixture.orderId() + "/fotos/" + photo + "/conteudo?tamanho=original";
            String ownerPath = "/api/ordens-servico/" + fixture.orderId() + "/fotos/" + photo + "/arquivo";
            Browser customer = new Browser();
            Challenge challenge = challenge(fixture, customer);
            assertEquals(204, customer.send("POST", "/api/portal/acesso/validacao",
                Map.of("desafioId", challenge.id(), "codigo", challenge.code())).statusCode());
            var content = customer.client.send(HttpRequest.newBuilder(uri(publicPath)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, content.statusCode());
            assertArrayEquals(bytes.toByteArray(), content.body());
            assertTrue(content.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
            assertEquals(401, new Browser().get(publicPath).statusCode());
            assertEquals(200, fixture.owner().get(ownerPath).statusCode());
            assertEquals(401, customer.get(ownerPath).statusCode());
            assertEquals(404, outsider.owner().get(ownerPath).statusCode());
            assertEquals(404, customer.get(publicPath.replace(photo.toString(), privatePhoto.toString())).statusCode());
            Browser otherCustomer = new Browser();
            Challenge otherChallenge = challenge(outsider, otherCustomer);
            assertEquals(204, otherCustomer.send("POST", "/api/portal/acesso/validacao",
                Map.of("desafioId", otherChallenge.id(), "codigo", otherChallenge.code())).statusCode());
            assertEquals(404, otherCustomer.get(publicPath).statusCode());
            Browser neighborBrowser = new Browser();
            Challenge neighborCode = challenge(neighbor, neighborBrowser);
            assertEquals(204, neighborBrowser.send("POST", "/api/portal/acesso/validacao",
                Map.of("desafioId", neighborCode.id(), "codigo", neighborCode.code())).statusCode());
            assertEquals(404, neighborBrowser.get(publicPath).statusCode());
            jdbc.update("UPDATE ordem_servico_foto SET publicada=false WHERE id=?", photo);
            assertEquals(404, customer.get(publicPath).statusCode());
        } finally {
            storage.delete(stored.key());
            storage.delete(stored.thumbnailKey());
        }
    }

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
        String wrongCode = "999999".equals(limited.code()) ? "000000" : "999999";
        for (int attempt = 0; attempt < 5; attempt++) {
            assertEquals(400, anonymous.send("POST", "/api/portal/acesso/validacao",
                Map.of("desafioId", limited.id(), "codigo", wrongCode)).statusCode());
            assertEquals(attempt + 1, jdbc.queryForObject("SELECT tentativas FROM portal_desafio WHERE id=?",
                Integer.class, limited.id()));
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
        assertTrue(Duration.between(Instant.now(), Instant.parse(link.get("expiraEm").asText())).toSeconds()
            > Duration.ofDays(7).minusMinutes(1).toSeconds());
        assertFalse(created.body().contains("52998224725"));
        assertNotEquals(link.get("token").asText(), jdbc.queryForObject(
            "SELECT token_hash FROM portal_link_os WHERE ordem_servico_id=?", String.class, fixture.orderId()));
        assertEquals(404, outsider.owner().send("POST",
            "/api/ordens-servico/" + fixture.orderId() + "/acesso", Map.of()).statusCode());
        assertEquals(404, outsider.owner().send("DELETE",
            "/api/ordens-servico/" + fixture.orderId() + "/acesso", Map.of()).statusCode());

        Browser visitor = new Browser();
        assertEquals(204, visitor.send("POST", "/api/portal/acesso/link",
            Map.of("token", link.get("token").asText())).statusCode());
        assertEquals(fixture.orderId().toString(), mapper.readTree(visitor.get("/api/portal/servico-atual").body())
            .get("servico").get("id").asText());
        assertEquals(404, visitor.get("/api/portal/ordens-servico/" + outsider.orderId() + "/atualizacoes").statusCode());
        assertTrue(mapper.readTree(visitor.get("/api/portal/veiculos").body()).isEmpty());
        assertEquals(401, visitor.send("POST", "/api/ordens-servico/" + fixture.orderId() + "/status",
            Map.of("status", "EM_DIAGNOSTICO", "expectedVersion", 0)).statusCode());

        assertEquals(204, fixture.owner().send("DELETE",
            "/api/ordens-servico/" + fixture.orderId() + "/acesso", Map.of()).statusCode());
        assertEquals(401, visitor.get("/api/portal/servico-atual").statusCode());

        JsonNode expiring = mapper.readTree(fixture.owner().send("POST",
            "/api/ordens-servico/" + fixture.orderId() + "/acesso", Map.of()).body());
        Browser expiringSession = new Browser();
        assertEquals(204, expiringSession.send("POST", "/api/portal/acesso/link",
            Map.of("token", expiring.get("token").asText())).statusCode());
        jdbc.update("UPDATE portal_link_os SET expira_em=now()-interval '1 second' WHERE oficina_id=? AND ordem_servico_id=? AND revogado_em IS NULL",
            fixture.shopId(), fixture.orderId());
        assertEquals(400, new Browser().send("POST", "/api/portal/acesso/link",
            Map.of("token", expiring.get("token").asText())).statusCode());
        assertEquals(401, expiringSession.get("/api/portal/servico-atual").statusCode());
    }

    @Test
    void reissueInvalidatesOldLinkAndSessionAndClosureStopsSharing() throws Exception {
        Fixture fixture = fixture("REN1A23", "52998224725");
        String path = "/api/ordens-servico/" + fixture.orderId() + "/acesso";
        String firstToken = mapper.readTree(fixture.owner().send("POST", path, Map.of()).body()).get("token").asText();
        Browser previous = new Browser();
        assertEquals(204, previous.send("POST", "/api/portal/acesso/link", Map.of("token", firstToken)).statusCode());
        String nextToken = mapper.readTree(fixture.owner().send("POST", path, Map.of()).body()).get("token").asText();
        assertNotEquals(firstToken, nextToken);
        assertEquals(401, previous.get("/api/portal/servico-atual").statusCode());
        assertEquals(400, new Browser().send("POST", "/api/portal/acesso/link", Map.of("token", firstToken)).statusCode());
        Browser current = new Browser();
        assertEquals(204, current.send("POST", "/api/portal/acesso/link", Map.of("token", nextToken)).statusCode());
        jdbc.update("UPDATE ordem_servico SET status='ENTREGUE',encerrada_em=now() WHERE id=?", fixture.orderId());
        assertEquals(401, current.get("/api/portal/servico-atual").statusCode());
        assertEquals(400, new Browser().send("POST", "/api/portal/acesso/link", Map.of("token", nextToken)).statusCode());
        assertEquals(409, fixture.owner().send("POST", path, Map.of()).statusCode());
        assertEquals(204, fixture.owner().send("DELETE", path, Map.of()).statusCode());
    }

    @Test
    void probingPortalWithoutGrantDoesNotInvalidateOwnerSession() throws Exception {
        Fixture fixture = fixture("PRO1A23", "52998224725");
        assertEquals(401, fixture.owner().get("/api/portal/veiculos").statusCode());
        assertEquals(200, fixture.owner().get("/api/auth/me").statusCode());
    }

    @Test
    void concurrentIssuanceLeavesOnlyOneLiveLink() throws Exception {
        Fixture fixture = fixture("CON1A23", "52998224725");
        String path = "/api/ordens-servico/" + fixture.orderId() + "/acesso";
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<HttpResponse<String>> issue = () -> {
                ready.countDown();
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return fixture.owner().send("POST", path, Map.of());
            };
            var first = executor.submit(issue);
            var second = executor.submit(issue);
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            assertEquals(200, first.get(20, TimeUnit.SECONDS).statusCode());
            assertEquals(200, second.get(20, TimeUnit.SECONDS).statusCode());
        }
        assertEquals(1, jdbc.queryForObject("""
            SELECT count(*) FROM portal_link_os WHERE ordem_servico_id=? AND revogado_em IS NULL
            """, Integer.class, fixture.orderId()));
    }

    @Test
    void returnsOnlyPublicPortalDataAndKeepsPublishedPhotosFromPreviousStages() throws Exception {
        Fixture fixture = fixture("GAL1A23", "52998224725");
        jdbc.update("UPDATE oficina SET telefone=?,email_contato=? WHERE id=?",
            "(61) 3333-4444", "contato@oficina.test", fixture.shopId());
        UUID ownerId = jdbc.queryForObject("SELECT criado_por FROM ordem_servico WHERE id=?", UUID.class,
            fixture.orderId());
        UUID publicEvent = UUID.randomUUID();
        UUID privateEvent = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO ordem_servico_evento(id,oficina_id,ordem_servico_id,tipo,texto_publico,
              texto_interno,publicada,autor_id,created_at)
            VALUES (?,?,?,'ATUALIZACAO','Diagnóstico concluído.','custo interno sigiloso',true,?,now()-interval '1 day')
            """, publicEvent, fixture.shopId(), fixture.orderId(), ownerId);
        jdbc.update("""
            INSERT INTO ordem_servico_evento(id,oficina_id,ordem_servico_id,tipo,texto_interno,
              publicada,autor_id)
            VALUES (?,?,?,'ATUALIZACAO','não publicar diagnóstico',false,?)
            """, privateEvent, fixture.shopId(), fixture.orderId(), ownerId);

        UUID entrancePhoto = insertPhoto(fixture, ownerId, "RECEBIDO", true, "entrada.jpg", "2 days");
        UUID currentPhoto = insertPhoto(fixture, ownerId, "EM_MANUTENCAO", true, "motor.jpg", "1 hour");
        UUID privatePhoto = insertPhoto(fixture, ownerId, "EM_DIAGNOSTICO", false, "interna.jpg", "30 minutes");
        jdbc.update("UPDATE ordem_servico SET status='EM_MANUTENCAO' WHERE id=?", fixture.orderId());

        Browser customer = new Browser();
        Challenge challenge = challenge(fixture, customer);
        assertEquals(204, customer.send("POST", "/api/portal/acesso/validacao",
            Map.of("desafioId", challenge.id(), "codigo", challenge.code())).statusCode());

        JsonNode current = mapper.readTree(customer.get("/api/portal/servico-atual?veiculoId="
            + fixture.vehicleId()).body());
        assertEquals("(61) 3333-4444", current.get("oficina").get("telefone").asText());
        JsonNode service = current.get("servico");
        assertEquals("EM_MANUTENCAO", service.get("status").asText());
        assertTrue(service.has("ultimaAtualizacao"));
        assertFalse(service.has("relatoInicial"));
        assertFalse(service.has("clienteId"));
        assertFalse(service.has("observacaoInterna"));

        JsonNode updates = mapper.readTree(customer.get("/api/portal/ordens-servico/"
            + fixture.orderId() + "/atualizacoes").body());
        assertEquals(1, updates.size());
        assertEquals(publicEvent.toString(), updates.get(0).get("id").asText());
        assertFalse(updates.get(0).has("autor"));
        assertFalse(updates.toString().contains("sigiloso"));
        assertFalse(updates.toString().contains(privateEvent.toString()));

        JsonNode photos = mapper.readTree(customer.get("/api/portal/ordens-servico/"
            + fixture.orderId() + "/fotos").body());
        assertEquals(2, photos.size());
        assertEquals(entrancePhoto.toString(), photos.get(0).get("id").asText());
        assertEquals(currentPhoto.toString(), photos.get(1).get("id").asText());
        assertFalse(photos.toString().contains(privatePhoto.toString()));

        jdbc.update("UPDATE ordem_servico SET status='ENTREGUE',encerrada_em=now() WHERE id=?",
            fixture.orderId());
        JsonNode empty = mapper.readTree(customer.get("/api/portal/servico-atual?veiculoId="
            + fixture.vehicleId()).body());
        assertTrue(empty.get("servico").isNull());
        assertEquals("contato@oficina.test", empty.get("oficina").get("email").asText());
    }

    @Test
    void realClosureRevokesOpenLinkSessionButIdentitySeesNewVisitAndOldHistoryStaysInternal() throws Exception {
        Fixture fixture = fixture("RET1A23", "52998224725");
        Fixture other = fixture("DIF1A23", "16899535009");
        String path = "/api/ordens-servico/" + fixture.orderId();
        Browser identity = new Browser(), exclusive = new Browser();
        Challenge challenge = challenge(fixture, identity);
        assertEquals(204, identity.send("POST", "/api/portal/acesso/validacao",
            Map.of("desafioId", challenge.id(), "codigo", challenge.code())).statusCode());
        String token = mapper.readTree(fixture.owner().send("POST", path + "/acesso", Map.of()).body()).get("token").asText();
        assertEquals(204, exclusive.send("POST", "/api/portal/acesso/link", Map.of("token", token)).statusCode());
        assertEquals(200, exclusive.get("/api/portal/servico-atual").statusCode());
        var input = Map.of("tipo", "ENTREGUE", "confirmado", true, "cancelarPendencias", false, "expectedVersion", 0);
        assertEquals(404, other.owner().send("POST", path + "/encerramento", input).statusCode());
        assertEquals(404, other.owner().get(path + "/encerramento").statusCode());
        assertEquals(401, new Browser().get(path + "/encerramento").statusCode());
        assertEquals(400, fixture.owner().send("POST", path + "/encerramento", Map.of("tipo", "ENTREGUE", "expectedVersion", 0)).statusCode());
        var closed = fixture.owner().send("POST", path + "/encerramento", input);
        assertEquals(200, closed.statusCode(), closed.body());
        assertEquals("ENTREGUE", mapper.readTree(closed.body()).get("status").asText());
        assertEquals(401, exclusive.get("/api/portal/servico-atual").statusCode());
        assertTrue(mapper.readTree(identity.get("/api/portal/servico-atual?veiculoId=" + fixture.vehicleId()).body()).get("servico").isNull());
        assertEquals(404, identity.get("/api/portal/ordens-servico/" + fixture.orderId() + "/atualizacoes").statusCode());
        assertEquals(409, fixture.owner().send("DELETE", path + "/fotos/" + UUID.randomUUID(), Map.of()).statusCode());
        assertEquals(409, fixture.owner().send("POST", path + "/acesso", Map.of()).statusCode());
        var next = fixture.owner().send("POST", "/api/ordens-servico", Map.of("clienteId", fixture.customerId(),
            "veiculoId", fixture.vehicleId(), "relatoInicial", "Nova visita para revisão completa", "entradaEm", Instant.now().minusSeconds(5).toString(), "kmEntrada", 15000));
        assertEquals(201, next.statusCode(), next.body());
        String nextId = mapper.readTree(next.body()).get("id").asText();
        assertNotEquals(fixture.orderId().toString(), nextId);
        assertEquals(nextId, mapper.readTree(identity.get("/api/portal/servico-atual?veiculoId=" + fixture.vehicleId()).body()).get("servico").get("id").asText());
        assertEquals(400, new Browser().send("POST", "/api/portal/acesso/link", Map.of("token", token)).statusCode());
        assertEquals(401, exclusive.get("/api/portal/ordens-servico/" + nextId + "/atualizacoes").statusCode());
        assertEquals(2, mapper.readTree(fixture.owner().get(path + "/atualizacoes").body()).size());
        assertEquals("Dono", mapper.readTree(fixture.owner().get(path + "/encerramento").body()).get("encerramento").get("autorNome").asText());
    }

    private UUID insertPhoto(Fixture fixture, UUID ownerId, String stage, boolean published,
                             String name, String age) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO ordem_servico_foto(id,oficina_id,ordem_servico_id,etapa,legenda,publicada,
              estado,nome_arquivo,tipo_conteudo,tamanho_bytes,chave_arquivo,upload_id,criado_por,created_at)
            VALUES (?,?,?,?,?,?,'PRONTA',?,'image/jpeg',100,?,?,?,now()-(?::interval))
            """, id, fixture.shopId(), fixture.orderId(), stage, name, published, name,
            "portal-test/" + id, UUID.randomUUID(), ownerId, age);
        return id;
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
        String slug = mapper.readTree(owner.get("/api/oficina").body()).get("slug").asText();
        return createVisit(owner, shopId, slug, plate, cpf);
    }

    private Fixture createVisit(Browser owner, UUID shopId, String slug, String plate, String cpf) throws Exception {
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
        jdbc.update("UPDATE portal_desafio SET created_at=now()-interval '61 seconds' WHERE cliente_id=?", fixture.customerId());
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
