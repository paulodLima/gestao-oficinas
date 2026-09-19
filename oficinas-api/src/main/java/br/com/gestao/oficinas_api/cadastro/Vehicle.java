package br.com.gestao.oficinas_api.cadastro;

import java.time.Instant;
import java.util.UUID;

public record Vehicle(UUID id, String placa, String marca, String modelo, Integer ano, String cor,
                      UUID clienteId, String clienteNome, Instant vinculoDesde, long versao) {}
