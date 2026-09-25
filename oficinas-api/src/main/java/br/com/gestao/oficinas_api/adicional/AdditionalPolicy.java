package br.com.gestao.oficinas_api.adicional;

import br.com.gestao.oficinas_api.identidade.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AdditionalPolicy {
    private static final BigDecimal MAX_QUANTITY = new BigDecimal("999999999.999");
    private static final BigDecimal MAX_UNIT_VALUE = new BigDecimal("9999999999999.99");

    public BigDecimal quantity(BigDecimal value) {
        if (value == null || value.signum() <= 0 || value.compareTo(MAX_QUANTITY) > 0) {
            throw invalid("Informe uma quantidade maior que zero.");
        }
        try {
            return value.setScale(3, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw invalid("A quantidade deve ter no máximo três casas decimais.");
        }
    }

    public BigDecimal unitValue(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(MAX_UNIT_VALUE) > 0) {
            throw invalid("Informe um valor unitário válido.");
        }
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw invalid("O valor unitário deve ter no máximo duas casas decimais.");
        }
    }

    public BigDecimal itemTotal(BigDecimal quantity, BigDecimal unitValue) {
        return quantity.multiply(unitValue).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal versionTotal(List<AdditionalRequest.Item> items) {
        return items.stream().map(AdditionalRequest.Item::total)
            .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    public void validateGroups(List<String> groups) {
        Map<String, Integer> counts = new HashMap<>();
        groups.stream().filter(group -> group != null && !group.isBlank())
            .forEach(group -> counts.merge(group, 1, Integer::sum));
        if (counts.values().stream().anyMatch(count -> count < 2)) {
            throw invalid("Cada grupo dependente deve conter pelo menos dois itens.");
        }
    }

    private ApiException invalid(String detail) {
        return new ApiException(400, "DADOS_INVALIDOS", detail);
    }
}
