CREATE TABLE ordem_servico_evento (
 id uuid PRIMARY KEY,
 oficina_id uuid NOT NULL,
 ordem_servico_id uuid NOT NULL,
 tipo varchar(20) NOT NULL,
 status_anterior varchar(40),
 status_novo varchar(40),
 motivo varchar(1000),
 texto_publico varchar(2000),
 texto_interno varchar(2000),
 publicada boolean NOT NULL DEFAULT false,
 autor_id uuid NOT NULL REFERENCES proprietario(id),
 created_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT ordem_evento_ordem_fk FOREIGN KEY (oficina_id, ordem_servico_id)
   REFERENCES ordem_servico(oficina_id, id),
 CONSTRAINT ordem_evento_tipo CHECK (tipo IN ('STATUS','ATUALIZACAO')),
 CONSTRAINT ordem_evento_status CHECK (
   (tipo='STATUS' AND status_novo IS NOT NULL) OR
   (tipo='ATUALIZACAO' AND status_anterior IS NULL AND status_novo IS NULL)
 ),
 CONSTRAINT ordem_evento_conteudo CHECK (
   tipo='STATUS' OR nullif(btrim(texto_publico),'') IS NOT NULL OR nullif(btrim(texto_interno),'') IS NOT NULL
 ),
 CONSTRAINT ordem_evento_publicacao CHECK (NOT publicada OR nullif(btrim(texto_publico),'') IS NOT NULL)
);

CREATE INDEX ordem_evento_linha_tempo_idx
 ON ordem_servico_evento(oficina_id, ordem_servico_id, created_at DESC, id DESC);

INSERT INTO ordem_servico_evento(
 id,oficina_id,ordem_servico_id,tipo,status_novo,autor_id,created_at
)
SELECT gen_random_uuid(),oficina_id,id,'STATUS','RECEBIDO',criado_por,created_at
  FROM ordem_servico;
