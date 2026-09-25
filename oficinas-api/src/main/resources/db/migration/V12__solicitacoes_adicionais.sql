CREATE TABLE solicitacao_adicional (
 id uuid PRIMARY KEY,
 oficina_id uuid NOT NULL,
 ordem_servico_id uuid NOT NULL,
 estado varchar(30) NOT NULL DEFAULT 'RASCUNHO',
 versao bigint NOT NULL DEFAULT 0,
 motivo_cancelamento varchar(1000),
 criado_por uuid NOT NULL REFERENCES proprietario(id),
 created_at timestamptz NOT NULL DEFAULT now(),
 updated_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT adicional_solicitacao_os_fk FOREIGN KEY (oficina_id, ordem_servico_id)
   REFERENCES ordem_servico(oficina_id, id),
 CONSTRAINT adicional_solicitacao_estado CHECK (estado IN (
   'RASCUNHO','ENVIADA','PARCIALMENTE_DECIDIDA','DECIDIDA','CANCELADA'))
);
CREATE INDEX adicional_solicitacao_os_idx
 ON solicitacao_adicional(oficina_id, ordem_servico_id, created_at DESC);

CREATE TABLE adicional_versao (
 id uuid PRIMARY KEY,
 solicitacao_id uuid NOT NULL REFERENCES solicitacao_adicional(id),
 oficina_id uuid NOT NULL,
 ordem_servico_id uuid NOT NULL,
 numero integer NOT NULL,
 estado varchar(20) NOT NULL DEFAULT 'RASCUNHO',
 problema varchar(2000) NOT NULL,
 justificativa varchar(2000) NOT NULL,
 previsao_proposta timestamptz,
 impacto_prazo varchar(1000) NOT NULL,
 motivo_substituicao varchar(1000),
 enviada_em timestamptz,
 substituida_em timestamptz,
 created_at timestamptz NOT NULL DEFAULT now(),
 updated_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT adicional_versao_solicitacao_unique UNIQUE (solicitacao_id, numero),
 CONSTRAINT adicional_versao_os_fk FOREIGN KEY (oficina_id, ordem_servico_id)
   REFERENCES ordem_servico(oficina_id, id),
 CONSTRAINT adicional_versao_estado CHECK (estado IN ('RASCUNHO','ENVIADA','SUBSTITUIDA'))
);

CREATE INDEX adicional_versao_solicitacao_idx
 ON adicional_versao(solicitacao_id, numero DESC);

CREATE TABLE adicional_item (
 id uuid PRIMARY KEY,
 versao_id uuid NOT NULL REFERENCES adicional_versao(id),
 tipo varchar(20) NOT NULL,
 descricao varchar(500) NOT NULL,
 quantidade numeric(12,3) NOT NULL,
 valor_unitario numeric(15,2) NOT NULL,
 total numeric(15,2) NOT NULL,
 grupo_dependencia varchar(60),
 ordem integer NOT NULL,
 CONSTRAINT adicional_item_tipo CHECK (tipo IN ('PECA','MAO_DE_OBRA')),
 CONSTRAINT adicional_item_quantidade CHECK (quantidade > 0),
 CONSTRAINT adicional_item_valor CHECK (valor_unitario >= 0),
 CONSTRAINT adicional_item_total CHECK (total >= 0),
 CONSTRAINT adicional_item_ordem_unique UNIQUE (versao_id, ordem)
);

CREATE TABLE adicional_versao_foto (
 versao_id uuid NOT NULL REFERENCES adicional_versao(id),
 foto_id uuid NOT NULL REFERENCES ordem_servico_foto(id),
 ordem integer NOT NULL,
 PRIMARY KEY (versao_id, foto_id),
 CONSTRAINT adicional_foto_ordem_unique UNIQUE (versao_id, ordem)
);
