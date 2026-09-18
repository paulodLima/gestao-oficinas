# Implementação do Sistema de Acompanhamento de Serviços de Oficinas

Você é um desenvolvedor full stack sênior especializado em Angular e Spring Boot, responsável por implementar uma plataforma de acompanhamento de serviços automotivos para pequenas oficinas e seus clientes.

O proprietário registra o andamento e as fotos do trabalho pelo celular. O cliente acompanha seu veículo, entende atrasos e autoriza serviços adicionais. Este documento orienta a implementação futura.

---

## ### Business

### Oficina e proprietário

* Cadastro e autenticação do proprietário, recuperação de acesso e configuração de nome, logo, contatos, endereço e horário da oficina.
* Isolamento dos dados de cada oficina.
* Painel de serviços ativos, atrasados, aguardando aprovação, aguardando peças e prontos para retirada.
* Busca por placa, cliente e número da ordem de serviço (OS).
* Lista e quadro por status, mostrando última atualização, previsão e tempo na etapa atual.

### Clientes e veículos

* Cadastrar cliente com nome, CPF, telefone e e-mail quando disponível; exigir um canal verificado para acesso por código.
* Cadastrar placa, marca, modelo, ano e cor do veículo; registrar quilometragem em cada atendimento.
* Um cliente pode ter vários veículos; cada veículo pode ter vários serviços históricos.
* Normalizar placas antigas e Mercosul para pesquisa.
* Preservar o cliente responsável em cada OS. Transferência de veículo não transfere acesso aos serviços do proprietário anterior.

### Ordens de serviço e andamento

* Abrir OS com número, cliente, veículo, relato inicial, data de entrada e previsão opcional de conclusão.
* Permitir somente uma OS ativa por veículo dentro da mesma oficina, inclusive em tentativas simultâneas.
* Status iniciais: Recebido, Em diagnóstico, Aguardando aprovação, Aguardando peças, Em manutenção, Em montagem, Em testes, Pronto para retirada, Entregue e Cancelado.
* Permitir etapas opcionais como Funilaria e Pintura. Nem todo atendimento precisa passar por todas as etapas.
* Registrar mudanças com autor, data, horário e explicação pública opcional. Retornos de etapa exigem motivo e preservam o histórico.
* Permitir publicação de texto e fotos sem mudança de status.
* Separar observações internas de conteúdo publicado para o cliente.

### Vistoria de entrada com fotos pelo celular

* Realizar todo o fluxo no navegador móvel: selecionar OS, preencher checklist, capturar fotos, revisar e salvar.
* Registrar quilometragem, combustível aproximado, objetos deixados, avarias aparentes e observações do cliente.
* Oferecer roteiro de fotos: frente, traseira, laterais, painel e detalhes de avarias; indicar fotos não realizadas sem bloquear o atendimento.
* Permitir câmera ou galeria, múltiplas fotos e legendas.
* Solicitar permissão da câmera quando necessária e oferecer seleção de arquivos quando a captura direta não estiver disponível.
* Mostrar miniaturas, progresso, falhas e tentativa de reenvio. Preservar campos na sessão durante falhas de envio; confirmar salvamento somente após resposta do servidor.
* Permitir rascunho e confirmação da vistoria. Correções posteriores preservam o conteúdo anterior em auditoria.
* Publicar ao cliente somente dados e fotos liberados pela oficina.

### Galeria de todas as etapas

* Abaixo do status atual no portal, mostrar todas as fotos publicadas daquela OS, incluindo vistoria e etapas anteriores.
* Ordenar cronologicamente, identificando etapa, legenda e data; permitir filtro por etapa e visualização ampliada com navegação por toque/deslize.
* Manter fotos anteriores disponíveis quando o status mudar.
* A própria galeria permite visualizar a evolução. Não criar módulo separado de antes/depois nem exigir pares de fotos.
* Fotos internas nunca aparecem no portal. Alterações de visibilidade e remoções devem ser auditadas.

### Aprovação de serviço adicional

* Criar solicitação vinculada à OS com problema encontrado, justificativa, fotos opcionais, itens de peças/mão de obra, quantidades, valores e impacto no prazo.
* Exibir total adicional e previsão revisada antes da decisão.
* Permitir aprovação ou recusa por item independente, com comentário opcional. Itens dependentes formam grupo indivisível claramente identificado.
* Registrar identidade autenticada, decisão, data, horário e versão exata dos itens e valores apresentados.
* Estados da solicitação: Rascunho, Enviada, Parcialmente decidida, Decidida, Cancelada e Substituída; cada item registra sua decisão.
* Congelar o conteúdo enviado. Alterações geram nova versão; decisões pendentes da versão substituída ficam indisponíveis, preservando as decisões anteriores no histórico.
* Nunca considerar silêncio como autorização nem permitir execução de adicional pendente ou recusado.
* Apresentar o registro como aceite operacional, sem prometer assinatura eletrônica qualificada.

### Previsão e motivo de atraso

