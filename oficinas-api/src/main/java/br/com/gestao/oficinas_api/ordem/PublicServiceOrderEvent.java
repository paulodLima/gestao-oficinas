package br.com.gestao.oficinas_api.ordem;

import java.time.Instant;
import java.util.UUID;

public record PublicServiceOrderEvent(UUID id, String tipo,
                                      ServiceOrderStatus statusAnterior, ServiceOrderStatus statusNovo,
                                      String textoPublico, String autorNome, Instant createdAt) {}
