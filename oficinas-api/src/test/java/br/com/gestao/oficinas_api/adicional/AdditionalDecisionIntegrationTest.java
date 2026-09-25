package br.com.gestao.oficinas_api.adicional;

import br.com.gestao.oficinas_api.notificacoes.TransactionalEmail;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import br.com.gestao.oficinas_api.support.TestPostgres;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@Testcontainers
class AdditionalDecisionIntegrationTest {
    @Container static final TestPostgres postgres = new TestPostgres();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired AdditionalDecisionService service;
    @MockitoBean TransactionalEmail email;

    @Test void storesAnIndivisibleGroupAndReplaysTheSameIdempotentOperation() {
        doNothing().when(email).send(anyString(), anyString(), anyString());
        UUID owner = UUID.randomUUID(), shop = UUID.randomUUID(), customer = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID(), order = UUID.randomUUID(), request = UUID.randomUUID();
        UUID version = UUID.randomUUID(), left = UUID.randomUUID(), right = UUID.randomUUID();
        jdbc.update("INSERT INTO oficina(id,nome,slug) VALUES (?,?,?)",
            shop, "Oficina", "oficina-" + shop);
        jdbc.update("INSERT INTO proprietario(id,oficina_id,nome,email,senha_hash) VALUES (?,?,?,?,?)",
            owner, shop, "Dono", owner + "@test.local", "hash");
        jdbc.update("INSERT INTO cliente(id,oficina_id,nome,cpf,email,email_verificado_em) VALUES (?,?,?,?,?,now())",
            customer, shop, "Ana", "52998224725", "ana@test.local");
        jdbc.update("INSERT INTO veiculo(id,oficina_id,placa,marca,modelo) VALUES (?,?,?,?,?)",
            vehicle, shop, "BRA1E23", "Volkswagen", "T-Cross");
        jdbc.update("INSERT INTO ordem_servico(id,oficina_id,numero,cliente_id,veiculo_id,relato_inicial,entrada_em,km_entrada,status,criado_por) VALUES (?,?,?,?,?,?,?,?,?,?)",
            order, shop, 1, customer, vehicle, "Ruído dianteiro", java.sql.Timestamp.from(Instant.now()), 1000, "AGUARDANDO_APROVACAO", owner);
        jdbc.update("INSERT INTO solicitacao_adicional(id,oficina_id,ordem_servico_id,estado,criado_por) VALUES (?,?,?,'ENVIADA',?)",
            request, shop, order, owner);
        jdbc.update("INSERT INTO adicional_versao(id,solicitacao_id,oficina_id,ordem_servico_id,numero,estado,problema,justificativa,impacto_prazo,enviada_em) VALUES (?,?,?,?,1,'ENVIADA',?,?,?,now())",
            version, request, shop, order, "Freios", "Segurança", "Mais um dia");
        jdbc.update("INSERT INTO adicional_item(id,versao_id,tipo,descricao,quantidade,valor_unitario,total,grupo_dependencia,ordem) VALUES (?,?, 'PECA',?,1,100,100,'freios',0)",
            left, version, "Disco esquerdo");
        jdbc.update("INSERT INTO adicional_item(id,versao_id,tipo,descricao,quantidade,valor_unitario,total,grupo_dependencia,ordem) VALUES (?,?, 'PECA',?,1,100,100,'freios',1)",
            right, version, "Disco direito");

        // Insere um desafio conhecido para testar a confirmação sem depender da entrega de e-mail.
        UUID challenge = UUID.randomUUID();
        String secret = "development-only-secret-change-me";
        String hash = hmac(secret, challenge, "123456");
        jdbc.update("INSERT INTO adicional_desafio(id,oficina_id,cliente_id,ordem_servico_id,solicitacao_id,versao_id,versao_solicitacao,codigo_hash,expira_em) VALUES (?,?,?,?,?,?,?,?,?)",
            challenge, shop, customer, order, request, version, 0, hash, java.sql.Timestamp.from(Instant.now().plusSeconds(600)));
        var input = new AdditionalDecisionService.Confirmation(challenge, "123456", 0,
            List.of(new AdditionalDecisionService.BlockDecision("grupo:freios",
                AdditionalDecisionService.Decision.APROVADO)), "Autorizado");
        service.confirm(shop, customer, order, request, "attempt-1", input);
        service.confirm(shop, customer, order, request, "attempt-1", input);

        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM adicional_item_decisao WHERE solicitacao_id=?", Integer.class, request));
        assertEquals("DECIDIDA", jdbc.queryForObject("SELECT estado FROM solicitacao_adicional WHERE id=?", String.class, request));
        assertEquals(new BigDecimal("200.00"), jdbc.queryForObject("SELECT sum(i.total) FROM adicional_item i JOIN adicional_item_decisao d ON d.item_id=i.id WHERE d.decisao='APROVADO' AND d.solicitacao_id=?", BigDecimal.class, request));
    }

    private String hmac(String secret, UUID id, String code) {
        try {
            var mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(mac.doFinal((id + ":" + code).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception exception) { throw new IllegalStateException(exception); }
    }
}
