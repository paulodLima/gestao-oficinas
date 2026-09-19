package br.com.gestao.oficinas_api.identidade;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record Identidade(UUID id, UUID oficinaId, long authVersion, Instant autenticadoEm) implements Serializable {}

