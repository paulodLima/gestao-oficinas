package br.com.gestao.oficinas_api.ordem;

import java.time.Instant;
import java.util.UUID;

public record ServiceOrder(UUID id, long numero, UUID clienteId, String clienteNome,
                           UUID veiculoId, String placa, String veiculo,
                           String relatoInicial, Instant entradaEm, int kmEntrada,
                           ServiceOrderStatus status, Instant previsaoEm,
                           long versao, Instant createdAt) {}
