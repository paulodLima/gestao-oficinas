# Especificação técnica — Vistoria de entrada móvel

Este recorte complementa [docs/techspec.md](../../docs/techspec.md), seções 3 e 5.

- Criar migração Flyway para vistoria e versões, escopadas por oficina e OS.
- Uma versão rascunho editável por OS; confirmação congela os dados. Correção cria versão nova com motivo e referência à versão anterior.
- Endpoints privados: `PUT/GET /api/ordens-servico/{id}/vistoria`, `POST .../confirmacoes` e `POST .../correcoes`.
- Todas as operações derivam oficina e proprietário da sessão, exigem CSRF, validam OS ativa e registram auditoria.
- O Angular mantém o formulário de rascunho no `sessionStorage` até confirmação, incluindo em falha de rede.
- Fotos do roteiro referenciam a galeria privada da OS, sem criar URL pública.

## Validação

Campos textuais possuem limites; quilometragem é inteiro não negativo; combustível aceita escala controlada. Controle otimista usa a versão da OS. Consultas fora da oficina retornam 404.
