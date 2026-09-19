package br.com.gestao.oficinas_api.ordem;

import java.time.Instant;
import java.util.UUID;

public record ServiceOrderForecast(UUID id, Instant previsaoAnterior, Instant previsaoNova,
                                   String motivoPublico, String proximaAcao,
                                   UUID autorId, String autorNome, Instant createdAt) {}
