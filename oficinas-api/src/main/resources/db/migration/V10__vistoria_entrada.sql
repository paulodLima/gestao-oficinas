CREATE TABLE ordem_servico_vistoria (
 id uuid PRIMARY KEY,
 oficina_id uuid NOT NULL,
 ordem_servico_id uuid NOT NULL,
 numero_versao integer NOT NULL,
 estado varchar(20) NOT NULL,
 checklist jsonb NOT NULL,
 motivo_correcao varchar(1000),
 versao_anterior_id uuid,
 criado_por uuid NOT NULL REFERENCES proprietario(id),
 created_at timestamptz NOT NULL DEFAULT now(),
 updated_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT vistoria_os_fk FOREIGN KEY (oficina_id,ordem_servico_id) REFERENCES ordem_servico(oficina_id,id),
 CONSTRAINT vistoria_anterior_fk FOREIGN KEY (versao_anterior_id) REFERENCES ordem_servico_vistoria(id),
 CONSTRAINT vistoria_estado CHECK (estado IN ('RASCUNHO','CONFIRMADA')),
 CONSTRAINT vistoria_versao_unique UNIQUE (oficina_id,ordem_servico_id,numero_versao)
);
CREATE UNIQUE INDEX vistoria_rascunho_unique ON ordem_servico_vistoria(oficina_id,ordem_servico_id) WHERE estado='RASCUNHO';
