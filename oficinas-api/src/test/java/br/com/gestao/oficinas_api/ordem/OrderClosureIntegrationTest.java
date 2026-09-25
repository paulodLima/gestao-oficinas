package br.com.gestao.oficinas_api.ordem;

import br.com.gestao.oficinas_api.adicional.*;
import br.com.gestao.oficinas_api.identidade.*;
import br.com.gestao.oficinas_api.notificacoes.TransactionalEmail;
import br.com.gestao.oficinas_api.support.TestPostgres;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
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

@SpringBootTest(properties = {"app.notifications.enabled=false", "spring.session.jdbc.cleanup-cron=-"})
@DirtiesContext
@org.testcontainers.junit.jupiter.Testcontainers
class OrderClosureIntegrationTest {
    @org.testcontainers.junit.jupiter.Container static final TestPostgres database = new TestPostgres();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", database::getJdbcUrl);
        registry.add("spring.datasource.username", database::getUsername);
        registry.add("spring.datasource.password", database::getPassword);
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired ServiceOrderService orders;
    @Autowired ServiceOrderRepository repository;
    @Autowired OrderClosureService closures;
    @Autowired AdditionalRequestService requests;
    @Autowired AdditionalDecisionService decisions;
    @Autowired InspectionService inspections;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean TransactionalEmail email;
    Identidade owner;
    UUID customer, vehicle;
    ServiceOrder order;

    @BeforeEach void setup() {
        UUID shop = UUID.randomUUID(), id = UUID.randomUUID();
        customer = UUID.randomUUID(); vehicle = UUID.randomUUID();
        owner = new Identidade(id, shop, 0, Instant.now());
        jdbc.update("INSERT INTO oficina(id,nome,slug) VALUES (?,?,?)", shop, "Oficina Teste", "teste-" + shop);
        jdbc.update("INSERT INTO proprietario(id,oficina_id,nome,email,senha_hash) VALUES (?,?,?,?,?)", id, shop, "Dono", id + "@example.test", "hash");
        jdbc.update("INSERT INTO cliente(id,oficina_id,nome,cpf,email,email_verificado_em) VALUES (?,?,?,?,?,now())", customer, shop, "Cliente", "52998224725", "cliente@example.test");
        jdbc.update("INSERT INTO veiculo(id,oficina_id,placa,marca,modelo) VALUES (?,?,?,?,?)", vehicle, shop, "BRA1E23", "Marca", "Modelo");
        jdbc.update("INSERT INTO vinculo_cliente_veiculo(id,oficina_id,cliente_id,veiculo_id,criado_por) VALUES (?,?,?,?,?)", UUID.randomUUID(), shop, customer, vehicle, id);
        order = orders.create(owner, null, input());
    }

