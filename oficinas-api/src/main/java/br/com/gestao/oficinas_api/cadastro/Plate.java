package br.com.gestao.oficinas_api.cadastro;

import br.com.gestao.oficinas_api.identidade.ApiException;
import java.util.Locale;

public final class Plate {
    private static final String OLD = "[A-Z]{3}[0-9]{4}";
    private static final String MERCOSUL = "[A-Z]{3}[0-9][A-Z][0-9]{2}";
    private Plate() {}

    public static String normalize(String value) {
        if (value == null) throw invalid();
        String normalized = value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (!normalized.matches(OLD) && !normalized.matches(MERCOSUL)) throw invalid();
        return normalized;
    }

    private static ApiException invalid() {
        return new ApiException(400, "PLACA_INVALIDA", "Informe uma placa antiga ou Mercosul válida.");
    }
}
