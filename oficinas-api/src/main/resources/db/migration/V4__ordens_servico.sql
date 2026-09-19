CREATE TABLE ordem_servico_numero (
 oficina_id uuid PRIMARY KEY REFERENCES oficina(id),
 ultimo_numero bigint NOT NULL,
 CONSTRAINT ordem_numero_positivo CHECK (ultimo_numero >= 0)
);

CREATE TABLE ordem_servico (
 id uuid PRIMARY KEY,
 oficina_id uuid NOT NULL,
 numero bigint NOT NULL,
 cliente_id uuid NOT NULL,
 veiculo_id uuid NOT NULL,
 relato_inicial varchar(2000) NOT NULL,
 entrada_em timestamptz NOT NULL,
 km_entrada integer NOT NULL,
 status varchar(40) NOT NULL DEFAULT 'RECEBIDO',
 previsao_em timestamptz,
 encerrada_em timestamptz,
 versao bigint NOT NULL DEFAULT 0,
 criado_por uuid NOT NULL REFERENCES proprietario(id),
 created_at timestamptz NOT NULL DEFAULT now(),
 updated_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT ordem_servico_oficina_id_unique UNIQUE (oficina_id, id),
 CONSTRAINT ordem_servico_numero_unique UNIQUE (oficina_id, numero),
 CONSTRAINT ordem_servico_cliente_fk FOREIGN KEY (oficina_id, cliente_id) REFERENCES cliente(oficina_id, id),
 CONSTRAINT ordem_servico_veiculo_fk FOREIGN KEY (oficina_id, veiculo_id) REFERENCES veiculo(oficina_id, id),
 CONSTRAINT ordem_servico_relato CHECK (length(btrim(relato_inicial)) BETWEEN 10 AND 2000),
 CONSTRAINT ordem_servico_km CHECK (km_entrada BETWEEN 0 AND 9999999),
 CONSTRAINT ordem_servico_previsao CHECK (previsao_em IS NULL OR previsao_em >= entrada_em),
 CONSTRAINT ordem_servico_status CHECK (status IN ('RECEBIDO','EM_DIAGNOSTICO','AGUARDANDO_APROVACAO',
  'AGUARDANDO_PECAS','EM_MANUTENCAO','EM_MONTAGEM','EM_TESTES','PRONTO_PARA_RETIRADA',
  'ENTREGUE','CANCELADO','FUNILARIA','PINTURA')),
 CONSTRAINT ordem_servico_encerramento CHECK (
  (encerrada_em IS NOT NULL AND status IN ('ENTREGUE','CANCELADO')) OR
  (encerrada_em IS NULL AND status NOT IN ('ENTREGUE','CANCELADO'))
 )
);

CREATE UNIQUE INDEX ordem_servico_veiculo_ativa_unique
 ON ordem_servico (oficina_id, veiculo_id) WHERE encerrada_em IS NULL;
CREATE INDEX ordem_servico_busca_idx
 ON ordem_servico (oficina_id, entrada_em DESC, id);
CREATE INDEX ordem_servico_cliente_idx
 ON ordem_servico (oficina_id, cliente_id, entrada_em DESC);
