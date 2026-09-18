Aqui está um prompt completo, seguindo exatamente o padrão que você pediu — adaptado para o seu sistema de eleição 👇

---

# 🗳️ Implementação do Sistema de Eleição Digital

Você é um desenvolvedor full stack sênior especializado em Angular 19 e Spring Boot, responsável por implementar um sistema completo de votação online para entidades de previdência privada.

---

## ### Business

* Permitir que uma entidade crie e gerencie eleições
* Cadastro de candidatos com:

  * Nome
  * Foto
  * Proposta (exibida em uma página pública estilo single page)
* Cadastro de eleitores com:

  * Nome
  * Email
  * Telefone (opcional)
  * Identificador único (ex: CPF ou matrícula)
* Envio de e-mail para eleitores com link único para votação
* Cada eleitor pode votar apenas uma vez
* Após votar, o eleitor recebe um e-mail de confirmação
* A eleição deve possuir:

  * Data e hora de início
  * Data e hora de fim
* O sistema deve impedir votos fora do período configurado
* Enviar e-mail de lembrete 1 hora antes do encerramento:

  * Apenas para eleitores que ainda não votaram
* Registrar votos com segurança e rastreabilidade
* Exibir resultado da eleição após encerramento

---

## ### Technical

* Implementar nos projetos existentes:

  * `./frontend` (Angular 19)
  * `./backend` (Spring Boot)

* O frontend deve consumir exclusivamente a API do backend

* O backend deve ser responsável por:

  * Regras de negócio
  * Segurança
  * Envio de e-mails
  * Geração e validação de tokens

---

### 🔐 Segurança

* Cada eleitor deve receber um link com token único
* O token deve conter:

  * ID do eleitor
  * ID da eleição
  * Data de expiração
* O token deve ser assinado (JWT ou equivalente seguro)
* O token deve ser invalidado após uso
* Não permitir múltiplos votos do mesmo eleitor
* Validar se a eleição está ativa antes de permitir o voto

---

### 📦 Entidades principais

* Eleicao
* Candidato
* Eleitor
* Voto
* TokenVotacao

---

### 📧 E-mails

* Envio inicial com link de votação
* Confirmação de voto realizado
* Lembrete automático (1h antes do fim)

---

### Backend

#### Endpoints obrigatórios:

**POST /api/eleicoes**

* Criar eleição

**GET /api/eleicoes/{id}**

* Detalhar eleição

**POST /api/candidatos**

* Cadastrar candidato

**POST /api/eleitores**

* Cadastrar eleitores

**POST /api/eleicoes/{id}/disparar**

* Disparar e-mails com tokens

**GET /api/votacao?token=<token>**

* Validar token e retornar dados da eleição + candidatos

**POST /api/votacao**

* Registrar voto

**GET /api/eleicoes/{id}/resultado**

* Retornar resultado da eleição

---

### Status Code

* 200: Sucesso
* 400: Dados inválidos
* 401: Token inválido ou expirado
* 403: Eleitor já votou
* 404: Eleição ou eleitor não encontrado
* 422: Eleição fora do período

---

### Validação de endpoints

* Testar todos endpoints com curl ou Postman
* Garantir:

  * Token válido
  * Bloqueio de voto duplicado
  * Respeito ao período da eleição

---

## ### UI/UX

* Design responsivo (mobile-first)
* Página pública de votação simples e objetiva
* Página do candidato estilo landing page:

  * Foto à direita
  * Proposta à esquerda
* Lista de candidatos com:

  * Foto
  * Nome
  * Resumo da proposta
* Feedback visual ao votar:

  * Confirmação de sucesso
* Tela de erro para:

  * Token inválido
  * Token expirado
  * Eleição encerrada
* Skeleton loading durante carregamento
* Feedback visual de loading ao enviar voto
* Mensagem clara após voto realizado
* Interface simples para evitar erro do usuário
* Botões grandes e acessíveis (mobile-first)

---

## ### Regras de negócio

* Um eleitor só pode votar uma vez
* Não permitir voto fora do período
* Token só pode ser usado uma vez
* Voto não pode ser alterado após confirmação
* Lembrete só deve ser enviado para quem não votou

---

## ### Skills obrigatórias

* angular-best-practices — arquitetura, organização e performance
* spring-boot-best-practices — APIs robustas e seguras
* jwt-security — autenticação e validação segura de tokens
* email-service — envio assíncrono e confiável de e-mails
* ui-ux-pro-max — interfaces simples, claras e responsivas
* database-design — modelagem eficiente e segura
* clean-code — código limpo e bem estruturado

---

## ### Fora do Escopo

* *NÃO* implementar pagamento
* *NÃO* implementar autenticação de usuários administrativos (mockar se necessário)
* *NÃO* criar comentários no código