* Mostrar previsão quando informada, identificada como estimativa.
* Registrar alteração com previsão anterior, nova previsão, motivo, autor e horário.
* Motivos sugeridos: aguardando autorização, chegada de peça, peça incompatível, atraso do fornecedor, adicional encontrado e testes adicionais.
* Destacar serviços cuja previsão passou e que ainda não estejam prontos, entregues ou cancelados.
* Diferenciar atraso de execução de veículo pronto aguardando retirada.
* Exibir motivo público e próxima ação. Permitir informar ausência de nova previsão.

### Notificações úteis

* Notificações dentro do sistema e e-mail transacional quando houver endereço verificado.
* Eventos: abertura, solicitação de adicional, decisão do cliente, alteração relevante de previsão, veículo pronto e encerramento.
* Pequenas edições e fotos entram na linha do tempo, sem obrigar envio externo individual.
* Botão de compartilhamento manual no WhatsApp com mensagem e link seguro; não equivale a integração automática.
* Registrar envio, falha e nova tentativa, evitando duplicidade. Falha de notificação não desfaz atualização salva.

### Encerramento e retorno do veículo

* Pronto para retirada mantém a OS ativa; Entregue encerra o atendimento, registrando data, horário e responsável. Cancelado também retira a OS da lista ativa.
* Ao encerrar, resolver ou cancelar solicitações pendentes e bloquear novas atualizações operacionais e aprovações.
* Preservar vistoria, fotos, decisões e histórico para consulta interna da oficina.
* No portal autenticado, mostrar somente o serviço ativo do veículo autorizado. Sem OS ativa, mostrar “Nenhum serviço em andamento” e o contato da oficina.
* No retorno, criar outra OS. O portal autenticado passa a mostrar o novo atendimento, sem misturar conteúdo anterior.
* Revogar o acesso operacional da OS anterior. Links antigos nunca dão acesso nem são redirecionados ao novo serviço.
* Permitir resumo de encerramento e avaliação por acesso restrito específico, sem reativar o acompanhamento antigo.

### Avaliação após a entrega

* Solicitar nota de 1 a 5 e comentário opcional, uma avaliação por OS entregue.
* Avaliações privadas para a oficina por padrão; publicação exige autorização específica do cliente.
* Acesso de avaliação separado, restrito ao atendimento entregue, com validade inicial de sete dias.
* Link opcional para avaliar no Google, apresentado igualmente a todos, independentemente da nota interna.
* Depoimentos públicos não exibem placa, CPF ou fotos do veículo.

### Acesso do cliente

* Acesso no contexto de uma oficina por placa e código temporário enviado ao contato cadastrado e verificado.
* CPF é dado cadastral, nunca senha ou prova suficiente de identidade.
* Como alternativa, link aleatório exclusivo da OS, com expiração e revogação, compartilhado pela oficina.
* Não revelar publicamente nome, contato completo ou existência de atendimento ao pesquisar placa.
* Para autorizar adicionais, exigir confirmação por código no contato verificado; o link de acompanhamento sozinho não autoriza despesas.
* Validar oficina, cliente, veículo e OS em cada acesso, nunca apenas a placa.
* Clientes com vários veículos selecionam somente aqueles aos quais possuem acesso.

---

## ### Technical

* Utilizar os projetos existentes:
  * `./oficinas-app`: Angular 19.
  * `./oficinas-api`: Spring Boot e Java 21.
  * `./docker`: ambiente com PostgreSQL, backend e frontend.
* Frontend consome a API; backend aplica autorização, regras de negócio, validações e auditoria.
* Interface em português do Brasil, valores em BRL com precisão decimal e datas no fuso da oficina, inicialmente America/Sao_Paulo.
* Fotos em armazenamento privado, com miniaturas, validação de conteúdo e limites de tamanho. Não usar URLs públicas permanentes.
* Documentar formatos suportados e tratar fotos móveis incompatíveis com conversão ou mensagem clara e alternativa de captura.
* E-mails dependem de provedor transacional configurado; não simular envio bem-sucedido quando faltar configuração.

### Segurança

* Autenticar o proprietário e isolar oficinas em todos os recursos, inclusive arquivos.
* Códigos e links imprevisíveis, com expiração, revogação e limitação de tentativas.
* Código válido gera sessão limitada e deixa de poder ser reutilizado.
* Não incluir CPF ou outros dados pessoais em URLs e logs.
* HTTPS em produção e respostas do portal sem campos internos.
* Auditar status, vistoria, publicação, decisões, prazos e encerramento.
* Revalidar autorização a cada requisição, inclusive para sessões abertas antes do encerramento.

### Entidades principais

* Oficina, UsuarioProprietario, Cliente, Veiculo e VinculoClienteVeiculo.
* OrdemServico, AtualizacaoServico e HistoricoStatus.
* VistoriaEntrada, ItemVistoria e FotoServico.
* SolicitacaoAdicional, VersaoSolicitacao, ItemAdicional e DecisaoCliente.
* HistoricoPrevisao, Notificacao, CredencialAcessoCliente, Avaliacao e EventoAuditoria.

