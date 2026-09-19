package br.com.gestao.oficinas_api.identidade;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {
    private PasswordPolicy() {}
    public static boolean valid(String password) {
        return password != null && password.codePointCount(0,password.length()) >= 12
            && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
    public static void require(String password) {
        if (!valid(password)) throw new ApiException(400,"SENHA_INVALIDA",
            "Use pelo menos 12 caracteres e no máximo 72 bytes na senha.");
    }
}

