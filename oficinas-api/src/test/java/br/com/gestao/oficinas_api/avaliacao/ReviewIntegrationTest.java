package br.com.gestao.oficinas_api.avaliacao;

import br.com.gestao.oficinas_api.identidade.*;
import br.com.gestao.oficinas_api.notificacoes.*;
import br.com.gestao.oficinas_api.ordem.*;
import br.com.gestao.oficinas_api.support.TestPostgres;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"app.notifications.enabled=false", "spring.session.jdbc.cleanup-cron=-"})
@DirtiesContext
@org.testcontainers.junit.jupiter.Testcontainers
class ReviewIntegrationTest {
    @org.testcontainers.junit.jupiter.Container static final TestPostgres database = new TestPostgres();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", database::getJdbcUrl);
        registry.add("spring.datasource.username", database::getUsername);
        registry.add("spring.datasource.password", database::getPassword);
    }
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired ServiceOrderService orders;
    @Autowired OrderClosureService closures;
    @Autowired ReviewAccessService access;
    @Autowired ReviewService reviews;
    @Autowired NotificationWorker worker;
    @Autowired PasswordEncoder passwords;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean TransactionalEmail email;
    final ObjectMapper mapper = new ObjectMapper();
    static final String PASSWORD = "Oficina-segura-123";
    Identidade owner;
    UUID customer, vehicle;
    ServiceOrder order;

    @BeforeEach void setup() {
        jdbc.update("DELETE FROM notificacao_email_tentativa");
        jdbc.update("DELETE FROM notificacao_email");
        UUID shop = UUID.randomUUID(), id = UUID.randomUUID();
        customer = UUID.randomUUID(); vehicle = UUID.randomUUID(); owner = new Identidade(id, shop, 0, Instant.now());
        jdbc.update("INSERT INTO oficina(id,nome,slug) VALUES (?,?,?)", shop, "Oficina Teste", "teste-" + shop);
        jdbc.update("INSERT INTO proprietario(id,oficina_id,nome,email,senha_hash) VALUES (?,?,?,?,?)", id, shop, "Dono", id + "@example.test", passwords.encode(PASSWORD));
        jdbc.update("INSERT INTO cliente(id,oficina_id,nome,cpf,email,email_verificado_em) VALUES (?,?,?,?,?,now())", customer, shop, "Cliente Secreto", "52998224725", "cliente@example.test");
        jdbc.update("INSERT INTO veiculo(id,oficina_id,placa,marca,modelo) VALUES (?,?,?,?,?)", vehicle, shop, "BRA1E23", "Marca", "Modelo");
        jdbc.update("INSERT INTO vinculo_cliente_veiculo(id,oficina_id,cliente_id,veiculo_id,criado_por) VALUES (?,?,?,?,?)", UUID.randomUUID(), shop, customer, vehicle, id);
        order = orders.create(owner, null, input());
    }
    @Test void createsOneInvitationAtDeliveryAndRollsBackAtomically() {
        assertThrows(ApiException.class, () -> access.invitation(owner, order.id()));
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            deliver(); throw new IllegalStateException("rollback");
        }));
        assertEquals(0, count("avaliacao_convite"));
        deliver();
        var invitation = access.invitation(owner, order.id());
        assertEquals(invitation, access.invitation(owner, order.id()));
        assertEquals(1, count("avaliacao_convite"));
        Instant closed = jdbc.queryForObject("SELECT encerrada_em FROM ordem_servico WHERE id=?", java.sql.Timestamp.class, order.id()).toInstant();
        assertEquals(closed.plus(Duration.ofDays(7)), invitation.expiraEm());
        assertNotEquals(invitation.token(), jdbc.queryForObject("SELECT token_hash FROM avaliacao_convite WHERE ordem_servico_id=?", String.class, order.id()));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM notificacao WHERE oficina_id=? AND evento='AVALIACAO_SOLICITADA'", Integer.class, owner.oficinaId()));
        verifyNoInteractions(email);
    }
    @Test void canceledOrderHasNoInvitation() {
        closures.close(owner, order.id(), new OrderClosurePolicy.Input(ServiceOrderStatus.CANCELADO, "Desistência", true, false, order.versao()));
        assertEquals(0, count("avaliacao_convite"));
        assertThrows(ApiException.class, () -> access.invitation(owner, order.id()));
    }
    @Test void privateSummaryOmitsSensitiveFieldsAndCredentialCannotReactivatePortal() throws Exception {
        orders.publish(owner, order.id(), new ServiceOrderService.UpdateInput("Serviço finalizado", "Custo interno secreto 300", true, order.versao()));
        order = orders.order(owner, order.id());
        deliver(); var invitation = access.invitation(owner, order.id());
        Browser browser = new Browser();
        assertEquals(401, browser.get("/api/portal/avaliacoes/resumo").statusCode());
        assertEquals(403, browser.raw("POST", "/api/portal/avaliacoes/acesso", Map.of("token", invitation.token()), null).statusCode());
        assertEquals(204, browser.send("POST", "/api/portal/avaliacoes/acesso", Map.of("token", invitation.token())).statusCode());
        var summary = browser.get("/api/portal/avaliacoes/resumo");
        assertEquals(200, summary.statusCode()); assertTrue(summary.headers().firstValue("cache-control").orElse("").contains("no-store"));
        assertTrue(summary.body().contains("Serviço finalizado"));
        for (String secret : List.of("Custo interno", "52998224725", "BRA1E23", "Cliente Secreto", "cliente@example.test", "relatoInicial", "clienteId", "fotos")) {
            // Consent wording mentions photos, but no photo data or endpoint is included.
            if (!secret.equals("fotos")) assertFalse(summary.body().contains(secret), secret);
        }
        assertEquals(401, browser.get("/api/portal/veiculos").statusCode());
        assertEquals(401, browser.get("/api/avaliacoes").statusCode());
        assertNotEquals(200, browser.get("/api/portal/ordens-servico/" + order.id() + "/fotos").statusCode());
        assertEquals(400, browser.send("POST", "/api/portal/acesso/link", Map.of("token", invitation.token())).statusCode());
        var returned = orders.create(owner, null, input());
        assertEquals(order.numero(), mapper.readTree(browser.get("/api/portal/avaliacoes/resumo").body()).get("atendimento").get("numero").asLong());
        assertNotEquals(order.id(), returned.id());
        assertEquals(401, browser.get("/api/portal/veiculos").statusCode());
        assertEquals(401, browser.send("POST", "/api/portal/avaliacoes/acesso", Map.of("token", "invalid")).statusCode());
        assertEquals(401, browser.get("/api/portal/avaliacoes/resumo").statusCode());
    }
    @Test void validatesHttpScoresDefaultsConsentAndNeverOverwrites() throws Exception {
        Browser browser = authenticated();
        assertEquals(400, browser.send("POST", "/api/portal/avaliacoes", Map.of("contexto", context(), "nota", 1.5)).statusCode());
        assertEquals(400, browser.send("POST", "/api/portal/avaliacoes", Map.of("contexto", context(), "nota", 6)).statusCode());
        assertEquals(403, browser.raw("POST", "/api/portal/avaliacoes", Map.of("nota", 1), null).statusCode());
        var result = browser.send("POST", "/api/portal/avaliacoes", Map.of("contexto", context(), "nota", 1, "comentario", " Precisa melhorar "));
        assertEquals(200, result.statusCode(), result.body());
        assertFalse(mapper.readTree(result.body()).get("consentimentoPublicacao").asBoolean());
        assertEquals(result.body(), browser.send("POST", "/api/portal/avaliacoes", Map.of("contexto", context(), "nota", 1, "comentario", "Precisa melhorar")).body());
        assertEquals(409, browser.send("POST", "/api/portal/avaliacoes", Map.of("contexto", context(), "nota", 5)).statusCode());
        assertEquals(1, count("avaliacao")); assertEquals(1, count("avaliacao_auditoria"));
        assertNull(jdbc.queryForObject("SELECT consentimento_em FROM avaliacao WHERE ordem_servico_id=?", java.sql.Timestamp.class, order.id()));
    }
    @Test void concurrentSubmissionsAreIdempotentAndConsentHasItsOwnAudit() throws Exception {
        deliver(); var grant = access.exchange(access.invitation(owner, order.id()).token());
        var input = new ReviewPolicy.Input(BigDecimal.valueOf(4), "Bom", true);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var gate = new CountDownLatch(1);
            var first = executor.submit(() -> { gate.await(); return reviews.submit(grant.id(), input); });
            var second = executor.submit(() -> { gate.await(); return reviews.submit(grant.id(), input); });
            gate.countDown(); assertEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        }
        assertEquals(1, count("avaliacao")); assertEquals(1, count("avaliacao_auditoria"));
        assertNotNull(jdbc.queryForObject("SELECT consentimento_em FROM avaliacao WHERE ordem_servico_id=?", java.sql.Timestamp.class, order.id()));
        assertEquals(ReviewPolicy.CONSENT, jdbc.queryForObject("SELECT consentimento_texto FROM avaliacao WHERE ordem_servico_id=?", String.class, order.id()));
    }
    @Test void expiryRevocationAndContactChangesInvalidateExistingSessions() throws Exception {
        Browser browser = authenticated();
        jdbc.update("UPDATE avaliacao_convite SET expira_em=now()-interval '1 second' WHERE ordem_servico_id=?", order.id());
        assertEquals(401, browser.get("/api/portal/avaliacoes/resumo").statusCode());
        assertEquals(401, browser.send("POST", "/api/portal/avaliacoes", Map.of("contexto", context(), "nota", 5)).statusCode());
        jdbc.update("UPDATE avaliacao_convite SET expira_em=now()+interval '1 day' WHERE ordem_servico_id=?", order.id());
        jdbc.update("UPDATE cliente SET email='novo@example.test' WHERE id=?", customer);
        assertEquals(401, browser.get("/api/portal/avaliacoes/resumo").statusCode());
        jdbc.update("UPDATE cliente SET email='cliente@example.test' WHERE id=?", customer);
        assertEquals(200, browser.get("/api/portal/avaliacoes/resumo").statusCode());
        access.revoke(owner, order.id());
        assertEquals(401, browser.get("/api/portal/avaliacoes/resumo").statusCode());
        assertThrows(ApiException.class, () -> access.invitation(owner, order.id()));
    }
    @Test void isolatesOwnerResultsConfigurationAndInvitationsAcrossOffices() throws Exception {
        Browser reviewer = authenticated();
        assertEquals(200, reviewer.send("POST", "/api/portal/avaliacoes", Map.of("contexto", context(), "nota", 2)).statusCode());
        Browser ownerBrowser = new Browser();
        assertEquals(200, ownerBrowser.send("POST", "/api/auth/login", Map.of("email", owner.id() + "@example.test", "senha", PASSWORD)).statusCode());
        assertEquals(1, mapper.readTree(ownerBrowser.get("/api/avaliacoes").body()).get("totalElements").asInt());
        assertEquals(400, ownerBrowser.get("/api/avaliacoes?size=101").statusCode());
        var url = "https://g.page/r/Test/review";
        assertEquals(200, ownerBrowser.send("PATCH", "/api/avaliacoes/configuracao", Map.of("googleUrl", url)).statusCode());
        assertEquals(url, mapper.readTree(reviewer.get("/api/portal/avaliacoes/resumo").body()).get("atendimento").get("googleUrl").asText());
        assertEquals(400, ownerBrowser.send("PATCH", "/api/avaliacoes/configuracao", Map.of("googleUrl", "https://evil.test")).statusCode());
        assertEquals(200, ownerBrowser.send("POST", "/api/ordens-servico/" + order.id() + "/avaliacao/convite", Map.of()).statusCode());
        UUID oldOrder = order.id(); setup();
        assertEquals(0, reviews.list(owner.oficinaId(), 0, 20).totalElements());
        assertNull(reviews.configuration(owner.oficinaId()).googleUrl());
        assertEquals(404, assertThrows(ApiException.class, () -> access.invitation(owner, oldOrder)).status);
        assertEquals(404, assertThrows(ApiException.class, () -> access.revoke(owner, oldOrder)).status);
    }
    @Test void workerSendsOnlyDedicatedValidLinkAndCancelsRevokedInvites() {
        deliver();
        jdbc.update("DELETE FROM notificacao_email WHERE notificacao_id IN (SELECT id FROM notificacao WHERE evento<>'AVALIACAO_SOLICITADA')");
        assertTrue(worker.processOne());
        var body = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(email).send(eq("cliente@example.test"), anyString(), body.capture());
        String token = body.getValue().split("#token=")[1].split("\\s")[0];
        assertEquals(order.id(), access.exchange(token).order());
        assertFalse(body.getValue().contains("BRA1E23")); assertFalse(body.getValue().contains("/acompanhar"));
        assertFalse(worker.processOne());
        reset(email); setup(); deliver(); access.revoke(owner, order.id());
        jdbc.update("DELETE FROM notificacao_email WHERE notificacao_id IN (SELECT id FROM notificacao WHERE evento<>'AVALIACAO_SOLICITADA')");
        assertTrue(worker.processOne()); verifyNoInteractions(email);
        assertEquals("CANCELADO", jdbc.queryForObject("SELECT estado FROM notificacao_email WHERE oficina_id=?", String.class, owner.oficinaId()));
    }
    @Test void staleTabContextCannotWriteRatingOrConsentToAnotherOffice() throws Exception {
        Browser browser = authenticated();
        UUID firstOrder = order.id();
        String firstContext = mapper.readTree(browser.get("/api/portal/avaliacoes/resumo").body()).get("contexto").asText();
        setup(); deliver();
        assertEquals(204, browser.send("POST", "/api/portal/avaliacoes/acesso", Map.of("token", access.invitation(owner, order.id()).token())).statusCode());
        String secondContext = mapper.readTree(browser.get("/api/portal/avaliacoes/resumo").body()).get("contexto").asText();
        assertNotEquals(firstContext, secondContext);
        var stale = browser.send("POST", "/api/portal/avaliacoes", Map.of("contexto", firstContext, "nota", 5, "consentimentoPublicacao", true));
        assertEquals(409, stale.statusCode());
        assertEquals("AVALIACAO_CONTEXTO_ALTERADO", mapper.readTree(stale.body()).get("code").asText());
        assertEquals(409, browser.send("POST", "/api/portal/avaliacoes", Map.of("nota", 5)).statusCode());
        for (String table : List.of("avaliacao", "avaliacao_auditoria")) {
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE ordem_servico_id IN (?,?)", Integer.class, firstOrder, order.id()));
        }
        assertEquals(200, browser.send("POST", "/api/portal/avaliacoes", Map.of("contexto", secondContext, "nota", 2)).statusCode());
        assertEquals(1, count("avaliacao"));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM avaliacao WHERE ordem_servico_id=?", Integer.class, firstOrder));
    }
    @Test void explicitlyEmptyTokenClearsPriorReviewGrant() throws Exception {
        Browser browser = authenticated();
        assertEquals(401, browser.send("POST", "/api/portal/avaliacoes/acesso", Map.of("token", "")).statusCode());
        assertEquals(401, browser.get("/api/portal/avaliacoes/resumo").statusCode());
        assertEquals(401, browser.send("POST", "/api/portal/avaliacoes", Map.of("contexto", context(), "nota", 5)).statusCode());
        assertEquals(0, count("avaliacao"));
    }
    private int count(String table) { return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE ordem_servico_id=?", Integer.class, order.id()); }
    private UUID context() { return jdbc.queryForObject("SELECT id FROM avaliacao_convite WHERE ordem_servico_id=?", UUID.class, order.id()); }
    private void deliver() { closures.close(owner, order.id(), new OrderClosurePolicy.Input(ServiceOrderStatus.ENTREGUE, null, true, false, order.versao())); }
    private ServiceOrderService.CreateInput input() { return new ServiceOrderService.CreateInput(customer, vehicle, "Relato interno", Instant.now().minusSeconds(60), 1000, null); }
    private Browser authenticated() throws Exception {
        deliver(); var browser = new Browser();
        assertEquals(204, browser.send("POST", "/api/portal/avaliacoes/acesso", Map.of("token", access.invitation(owner, order.id()).token())).statusCode());
        return browser;
    }
    private class Browser {
        final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        HttpResponse<String> get(String path) throws Exception { return raw("GET", path, null, null); }
        HttpResponse<String> send(String method, String path, Map<String, ?> body) throws Exception {
            String csrf = mapper.readTree(get("/api/auth/csrf").body()).get("token").asText();
            return raw(method, path, body, csrf);
        }
        HttpResponse<String> raw(String method, String path, Map<String, ?> body, String csrf) throws Exception {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
            if (csrf != null) request.header("X-CSRF-TOKEN", csrf);
            request.header("Content-Type", "application/json");
            return client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
