package br.com.gestao.oficinas_api.ordem;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record Inspection(UUID id, int numeroVersao, String estado, Map<String,Object> checklist,
                         String motivoCorrecao, Instant createdAt, Instant updatedAt) {}
