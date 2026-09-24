CREATE TABLE portal_desafio (
 id uuid PRIMARY KEY, oficina_id uuid NOT NULL, cliente_id uuid NOT NULL, placa varchar(10) NOT NULL,
 codigo_hash char(64) NOT NULL, expira_em timestamptz NOT NULL, tentativas integer NOT NULL DEFAULT 0,
 usado_em timestamptz, created_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT portal_desafio_cliente_fk FOREIGN KEY(oficina_id,cliente_id) REFERENCES cliente(oficina_id,id)
);
CREATE INDEX portal_desafio_busca_idx ON portal_desafio(id,oficina_id);
CREATE TABLE portal_link_os (
 id uuid PRIMARY KEY, oficina_id uuid NOT NULL, ordem_servico_id uuid NOT NULL, token_hash char(64) NOT NULL UNIQUE,
 expira_em timestamptz NOT NULL, revogado_em timestamptz, usado_em timestamptz, criado_por uuid NOT NULL REFERENCES proprietario(id), created_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT portal_link_os_fk FOREIGN KEY(oficina_id,ordem_servico_id) REFERENCES ordem_servico(oficina_id,id)
);
