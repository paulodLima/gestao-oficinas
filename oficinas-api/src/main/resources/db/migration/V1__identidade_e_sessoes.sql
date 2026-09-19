CREATE TABLE oficina (
 id uuid PRIMARY KEY, nome varchar(120) NOT NULL, slug varchar(150) NOT NULL UNIQUE,
 fuso varchar(80) NOT NULL DEFAULT 'America/Sao_Paulo',
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE proprietario (
 id uuid PRIMARY KEY, oficina_id uuid NOT NULL REFERENCES oficina(id), nome varchar(120) NOT NULL,
 email varchar(254) NOT NULL UNIQUE, senha_hash varchar(100) NOT NULL, ativo boolean NOT NULL DEFAULT true,
 auth_version bigint NOT NULL DEFAULT 0,
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE recuperacao_senha (
 token_hash char(64) PRIMARY KEY, proprietario_id uuid NOT NULL REFERENCES proprietario(id),
 expires_at timestamptz NOT NULL, used_at timestamptz
);
CREATE INDEX recuperacao_proprietario_idx ON recuperacao_senha(proprietario_id);
CREATE TABLE auth_limite (
 chave varchar(64) PRIMARY KEY, janela bigint NOT NULL, tentativas integer NOT NULL
);
CREATE TABLE identidade_auditoria (
 id uuid PRIMARY KEY, oficina_id uuid NOT NULL REFERENCES oficina(id),
 proprietario_id uuid NOT NULL REFERENCES proprietario(id),
 acao varchar(60) NOT NULL, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE SPRING_SESSION (
 PRIMARY_ID CHAR(36) NOT NULL PRIMARY KEY, SESSION_ID CHAR(36) NOT NULL,
 CREATION_TIME BIGINT NOT NULL, LAST_ACCESS_TIME BIGINT NOT NULL,
 MAX_INACTIVE_INTERVAL INT NOT NULL, EXPIRY_TIME BIGINT NOT NULL, PRINCIPAL_NAME VARCHAR(100)
);
CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION(SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION(EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION(PRINCIPAL_NAME);
CREATE TABLE SPRING_SESSION_ATTRIBUTES (
 SESSION_PRIMARY_ID CHAR(36) NOT NULL REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE,
 ATTRIBUTE_NAME VARCHAR(200) NOT NULL, ATTRIBUTE_BYTES BYTEA NOT NULL,
 PRIMARY KEY(SESSION_PRIMARY_ID, ATTRIBUTE_NAME)
);