### E-mails

* Recuperação de acesso e código temporário.
* Abertura de serviço e solicitação de adicional.
* Decisão do cliente e alteração relevante de previsão.
* Veículo pronto, encerramento e convite para avaliação.
* Enviar somente aos destinatários vinculados ao atendimento e evitar repetição indevida.

### Backend

#### Grupos de endpoints previstos

* `/api/auth`: autenticação do proprietário.
* `/api/oficina`: dados da oficina autenticada.
* `/api/clientes` e `/api/veiculos`: cadastros autorizados.
* `/api/ordens-servico`: abertura, pesquisa e detalhes.
* Sub-recursos da OS: `status`, `atualizacoes`, `vistoria`, `fotos`, `previsao`, `adicionais` e `encerramento`.
* `/api/portal/acesso`: solicitação e validação de código/link.
* `/api/portal/servico-atual`: serviço ativo autorizado.
* `/api/portal/ordens-servico/{id}/adicionais/{solicitacaoId}/decisoes`: decisão autenticada sobre uma versão.
* `/api/portal/avaliacoes`: registro por acesso restrito de avaliação.
* Métodos, contratos e paginação serão detalhados na especificação técnica.

### Status Code

* 200/201/204: operação concluída.
* 400: dados inválidos.
* 401: credencial ou sessão inválida/expirada.
* 403: ação não permitida ao perfil.
* 404: recurso inexistente ou inacessível, sem revelar dados de terceiros.
* 409: OS ativa duplicada, versão substituída ou conflito de estado.
* 413: arquivo maior que o permitido.
* 429: excesso de tentativas.

### Validação de endpoints

* Testar isolamento entre oficinas/clientes e download indevido de fotos.
* Validar código expirado/reutilizado e revogação após encerramento.
* Testar abertura simultânea de OS, aprovação duplicada e decisão concorrente ao encerramento.
* Confirmar que link antigo não acessa nova OS e transferência de veículo não transfere histórico.
* Validar totais de adicionais, previsão alterada, upload incompleto e notificações duplicadas.

---

## ### UI/UX

* Experiência mobile-first com fluxos completos no Safari/iOS e Chrome/Android, além de desktop.
* Vistoria e publicação feitas junto ao veículo, sem depender de computador ou aplicativo instalado.
* Botões com área de toque mínima de 44 × 44 pixels, campos identificados, contraste legível e estados que não dependam só de cor.
* Suportar largura de 320 pixels; oferecer lista alternativa ao quadro no celular.
* Portal com identificação da oficina/veículo, status, previsão, última atualização, pendências, galeria abaixo do status e linha do tempo.
* Não apresentar porcentagem artificial de progresso baseada apenas no número de etapas.
* Miniaturas leves, carregamento progressivo, zoom e navegação por toque.
* Estados claros de carregamento, vazio, erro, acesso expirado, sem serviço ativo e envio pendente.
* Confirmações de aprovação mostram itens, versão, total e impacto no prazo.
* Encerramento exige confirmação explícita.
* Validar câmera, galeria, orientação de imagens, permissão negada, conexão lenta e reenvio em celulares reais ou ambiente equivalente.

---

## ### Regras de negócio

1. Cada oficina acessa exclusivamente seus dados.
2. Um veículo possui no máximo uma OS ativa por oficina.
3. Cada OS mantém cliente responsável e histórico próprios.
4. A galeria reúne todas as fotos publicadas da OS, mesmo após mudança de etapa.
5. Conteúdo interno nunca é enviado ao portal.
6. Adicionais exigem decisão explícita sobre a versão apresentada.
7. Alterações de previsão preservam motivo e histórico.
8. Pronto para retirada mantém o acompanhamento; Entregue e Cancelado encerram.
9. Encerramento revoga interações operacionais antigas; avaliação possui acesso independente.
10. Nova visita cria nova OS, com autorização própria.
11. CPF e placa não funcionam como senha.
12. Falhas de upload ou notificação não produzem falsas confirmações de sucesso.

---

## ### Skills e orientações de implementação

* Consultar habilidades disponíveis conforme a tarefa, sem pressupor habilidades inexistentes.
* `java-springboot`: backend.
* `ui-ux-pro-max` e `frontend-design`: experiência e interfaces.
* `cria-techspec`, `criar-tasks` e `executar-task`: detalhamento e execução quando solicitados.
* Este documento define requisitos; a implementação será planejada e solicitada separadamente.

---

## ### Fora do Escopo

* Estoque, compras, financeiro completo, pagamentos e emissão fiscal.
* Integração automática com WhatsApp/SMS; compartilhamento manual de link permanece incluído.
* Aplicativos nativos, operação totalmente offline, vídeos e inteligência artificial.
* Agenda, diagnóstico automatizado, consulta externa de placa e lembretes de manutenção.
* Módulo separado de comparação antes/depois.
* Histórico completo de serviços encerrados no portal; histórico interno da oficina permanece incluído.
* Equipe com permissões granulares e múltiplas filiais no primeiro lançamento.
