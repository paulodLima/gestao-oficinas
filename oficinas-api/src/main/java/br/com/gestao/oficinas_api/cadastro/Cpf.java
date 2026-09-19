package br.com.gestao.oficinas_api.cadastro;

import br.com.gestao.oficinas_api.identidade.ApiException;

public final class Cpf {
    private Cpf() {}

    public static String normalize(String value) {
        if (value == null) throw invalid();
        String digits = value.replaceAll("\\D", "");
        if (digits.length() != 11 || digits.chars().distinct().count() == 1
            || digit(digits, 9) != digits.charAt(9) - '0'
            || digit(digits, 10) != digits.charAt(10) - '0') throw invalid();
        return digits;
    }

    private static int digit(String cpf, int position) {
        int sum = 0;
        int weight = position + 1;
        for (int index = 0; index < position; index++) sum += (cpf.charAt(index) - '0') * (weight - index);
        int remainder = (sum * 10) % 11;
        return remainder == 10 ? 0 : remainder;
    }

    private static ApiException invalid() {
        return new ApiException(400, "CPF_INVALIDO", "Informe um CPF válido.");
    }
}