    @Test void readyStaysActiveAndClosureCreatesImmutableHistoryAndOneNotification() {
        order = orders.changeStatus(owner, order.id(), new ServiceOrderService.StatusInput(
            ServiceOrderStatus.PRONTO_PARA_RETIRADA, null, null, null, order.versao()));
        assertTrue(order.aguardandoRetirada());
        assertThrows(ApiException.class, () -> orders.create(owner, null, input()));
        assertEquals(1, notices("PRONTO_PARA_RETIRADA"));
        var closed = closures.close(owner, order.id(), closeInput(false));
        assertEquals(ServiceOrderStatus.ENTREGUE, closed.status());
        assertFalse(closed.aguardandoRetirada());
        assertFalse(closed.atrasada());
        assertEquals(1, notices("ORDEM_ENCERRADA"));
        assertNotNull(closures.summary(owner, order.id()).encerramento());
        assertEquals("Dono", closures.summary(owner, order.id()).encerramento().autorNome());
        assertThrows(ApiException.class, () -> closures.close(owner, order.id(), closeInput(false)));
        assertEquals(1, notices("ORDEM_ENCERRADA"));
        assertEquals(3, orders.timeline(owner, order.id()).size());
        assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update(
            "UPDATE ordem_servico_encerramento SET motivo='alterado' WHERE ordem_servico_id=?", order.id()));
        var returned = orders.create(owner, null, input());
        assertNotEquals(order.id(), returned.id());
        assertEquals(order.numero() + 1, returned.numero());
        verifyNoInteractions(email);
    }

    @Test void validatesVersionAndConsentWithoutPartialChangesAndFreezesAllWrites() {
        var pending = requests.create(owner, order.id(), draft());
        assertEquals(1, closures.summary(owner, order.id()).pendencias());
        assertThrows(ApiException.class, () -> closures.close(owner, order.id(), closeInput(false)));
        assertThrows(ApiException.class, () -> closures.close(owner, order.id(), new OrderClosurePolicy.Input(
            ServiceOrderStatus.ENTREGUE, null, true, true, 99L)));
        assertTrue(orders.order(owner, order.id()).status().active());
        assertEquals(AdditionalRequest.Status.RASCUNHO, requests.list(owner, order.id()).getFirst().estado());
        var closed = closures.close(owner, order.id(), new OrderClosurePolicy.Input(
            ServiceOrderStatus.CANCELADO, "Cliente desistiu", true, true, order.versao()));
        assertEquals(ServiceOrderStatus.CANCELADO, closed.status());
        assertEquals(1, closures.summary(owner, order.id()).encerramento().pendenciasCanceladas());
        assertEquals(AdditionalRequest.Status.CANCELADA, requests.list(owner, order.id()).getFirst().estado());
        assertThrows(ApiException.class, () -> requests.create(owner, order.id(), draft()));
        assertThrows(ApiException.class, () -> requests.send(owner, order.id(), pending.id(), new AdditionalRequestService.VersionInput(0L)));
        assertThrows(ApiException.class, () -> orders.publish(owner, order.id(), new ServiceOrderService.UpdateInput("Pós-fechamento", null, true, closed.versao())));
        assertThrows(ApiException.class, () -> orders.changeStatus(owner, order.id(), new ServiceOrderService.StatusInput(ServiceOrderStatus.RECEBIDO, "Reabrir", null, null, closed.versao())));
        assertThrows(ApiException.class, () -> orders.updateForecast(owner, order.id(), new ServiceOrderService.ForecastInput(Instant.now().plusSeconds(3600), "Teste", "Teste", closed.versao())));
        assertThrows(ApiException.class, () -> inspections.draft(owner, order.id(), null));
        assertThrows(ApiException.class, () -> decisions.issueCode(owner.oficinaId(), customer, order.id(), pending.id()));
        assertEquals(2, orders.timeline(owner, order.id()).size());
    }

    @Test void rollbackRestoresOrderPendingLinksChallengesAndNotification() {
        var decision = decision();
        UUID link = UUID.randomUUID();
        jdbc.update("INSERT INTO portal_link_os(id,oficina_id,ordem_servico_id,token_hash,expira_em,criado_por) VALUES (?,?,?, ?,now()+interval '1 day',?)",
            link, owner.oficinaId(), order.id(), "0".repeat(64), owner.id());
        assertThrows(IllegalStateException.class, () -> tx().executeWithoutResult(status -> {
            closures.close(owner, order.id(), closeInput(true));
            throw new IllegalStateException("Simulação de falha da transação");
        }));
        assertTrue(orders.order(owner, order.id()).status().active());
        assertEquals(1, closures.summary(owner, order.id()).pendencias());
        assertEquals(0, notices("ORDEM_ENCERRADA"));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM portal_link_os WHERE id=? AND revogado_em IS NULL", Integer.class, link));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM adicional_desafio WHERE id=? AND usado_em IS NULL", Integer.class, decision.input().desafioId()));
        closures.close(owner, order.id(), closeInput(true));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM portal_link_os WHERE id=? AND revogado_em IS NULL", Integer.class, link));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM adicional_desafio WHERE id=? AND usado_em IS NULL", Integer.class, decision.input().desafioId()));
    }

    @Test void closureWinningLockRejectsApprovalAlreadyInFlight() throws Exception {
        var decision = decision();
        try (var executor = Executors.newSingleThreadExecutor()) {
            var started = new CountDownLatch(1);
            AtomicReference<Future<?>> attempt = new AtomicReference<>();
            tx().executeWithoutResult(status -> {
                repository.lockActive(owner.oficinaId(), order.id());
                attempt.set(executor.submit(() -> { started.countDown(); approve(decision); }));
                awaitBlocked(started, attempt.get());
                closures.close(owner, order.id(), closeInput(true));
            });
            ExecutionException error = assertThrows(ExecutionException.class, () -> attempt.get().get(10, TimeUnit.SECONDS));
            assertInstanceOf(ApiException.class, error.getCause());
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM adicional_decisao_operacao WHERE ordem_servico_id=?", Integer.class, order.id()));
        }
    }

    @Test void approvalWinningLockIsPreservedWhenRemainingItemsAreCanceled() throws Exception {
        var decision = decision();
        try (var executor = Executors.newSingleThreadExecutor()) {
            var started = new CountDownLatch(1);
            AtomicReference<Future<ServiceOrder>> close = new AtomicReference<>();
            tx().executeWithoutResult(status -> {
                approve(decision);
                close.set(executor.submit(() -> { started.countDown(); return closures.close(owner, order.id(), closeInput(true)); }));
                awaitBlocked(started, close.get());
            });
            assertEquals(ServiceOrderStatus.ENTREGUE, close.get().get(10, TimeUnit.SECONDS).status());
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM adicional_item_decisao WHERE solicitacao_id=? AND decisao='APROVADO'", Integer.class, decision.request()));
            assertEquals(AdditionalRequest.Status.CANCELADA, requests.list(owner, order.id()).getFirst().estado());
            assertEquals(1, closures.summary(owner, order.id()).encerramento().pendenciasCanceladas());
        }
    }

    private void awaitBlocked(CountDownLatch started, Future<?> future) {
        try { assertTrue(started.await(5, TimeUnit.SECONDS)); assertThrows(TimeoutException.class, () -> future.get(150, TimeUnit.MILLISECONDS)); }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new AssertionError(error); }
    }
    private DecisionFixture decision() {
        var request = requests.create(owner, order.id(), draft());
        request = requests.send(owner, order.id(), request.id(), new AdditionalRequestService.VersionInput(request.versao()));
        AtomicReference<String> code = new AtomicReference<>();
        doAnswer(call -> { var match = java.util.regex.Pattern.compile("[0-9]{6}").matcher(call.<String>getArgument(2));
            assertTrue(match.find()); code.set(match.group()); return null; }).when(email).send(anyString(), anyString(), anyString());
        var challenge = decisions.issueCode(owner.oficinaId(), customer, order.id(), request.id());
        String block = decisions.list(owner.oficinaId(), customer, order.id()).getFirst().versoes().getFirst().blocos().getFirst().id();
        return new DecisionFixture(request.id(), new AdditionalDecisionService.Confirmation(challenge.desafioId(), code.get(), request.versao(),
            List.of(new AdditionalDecisionService.BlockDecision(block, AdditionalDecisionService.Decision.APROVADO)), "Ciente"));
    }
    private void approve(DecisionFixture decision) { decisions.confirm(owner.oficinaId(), customer, order.id(), decision.request(), "decision", decision.input()); }
    private TransactionTemplate tx() { return new TransactionTemplate(transactions); }
    private int notices(String event) { return jdbc.queryForObject("SELECT count(*) FROM notificacao WHERE oficina_id=? AND evento=?", Integer.class, owner.oficinaId(), event); }
    private OrderClosurePolicy.Input closeInput(boolean pending) { return new OrderClosurePolicy.Input(ServiceOrderStatus.ENTREGUE, null, true, pending, order.versao()); }
    private ServiceOrderService.CreateInput input() { return new ServiceOrderService.CreateInput(customer, vehicle, "Ruído dianteiro ao frear", Instant.now().minusSeconds(60), 1000, null); }
    private AdditionalRequestService.DraftInput draft() {
        return new AdditionalRequestService.DraftInput("Freios desgastados", "Segurança", null, "Sem alteração", List.of(), List.of(
            new AdditionalRequestService.ItemInput(AdditionalRequest.ItemType.PECA, "Disco", BigDecimal.ONE, BigDecimal.TEN, null),
            new AdditionalRequestService.ItemInput(AdditionalRequest.ItemType.PECA, "Filtro", BigDecimal.ONE, BigDecimal.TEN, null)), null);
    }
    private record DecisionFixture(UUID request, AdditionalDecisionService.Confirmation input) {}
}
