package br.com.gestao.oficinas_api.ordem;

import java.time.Instant;
import java.util.UUID;

public record ServicePhoto(UUID id, ServiceOrderStatus etapa, String legenda, boolean publicada,
                           String tipoConteudo, long tamanhoBytes, boolean miniaturaDisponivel,
                           Instant createdAt) {}
