ALTER TABLE cliente ADD COLUMN acesso_versao bigint NOT NULL DEFAULT 0;
ALTER TABLE portal_desafio ADD COLUMN acesso_versao bigint NOT NULL DEFAULT -1;
ALTER TABLE adicional_desafio ADD COLUMN acesso_versao bigint NOT NULL DEFAULT -1;
CREATE INDEX portal_desafio_cliente_data_idx ON portal_desafio(oficina_id,cliente_id,created_at DESC);
