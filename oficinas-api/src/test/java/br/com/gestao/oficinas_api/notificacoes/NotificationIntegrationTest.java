package br.com.gestao.oficinas_api.notificacoes;

import br.com.gestao.oficinas_api.adicional.*;
import br.com.gestao.oficinas_api.identidade.*;
import br.com.gestao.oficinas_api.ordem.*;
import br.com.gestao.oficinas_api.support.TestPostgres;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"app.notifications.enabled=false", "spring.session.jdbc.cleanup-cron=-"})
@DirtiesContext
@org.testcontainers.junit.jupiter.Testcontainers
class NotificationIntegrationTest {
    @org.testcontainers.junit.jupiter.Container static final TestPostgres database = new TestPostgres();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", database::getJdbcUrl);
        registry.add("spring.datasource.username", database::getUsername);
        registry.add("spring.datasource.password", database::getPassword);
    }
    @org.springframework.boot.test.web.server.LocalServerPort int port;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;
    @Autowired JdbcTemplate jdbc;
    @Autowired NotificationService notifications;
    @Autowired NotificationWorker worker;
    @Autowired ServiceOrderService orders;
    @Autowired AdditionalRequestService requests;
    @Autowired AdditionalDecisionService decisions;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean TransactionalEmail email;
    Fixture fixture;
    @BeforeEach void setup() {
        jdbc.update("DELETE FROM notificacao_email_tentativa");
        jdbc.update("DELETE FROM notificacao_email");
        jdbc.update("DELETE FROM notificacao");
        fixture = fixture(true);
    }
    @Test void openingIsAtomicDeduplicatedAndDoesNotCallSmtp() {
        UUID key = UUID.randomUUID();
        var input = input(fixture);
        var order = orders.create(fixture.owner(), key, input);
        assertEquals(order.id(), orders.create(fixture.owner(), key, input).id());
        assertEquals(1, count("notificacao"));
        assertEquals(1, count("notificacao_email"));
        verifyNoInteractions(email);
        Fixture rollback = fixture(true);
        assertThrows(IllegalStateException.class, () -> transaction().executeWithoutResult(status -> {
            orders.create(rollback.owner(), null, input(rollback));
            throw new IllegalStateException("rollback business operation");
        }));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM ordem_servico WHERE oficina_id=?",
            Integer.class, rollback.owner().oficinaId()));
        assertEquals(1, count("notificacao"));
    }
    @Test void unverifiedCustomerGetsOnlyInternalNoticeEvenAfterReplay() {
        jdbc.update("UPDATE cliente SET email_verificado_em=NULL WHERE id=?", fixture.customer());
        var key = UUID.randomUUID();
        var input = input(fixture);
        orders.create(fixture.owner(), key, input);
        jdbc.update("UPDATE cliente SET email_verificado_em=now() WHERE id=?", fixture.customer());
        orders.create(fixture.owner(), key, input);
        assertEquals(1, count("notificacao"));
        assertEquals(0, count("notificacao_email"));
        assertEquals("SEM_EMAIL_VERIFICADO", notices().getFirst().emailEstado());
        assertFalse(worker.processOne());
    }
    @Test void smtpFailureDoesNotUndoOrderAndRetriesKeepSameIdentity() {
        var order = open();
        UUID notice = notices().getFirst().id();
        UUID emailId = jdbc.queryForObject("SELECT id FROM notificacao_email", UUID.class);
        doThrow(new ApiException(503, "EMAIL_INDISPONIVEL", "provider private detail")).when(email)
            .send(anyString(), anyString(), anyString());
        for (int attempt = 1; attempt <= 5; attempt++) {
            jdbc.update("UPDATE notificacao_email SET proxima_tentativa_em=now()-interval '1 second'");
            assertTrue(worker.processOne());
            assertEquals(attempt, notices().getFirst().tentativas());
        }
        assertEquals("FALHOU", notices().getFirst().emailEstado());
        assertFalse(worker.processOne());
        assertNotNull(orders.order(fixture.owner(), order.id()));
        notifications.retry(fixture.owner().oficinaId(), notice);
        assertThrows(ApiException.class, () -> notifications.retry(fixture.owner().oficinaId(), notice));
        doNothing().when(email).send(anyString(), anyString(), anyString());
        assertTrue(worker.processOne());
        assertEquals("ENVIADO", notices().getFirst().emailEstado());
        assertEquals(emailId, jdbc.queryForObject("SELECT id FROM notificacao_email", UUID.class));
        assertEquals(6, count("notificacao_email_tentativa"));
        assertFalse(worker.processOne());
        assertNull(notices().getFirst().ultimoErro());
    }
    @Test void changedEmailCancelsWithoutSending() {
        open();
        jdbc.update("UPDATE cliente SET email='new@example.test',email_verificado_em=now() WHERE id=?", fixture.customer());
        assertTrue(worker.processOne());
        assertEquals("CANCELADO", notices().getFirst().emailEstado());
        assertEquals("CONTATO_ALTERADO", notices().getFirst().ultimoErro());
        verifyNoInteractions(email);
    }
    @Test void deactivatedCustomerIsNotEmailed() {
        open();
        jdbc.update("UPDATE cliente SET ativo=false WHERE id=?", fixture.customer());
        assertTrue(worker.processOne());
        assertEquals("CANCELADO", notices().getFirst().emailEstado());
        verifyNoInteractions(email);
    }
    @Test void emailProvidesTheCorrectShopIdentifierRequiredByThePortal() {
        open();
        Fixture other = fixture(true);
        orders.create(other.owner(), null, input(other));
        assertTrue(worker.processOne());
        assertTrue(worker.processOne());
        var messages = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(email, times(2)).send(eq("client@example.test"), anyString(), messages.capture());
        String firstSlug = "oficina-" + fixture.owner().oficinaId();
        String secondSlug = "oficina-" + other.owner().oficinaId();
        assertEquals(1, messages.getAllValues().stream().filter(body -> body.contains("Identificador da oficina: " + firstSlug)
            && !body.contains(secondSlug)).count());
        assertEquals(1, messages.getAllValues().stream().filter(body -> body.contains("Identificador da oficina: " + secondSlug)
            && !body.contains(firstSlug)).count());
        for (String body : messages.getAllValues()) {
            assertTrue(body.contains("Oficina: Oficina Teste"));
            assertTrue(body.contains("http://localhost:4200/acompanhar"));
            assertTrue(body.contains("placa do veículo"));
            assertFalse(body.contains("token="));
        }
    }
    @Test void concurrentWorkersDoNotDeliverSameMessage() throws Exception {
        open();
        CountDownLatch sending = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        doAnswer(call -> { sending.countDown(); assertTrue(finish.await(10, TimeUnit.SECONDS)); return null; })
            .when(email).send(anyString(), anyString(), anyString());
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = executor.submit(worker::processOne);
            assertTrue(sending.await(10, TimeUnit.SECONDS));
            try { assertFalse(executor.submit(worker::processOne).get(5, TimeUnit.SECONDS)); }
            finally { finish.countDown(); }
            assertTrue(first.get(10, TimeUnit.SECONDS));
        }
        verify(email, times(1)).send(anyString(), anyString(), anyString());
        assertEquals(1, count("notificacao_email_tentativa"));
    }
    @Test void workerCannotSeeUncommittedOrderAndRollbackLeavesNoMail() throws Exception {
        CountDownLatch queued = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            Future<?> pending = executor.submit(() -> transaction().executeWithoutResult(status -> {
                open(); queued.countDown();
                try { assertTrue(finish.await(10, TimeUnit.SECONDS)); }
                catch (InterruptedException failure) { throw new IllegalStateException(failure); }
                status.setRollbackOnly();
            }));
            assertTrue(queued.await(10, TimeUnit.SECONDS));
            try { assertFalse(worker.processOne()); } finally { finish.countDown(); }
            pending.get(10, TimeUnit.SECONDS);
        }
        assertEquals(0, count("notificacao_email"));
        verifyNoInteractions(email);
    }
    @Test void tenantIsolationReadingPaginationAndRetryAreEnforced() {
        open();
        UUID notice = notices().getFirst().id();
        UUID otherShop = fixture(false).owner().oficinaId();
        assertEquals(0, notifications.list(otherShop, 0, 20, false).totalElements());
        assertEquals(404, assertThrows(ApiException.class, () -> notifications.markRead(otherShop, notice)).status);
        assertEquals(404, assertThrows(ApiException.class, () -> notifications.retry(otherShop, notice)).status);
        notifications.markRead(fixture.owner().oficinaId(), notice);
        notifications.markRead(fixture.owner().oficinaId(), notice);
        assertEquals(0, notifications.list(fixture.owner().oficinaId(), 0, 20, true).totalElements());
        assertTrue(notices().getFirst().lida());
        assertThrows(ApiException.class, () -> notifications.list(otherShop, -1, 20, false));
    }
    @Test void httpEndpointsRequireOwnerCsrfAndDoNotExposeRecipient() throws Exception {
        open();
        UUID id = notices().getFirst().id();
        var client = java.net.http.HttpClient.newBuilder().cookieHandler(new java.net.CookieManager(null,
            java.net.CookiePolicy.ACCEPT_ALL)).build();
        assertEquals(401, http(client, "GET", "/api/notificacoes", null, null).statusCode());
        jdbc.update("UPDATE proprietario SET senha_hash=? WHERE id=?", passwords.encode("Notification-test-123"), fixture.owner().id());
        var mapper = new tools.jackson.databind.ObjectMapper();
        String csrf = mapper.readTree(http(client, "GET", "/api/auth/csrf", null, null).body()).get("token").asText();
        assertEquals(200, http(client, "POST", "/api/auth/login", "{\"email\":\"" + fixture.owner().id()
            + "@example.test\",\"senha\":\"Notification-test-123\"}", csrf).statusCode());
        csrf = mapper.readTree(http(client, "GET", "/api/auth/csrf", null, null).body()).get("token").asText();
        var response = http(client, "GET", "/api/notificacoes", null, null);
        assertEquals(200, response.statusCode());
        assertFalse(response.body().contains("client@example.test"));
        assertEquals(403, http(client, "PATCH", "/api/notificacoes/" + id, "{\"lida\":true}", null).statusCode());
        assertEquals(400, http(client, "PATCH", "/api/notificacoes/" + id, "{\"lida\":false}", csrf).statusCode());
        assertEquals(204, http(client, "PATCH", "/api/notificacoes/" + id, "{\"lida\":true}", csrf).statusCode());
        assertEquals(404, http(client, "POST", "/api/notificacoes/" + UUID.randomUUID() + "/reenvio", "{}", csrf).statusCode());
        assertEquals(409, http(client, "POST", "/api/notificacoes/" + id + "/reenvio", "{}", csrf).statusCode());
    }
    private java.net.http.HttpResponse<String> http(java.net.http.HttpClient client, String method, String path,
                                                   String body, String csrf) throws Exception {
        var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + port + path));
        if (csrf != null) request.header("X-CSRF-TOKEN", csrf);
        request.header("Content-Type", "application/json").method(method, body == null
            ? java.net.http.HttpRequest.BodyPublishers.noBody() : java.net.http.HttpRequest.BodyPublishers.ofString(body));
        return client.send(request.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
    }
    @Test void forecastQueuesNoticeButMinorTimelineUpdateDoesNot() {
        var order = open();
        var updated = orders.updateForecast(fixture.owner(), order.id(), new ServiceOrderService.ForecastInput(
            Instant.now().plusSeconds(86400), "Novo prazo", "Aguardar peça", order.versao()));
        orders.publish(fixture.owner(), order.id(), new ServiceOrderService.UpdateInput(
            "Pequena atualização", "SEGREDO INTERNO", true, updated.versao()));
        assertEquals(2, count("notificacao"));
        assertTrue(notices().stream().anyMatch(notice -> notice.evento().equals("PREVISAO_ALTERADA")));
        assertTrue(worker.processOne());
        assertTrue(worker.processOne());
        verify(email, times(2)).send(eq("client@example.test"), anyString(), argThat(text ->
            !text.contains("SEGREDO") && !text.contains("52998224725") && !text.contains("BRA1E23")));
    }
    @Test void sentAdditionalAndIdempotentClientDecisionEachQueueOneEvent() {
        var order = open();
        var draft = requests.create(fixture.owner(), order.id(), new AdditionalRequestService.DraftInput(
            "Freios desgastados", "Segurança na frenagem", null, "Sem alteração", List.of(),
            List.of(new AdditionalRequestService.ItemInput(AdditionalRequest.ItemType.PECA,
                "Disco", BigDecimal.ONE, BigDecimal.TEN, null)), null));
        var sent = requests.send(fixture.owner(), order.id(), draft.id(),
            new AdditionalRequestService.VersionInput(draft.versao()));
        AtomicReference<String> code = new AtomicReference<>();
        doAnswer(call -> { var matcher = java.util.regex.Pattern.compile("[0-9]{6}").matcher(call.<String>getArgument(2));
            assertTrue(matcher.find()); code.set(matcher.group()); return null; })
            .when(email).send(anyString(), anyString(), anyString());
        var challenge = decisions.issueCode(fixture.owner().oficinaId(), fixture.customer(), order.id(), sent.id());
        var publicRequest = decisions.list(fixture.owner().oficinaId(), fixture.customer(), order.id()).getFirst();
        String block = publicRequest.versoes().getFirst().blocos().getFirst().id();
        var confirmation = new AdditionalDecisionService.Confirmation(challenge.desafioId(), code.get(), sent.versao(),
            List.of(new AdditionalDecisionService.BlockDecision(block, AdditionalDecisionService.Decision.APROVADO)), "Ciente");
        decisions.confirm(fixture.owner().oficinaId(), fixture.customer(), order.id(), sent.id(), "decision-1", confirmation);
        decisions.confirm(fixture.owner().oficinaId(), fixture.customer(), order.id(), sent.id(), "decision-1", confirmation);
        assertEquals(3, count("notificacao"));
        assertEquals(3, count("notificacao_email"));
        assertEquals(1, notices().stream().filter(notice -> notice.evento().equals("DECISAO_REGISTRADA")).count());
    }
    private TransactionTemplate transaction() { return new TransactionTemplate(transactions); }
    private List<NotificationService.Notice> notices() { return notifications.list(fixture.owner().oficinaId(), 0, 20, false).items(); }
    private ServiceOrder open() { return orders.create(fixture.owner(), null, input(fixture)); }
    private int count(String table) { return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class); }
    private ServiceOrderService.CreateInput input(Fixture data) {
        return new ServiceOrderService.CreateInput(data.customer(), data.vehicle(), "Ruído dianteiro ao frear",
            Instant.now().minusSeconds(60), 1000, null);
    }
    private Fixture fixture(boolean verified) {
        UUID shop = UUID.randomUUID(), owner = UUID.randomUUID(), customer = UUID.randomUUID(), vehicle = UUID.randomUUID();
        jdbc.update("INSERT INTO oficina(id,nome,slug) VALUES (?,?,?)", shop, "Oficina Teste", "oficina-" + shop);
        jdbc.update("INSERT INTO proprietario(id,oficina_id,nome,email,senha_hash) VALUES (?,?,?,?,?)",
            owner, shop, "Dono", owner + "@example.test", "hash");
        jdbc.update("INSERT INTO cliente(id,oficina_id,nome,cpf,email,email_verificado_em) VALUES (?,?,?,?,?,?)",
            customer, shop, "Cliente Teste", "52998224725", "client@example.test", verified ? Timestamp.from(Instant.now()) : null);
        jdbc.update("INSERT INTO veiculo(id,oficina_id,placa,marca,modelo) VALUES (?,?,?,?,?)", vehicle, shop, "BRA1E23", "Marca", "Modelo");
        jdbc.update("INSERT INTO vinculo_cliente_veiculo(id,oficina_id,cliente_id,veiculo_id,criado_por) VALUES (?,?,?,?,?)",
            UUID.randomUUID(), shop, customer, vehicle, owner);
        return new Fixture(new Identidade(owner, shop, 0, Instant.now()), customer, vehicle);
    }
    private record Fixture(Identidade owner, UUID customer, UUID vehicle) {}
}
