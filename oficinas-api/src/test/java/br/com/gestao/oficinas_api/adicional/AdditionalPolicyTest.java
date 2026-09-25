package br.com.gestao.oficinas_api.adicional;

import br.com.gestao.oficinas_api.identidade.ApiException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdditionalPolicyTest {
    private final AdditionalPolicy policy = new AdditionalPolicy();

    @Test void calculatesItemAndVersionTotalsWithFinancialRounding() {
        assertEquals(new BigDecimal("22.49"), policy.itemTotal(
            new BigDecimal("1.125"), new BigDecimal("19.99")));
        var first = item("22.49");
        var second = item("10.01");
        assertEquals(new BigDecimal("32.50"), policy.versionTotal(List.of(first, second)));
    }

    @Test void enforcesQuantityAndUnitValuePrecision() {
        assertEquals(new BigDecimal("2.500"), policy.quantity(new BigDecimal("2.5")));
        assertEquals(new BigDecimal("12.30"), policy.unitValue(new BigDecimal("12.3")));
        assertThrows(ApiException.class, () -> policy.quantity(new BigDecimal("1.0001")));
        assertThrows(ApiException.class, () -> policy.unitValue(new BigDecimal("1.001")));
        assertThrows(ApiException.class, () -> policy.quantity(BigDecimal.ZERO));
        assertThrows(ApiException.class, () -> policy.unitValue(new BigDecimal("-0.01")));
    }

    @Test void acceptsOnlyCompleteDependentGroups() {
        assertDoesNotThrow(() -> policy.validateGroups(List.of("kit-freio", "kit-freio", "")));
        ApiException error = assertThrows(ApiException.class,
            () -> policy.validateGroups(List.of("kit-freio", "kit-suspensao", "kit-freio")));
        assertEquals("DADOS_INVALIDOS", error.code);
        assertTrue(error.getMessage().contains("pelo menos dois"));
    }

    private AdditionalRequest.Item item(String total) {
        return new AdditionalRequest.Item(UUID.randomUUID(), AdditionalRequest.ItemType.PECA,
            "Item", BigDecimal.ONE, new BigDecimal(total), new BigDecimal(total), null, 1);
    }
}
