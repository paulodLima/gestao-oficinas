CREATE TABLE adicional_desafio (
 id uuid PRIMARY KEY,
 oficina_id uuid NOT NULL REFERENCES oficina(id),
 cliente_id uuid NOT NULL,
 ordem_servico_id uuid NOT NULL,
 solicitacao_id uuid NOT NULL REFERENCES solicitacao_adicional(id),
 versao_id uuid NOT NULL REFERENCES adicional_versao(id),
 versao_solicitacao bigint NOT NULL,
 codigo_hash char(64) NOT NULL,
 expira_em timestamptz NOT NULL,
 tentativas integer NOT NULL DEFAULT 0,
 usado_em timestamptz,
 created_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT adicional_desafio_cliente_fk FOREIGN KEY(oficina_id,cliente_id)
   REFERENCES cliente(oficina_id,id),
 CONSTRAINT adicional_desafio_os_fk FOREIGN KEY(oficina_id,ordem_servico_id)
   REFERENCES ordem_servico(oficina_id,id),
 CONSTRAINT adicional_desafio_tentativas CHECK(tentativas BETWEEN 0 AND 5)
);
CREATE INDEX adicional_desafio_consulta_idx
 ON adicional_desafio(oficina_id,cliente_id,solicitacao_id,created_at DESC);

CREATE TABLE adicional_decisao_operacao (
 id uuid PRIMARY KEY,
 oficina_id uuid NOT NULL REFERENCES oficina(id),
 cliente_id uuid NOT NULL,
 ordem_servico_id uuid NOT NULL,
 solicitacao_id uuid NOT NULL REFERENCES solicitacao_adicional(id),
 versao_id uuid NOT NULL REFERENCES adicional_versao(id),
 idempotency_key varchar(100) NOT NULL,
 payload_hash char(64) NOT NULL,
 comentario varchar(1000),
 created_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT adicional_operacao_cliente_fk FOREIGN KEY(oficina_id,cliente_id)
   REFERENCES cliente(oficina_id,id),
 CONSTRAINT adicional_operacao_unique UNIQUE(solicitacao_id,idempotency_key)
);

CREATE TABLE adicional_item_decisao (
 id uuid PRIMARY KEY,
 operacao_id uuid NOT NULL REFERENCES adicional_decisao_operacao(id),
 solicitacao_id uuid NOT NULL REFERENCES solicitacao_adicional(id),
 versao_id uuid NOT NULL REFERENCES adicional_versao(id),
 item_id uuid NOT NULL REFERENCES adicional_item(id),
 cliente_id uuid NOT NULL REFERENCES cliente(id),
 decisao varchar(10) NOT NULL,
 bloco varchar(80) NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT adicional_item_decisao_valida CHECK(decisao IN ('APROVADO','RECUSADO')),
 CONSTRAINT adicional_item_decisao_unique UNIQUE(versao_id,item_id)
);
CREATE INDEX adicional_item_decisao_solicitacao_idx
 ON adicional_item_decisao(solicitacao_id,created_at);
