ALTER TABLE oficina
 ADD COLUMN telefone varchar(30) NOT NULL DEFAULT '',
 ADD COLUMN email_contato varchar(254) NOT NULL DEFAULT '',
 ADD COLUMN endereco varchar(500) NOT NULL DEFAULT '',
 ADD COLUMN horario varchar(1000) NOT NULL DEFAULT '',
 ADD COLUMN perfil_publico boolean NOT NULL DEFAULT false,
 ADD COLUMN logo bytea,
 ADD COLUMN versao bigint NOT NULL DEFAULT 0;
ALTER TABLE oficina ADD CONSTRAINT oficina_logo_size CHECK (octet_length(logo) <= 2097152);
