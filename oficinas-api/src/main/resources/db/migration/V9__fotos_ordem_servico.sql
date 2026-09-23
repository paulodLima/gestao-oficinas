CREATE TABLE ordem_servico_foto (
 id uuid PRIMARY KEY,
 oficina_id uuid NOT NULL,
 ordem_servico_id uuid NOT NULL,
 etapa varchar(40) NOT NULL,
 legenda varchar(500),
 publicada boolean NOT NULL DEFAULT false,
 estado varchar(20) NOT NULL DEFAULT 'PRONTA',
 nome_arquivo varchar(255) NOT NULL,
 tipo_conteudo varchar(80) NOT NULL,
 tamanho_bytes bigint NOT NULL,
 chave_arquivo varchar(180) NOT NULL,
 chave_miniatura varchar(180),
 upload_id uuid NOT NULL,
 criado_por uuid NOT NULL REFERENCES proprietario(id),
 created_at timestamptz NOT NULL DEFAULT now(),
 updated_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT os_foto_os_fk FOREIGN KEY (oficina_id, ordem_servico_id) REFERENCES ordem_servico(oficina_id, id),
 CONSTRAINT os_foto_etapa CHECK (etapa IN ('RECEBIDO','EM_DIAGNOSTICO','AGUARDANDO_APROVACAO','AGUARDANDO_PECAS','EM_MANUTENCAO','EM_MONTAGEM','EM_TESTES','PRONTO_PARA_RETIRADA','ENTREGUE','CANCELADO','FUNILARIA','PINTURA')),
 CONSTRAINT os_foto_estado CHECK (estado IN ('PRONTA','REMOVIDA')),
 CONSTRAINT os_foto_tamanho CHECK (tamanho_bytes > 0 AND tamanho_bytes <= 10485760),
 CONSTRAINT os_foto_upload_unique UNIQUE (oficina_id, ordem_servico_id, upload_id)
);
CREATE INDEX os_foto_ordem_idx ON ordem_servico_foto (oficina_id, ordem_servico_id, created_at DESC);
