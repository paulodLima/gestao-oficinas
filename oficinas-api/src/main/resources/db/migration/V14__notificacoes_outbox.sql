CREATE TABLE notificacao (
 id uuid PRIMARY KEY,
 oficina_id uuid NOT NULL,
 ordem_servico_id uuid NOT NULL,
 evento varchar(40) NOT NULL,
 referencia varchar(100) NOT NULL,
 titulo varchar(160) NOT NULL,
 mensagem varchar(500) NOT NULL,
 lida_em timestamptz,
 created_at timestamptz NOT NULL DEFAULT now(),
 FOREIGN KEY (oficina_id,ordem_servico_id) REFERENCES ordem_servico(oficina_id,id),
 UNIQUE (oficina_id,evento,referencia),
 UNIQUE (oficina_id,id)
);
CREATE INDEX notificacao_central_idx ON notificacao(oficina_id,created_at DESC,id);
CREATE TABLE notificacao_email (
 id uuid PRIMARY KEY,
 oficina_id uuid NOT NULL,
 notificacao_id uuid NOT NULL UNIQUE,
 cliente_id uuid NOT NULL,
 destinatario varchar(254) NOT NULL,
 verificado_em timestamptz NOT NULL,
 estado varchar(20) NOT NULL DEFAULT 'PENDENTE',
 tentativas integer NOT NULL DEFAULT 0 CHECK (tentativas >= 0),
 tentativas_ciclo integer NOT NULL DEFAULT 0 CHECK (tentativas_ciclo BETWEEN 0 AND 5),
 proxima_tentativa_em timestamptz NOT NULL DEFAULT now(),
 ultimo_erro varchar(60),
 ultimo_reenvio_em timestamptz,
 enviado_em timestamptz,
 FOREIGN KEY (oficina_id,notificacao_id) REFERENCES notificacao(oficina_id,id),
 FOREIGN KEY (oficina_id,cliente_id) REFERENCES cliente(oficina_id,id),
 CHECK (estado IN ('PENDENTE','ENVIADO','FALHOU','CANCELADO'))
);
CREATE INDEX notificacao_email_fila_idx ON notificacao_email(proxima_tentativa_em,id) WHERE estado='PENDENTE';
CREATE TABLE notificacao_email_tentativa (
 id uuid PRIMARY KEY,
 email_id uuid NOT NULL REFERENCES notificacao_email(id),
 numero integer NOT NULL,
 resultado varchar(20) NOT NULL CHECK (resultado IN ('ENVIADO','FALHOU','CANCELADO')),
 erro varchar(60),
 created_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE(email_id,numero)
);
