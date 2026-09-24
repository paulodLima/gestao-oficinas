# Especificação técnica — Acesso seguro do cliente

Complementa [docs/techspec.md](../../docs/techspec.md), seções 4 e 5.

- Persistir desafios HMAC, expiração de 10 minutos, cinco tentativas e uso único, vinculados a oficina, cliente e contexto de placa.
- `POST /api/portal/acesso/codigo` sempre responde 202 sem revelar cadastro; `POST .../validacao` estabelece sessão limitada de cliente.
- Links são aleatórios de 256 bits, armazenados apenas como hash, expiram/revogam e estabelecem sessão somente leitura de uma OS.
- Portal revalida vínculo atual, oficina e OS ativa em cada chamada; payloads nunca incluem CPF, contato ou notas internas.
