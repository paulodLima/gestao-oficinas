# Tarefa 9.0: Vistoria de entrada totalmente móvel

<critical>Ler os arquivos de prd.md e techspec.md desta pasta antes de implementar.</critical>

## Visão Geral

Entregar a vistoria de entrada da OS em fluxo mobile-first, com rascunho, confirmação versionada e roteiro opcional de fotos.

<skills>

### Conformidade com Skills Padrões

- `java-springboot` para API, migrações e transações.
- `frontend-design` e `ui-ux-pro-max` para o fluxo móvel.
</skills>

<requirements>

- Checklist com quilometragem, combustível, objetos, avarias e observações.
- Rascunho editável; confirmação imutável; correção auditável como nova versão.
- Roteiro de fotos opcional que referencia fotos privadas já enviadas para a OS.
- Persistência do formulário no navegador até confirmação bem-sucedida.
- Escopo por oficina, OS ativa, CSRF, auditoria e controle otimista.
</requirements>

## Subtarefas

- [x] 9.1 Persistir checklist e rascunho de vistoria ligado à OS.
- [x] 9.2 Criar roteiro de fotos opcional e integração com câmera/galeria existente.
- [x] 9.3 Implementar confirmação e correção auditável sem apagar versão anterior.
- [x] 9.4 Preservar campos durante falha de envio.
- [x] 9.5 Testes unitários, integração e E2E móvel pertinentes.

## Critérios de Sucesso

- Fluxo íntegro em largura de 320 px, com alternativa à câmera.
- Rascunho e versões anteriores preservados; não há confirmação falsa.
- Dados inacessíveis entre oficinas e fora de OS ativa.

## Testes da Tarefa

- [x] Unidade: validação, confirmação e correção.
- [x] Integração: rascunho, histórico e isolamento.
- [x] E2E: falha de envio e retomada de formulário.
