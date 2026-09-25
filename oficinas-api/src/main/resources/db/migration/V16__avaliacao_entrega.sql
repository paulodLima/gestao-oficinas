ALTER TABLE oficina ADD COLUMN google_avaliacao_url varchar(500);

CREATE TABLE avaliacao_convite (
 id uuid PRIMARY KEY,
 oficina_id uuid NOT NULL,
 ordem_servico_id uuid NOT NULL UNIQUE,
 cliente_id uuid NOT NULL,
 contato_email varchar(254),
 contato_verificado_em timestamptz,
 token_hash char(64) NOT NULL UNIQUE,
 expira_em timestamptz NOT NULL,
 revogado_em timestamptz,
 created_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE(oficina_id,id),
 FOREIGN KEY(oficina_id,ordem_servico_id) REFERENCES ordem_servico(oficina_id,id),
 FOREIGN KEY(oficina_id,cliente_id) REFERENCES cliente(oficina_id,id)
);

CREATE TABLE avaliacao (
 id uuid PRIMARY KEY,
 oficina_id uuid NOT NULL,
 ordem_servico_id uuid NOT NULL UNIQUE,
 cliente_id uuid NOT NULL,
 convite_id uuid NOT NULL UNIQUE,
 nota integer NOT NULL CHECK(nota BETWEEN 1 AND 5),
 comentario varchar(2000),
 consentimento_publicacao boolean NOT NULL DEFAULT false,
 consentimento_texto varchar(500) NOT NULL,
 consentimento_em timestamptz,
 created_at timestamptz NOT NULL,
 FOREIGN KEY(oficina_id,ordem_servico_id) REFERENCES ordem_servico(oficina_id,id),
 FOREIGN KEY(oficina_id,cliente_id) REFERENCES cliente(oficina_id,id),
 FOREIGN KEY(oficina_id,convite_id) REFERENCES avaliacao_convite(oficina_id,id),
 CHECK(consentimento_publicacao = (consentimento_em IS NOT NULL))
);
CREATE INDEX avaliacao_oficina_idx ON avaliacao(oficina_id,created_at DESC,id);

CREATE TABLE avaliacao_auditoria (
 id uuid PRIMARY KEY,
 oficina_id uuid NOT NULL,
 ordem_servico_id uuid NOT NULL,
 cliente_id uuid NOT NULL,
 acao varchar(40) NOT NULL,
 created_at timestamptz NOT NULL,
 FOREIGN KEY(oficina_id,ordem_servico_id) REFERENCES ordem_servico(oficina_id,id),
 FOREIGN KEY(oficina_id,cliente_id) REFERENCES cliente(oficina_id,id)
);
