CREATE TABLE cliente (
 id uuid PRIMARY KEY,
 oficina_id uuid NOT NULL REFERENCES oficina(id),
 nome varchar(120) NOT NULL,
 cpf char(11) NOT NULL,
 telefone varchar(30) NOT NULL DEFAULT '',
 email varchar(254) NOT NULL DEFAULT '',
 email_verificado_em timestamptz,
 ativo boolean NOT NULL DEFAULT true,
 versao bigint NOT NULL DEFAULT 0,
 created_at timestamptz NOT NULL DEFAULT now(),
 updated_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT cliente_oficina_id_unique UNIQUE (oficina_id, id),
 CONSTRAINT cliente_cpf_unique UNIQUE (oficina_id, cpf),
 CONSTRAINT cliente_cpf_digits CHECK (cpf ~ '^[0-9]{11}$')
);

CREATE INDEX cliente_busca_nome_idx ON cliente (oficina_id, lower(nome));
CREATE INDEX cliente_busca_email_idx ON cliente (oficina_id, lower(email));

CREATE TABLE veiculo (
 id uuid PRIMARY KEY,
 oficina_id uuid NOT NULL REFERENCES oficina(id),
 placa varchar(7) NOT NULL,
 marca varchar(80) NOT NULL,
 modelo varchar(120) NOT NULL,
 ano integer,
 cor varchar(50) NOT NULL DEFAULT '',
 versao bigint NOT NULL DEFAULT 0,
 created_at timestamptz NOT NULL DEFAULT now(),
 updated_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT veiculo_oficina_id_unique UNIQUE (oficina_id, id),
 CONSTRAINT veiculo_placa_unique UNIQUE (oficina_id, placa),
 CONSTRAINT veiculo_placa_formato CHECK (placa ~ '^[A-Z]{3}([0-9]{4}|[0-9][A-Z][0-9]{2})$'),
 CONSTRAINT veiculo_ano_valido CHECK (ano IS NULL OR ano BETWEEN 1886 AND 2200)
);

CREATE INDEX veiculo_busca_modelo_idx ON veiculo (oficina_id, lower(modelo));

CREATE TABLE vinculo_cliente_veiculo (
 id uuid PRIMARY KEY,
 oficina_id uuid NOT NULL,
 cliente_id uuid NOT NULL,
 veiculo_id uuid NOT NULL,
 inicio_em timestamptz NOT NULL DEFAULT now(),
 fim_em timestamptz,
 criado_por uuid NOT NULL REFERENCES proprietario(id),
 CONSTRAINT vinculo_cliente_fk FOREIGN KEY (oficina_id, cliente_id) REFERENCES cliente(oficina_id, id),
 CONSTRAINT vinculo_veiculo_fk FOREIGN KEY (oficina_id, veiculo_id) REFERENCES veiculo(oficina_id, id),
 CONSTRAINT vinculo_periodo_valido CHECK (fim_em IS NULL OR fim_em >= inicio_em)
);

CREATE UNIQUE INDEX vinculo_veiculo_atual_unique
 ON vinculo_cliente_veiculo (oficina_id, veiculo_id) WHERE fim_em IS NULL;
CREATE INDEX vinculo_cliente_atual_idx
 ON vinculo_cliente_veiculo (oficina_id, cliente_id) WHERE fim_em IS NULL;

CREATE TABLE verificacao_email_cliente (
 id uuid PRIMARY KEY,
 oficina_id uuid NOT NULL,
 cliente_id uuid NOT NULL,
 codigo_hash char(64) NOT NULL,
 expira_em timestamptz NOT NULL,
 tentativas integer NOT NULL DEFAULT 0,
 usado_em timestamptz,
 created_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT verificacao_cliente_fk FOREIGN KEY (oficina_id, cliente_id) REFERENCES cliente(oficina_id, id),
 CONSTRAINT verificacao_tentativas CHECK (tentativas BETWEEN 0 AND 5)
);

CREATE INDEX verificacao_cliente_idx ON verificacao_email_cliente (oficina_id, cliente_id, created_at DESC);

CREATE TABLE cadastro_auditoria (
 id uuid PRIMARY KEY,
 oficina_id uuid NOT NULL REFERENCES oficina(id),
 proprietario_id uuid NOT NULL REFERENCES proprietario(id),
 recurso varchar(30) NOT NULL,
 recurso_id uuid NOT NULL,
 acao varchar(60) NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX cadastro_auditoria_recurso_idx
 ON cadastro_auditoria (oficina_id, recurso, recurso_id, created_at);
