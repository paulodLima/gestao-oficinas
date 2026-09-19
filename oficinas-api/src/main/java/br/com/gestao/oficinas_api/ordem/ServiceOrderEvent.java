package br.com.gestao.oficinas_api.ordem;

import java.time.Instant;
import java.util.UUID;

public record ServiceOrderEvent(UUID id, String tipo,
                                ServiceOrderStatus statusAnterior, ServiceOrderStatus statusNovo,
                                String motivo, String textoPublico, String textoInterno,
                                boolean publicada, UUID autorId, String autorNome, Instant createdAt) {}
