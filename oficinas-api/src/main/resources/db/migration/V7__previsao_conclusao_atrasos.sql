CREATE TABLE ordem_servico_previsao (
 id uuid PRIMARY KEY,
 oficina_id uuid NOT NULL,
 ordem_servico_id uuid NOT NULL,
 previsao_anterior timestamptz,
 previsao_nova timestamptz,
 motivo_publico varchar(1000) NOT NULL,
 proxima_acao varchar(1000) NOT NULL,
 autor_id uuid NOT NULL REFERENCES proprietario(id),
 created_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT ordem_previsao_ordem_fk FOREIGN KEY (oficina_id, ordem_servico_id)
   REFERENCES ordem_servico(oficina_id, id),
 CONSTRAINT ordem_previsao_motivo CHECK (length(btrim(motivo_publico)) BETWEEN 1 AND 1000),
 CONSTRAINT ordem_previsao_acao CHECK (length(btrim(proxima_acao)) BETWEEN 1 AND 1000),
 CONSTRAINT ordem_previsao_alterada CHECK (previsao_anterior IS DISTINCT FROM previsao_nova)
);

CREATE INDEX ordem_previsao_historico_idx
 ON ordem_servico_previsao(oficina_id, ordem_servico_id, created_at DESC, id DESC);
