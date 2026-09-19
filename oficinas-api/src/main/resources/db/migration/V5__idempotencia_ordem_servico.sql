CREATE TABLE requisicao_idempotente (
 id uuid PRIMARY KEY,
 oficina_id uuid NOT NULL REFERENCES oficina(id),
 proprietario_id uuid NOT NULL REFERENCES proprietario(id),
 operacao varchar(60) NOT NULL,
 chave uuid NOT NULL,
 hash_corpo varchar(64) NOT NULL,
 recurso_id uuid,
 created_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT requisicao_idempotente_unique UNIQUE (oficina_id, proprietario_id, operacao, chave)
);
