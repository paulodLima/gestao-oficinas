package br.com.gestao.oficinas_api.cadastro;

import java.time.Instant;
import java.util.UUID;

public record Customer(UUID id, String nome, String cpf, String telefone, String email,
                       Instant emailVerificadoEm, boolean ativo, long versao) {
    public boolean emailVerificado() { return emailVerificadoEm != null; }
}
