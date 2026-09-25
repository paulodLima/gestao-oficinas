CREATE TABLE ordem_servico_encerramento (
 ordem_servico_id uuid PRIMARY KEY,
 oficina_id uuid NOT NULL,
 tipo varchar(10) NOT NULL CHECK (tipo IN ('ENTREGUE','CANCELADO')),
 motivo varchar(1000),
 pendencias_canceladas integer NOT NULL CHECK (pendencias_canceladas >= 0),
 autor_id uuid NOT NULL REFERENCES proprietario(id),
 created_at timestamptz NOT NULL,
 FOREIGN KEY (oficina_id,ordem_servico_id) REFERENCES ordem_servico(oficina_id,id),
 CHECK (tipo <> 'CANCELADO' OR nullif(btrim(motivo),'') IS NOT NULL)
);

CREATE FUNCTION preservar_encerramento() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 RAISE EXCEPTION 'O registro de encerramento é imutável';
END;
$$;
CREATE TRIGGER encerramento_imutavel BEFORE UPDATE OR DELETE ON ordem_servico_encerramento
 FOR EACH ROW EXECUTE FUNCTION preservar_encerramento();
