# Bugs encontrados · Tarefa 20

| ID | Severidade | Evidência / causa | Correção e regressão |
|---|---|---|---|
| QA-01 | Alta | Volume novo `/data/photos` pertencia a root e não permitia escrita pelo usuário spring. | Dockerfile prepara diretório com proprietário correto; upload real no Compose QA. O volume QA inicial foi ajustado, sem tocar no volume do usuário. |
| QA-02 | Média | ImageIO ignorava EXIF; foto em retrato produzia miniatura deitada. | Leitura EXIF e transformação da miniatura; 8 orientações e JPEG real com EXIF testados. Dimensões limitadas antes da decodificação. |
| QA-03 | Alta | Resposta perdida após persistência exigia selecionar novamente, gerando nova chave e duplicação. | Arquivo/chave mantidos até sucesso, botão de reenvio; teste real persiste, aborta resposta e repete, resultando em uma foto. Nomes iguais e troca de OS cobertos por testes unitários. |
| QA-04 | Média | Interface não oferecia publicação de fotos, embora portal e adicionais dependessem dela. | Checkbox explícito antes de selecionar arquivos, desmarcado por padrão; escolha preservada no reenvio. Sem alterar fotos já salvas. |
| QA-05 | Média | Erro ao carregar adicionais desaparecia quando lista estava vazia. | Estado de erro visível e botão para recarregar. |
| QA-06 | Média | Axe apontou contraste insuficiente de texto secundário sobre fundos claros. | Token de cor secundária escurecido, mantendo identidade visual. Rechecagem passou nas quatro configurações. |
| QA-07 | Média | Botões menores que 44px e foco invisível no seletor de fotos. | Alvos ampliados, seletor recebe foco visível; quatro configurações aprovadas. |
| QA-08 | Média | Links da navegação perdiam nome acessível quando o texto era ocultado no celular. | Labels explícitos em painel/perfil/clientes/veículos/nova OS; Axe móvel sem violações. |
| QA-09 | Alta | A nova navegação substituiu a página inicial que continha o único botão de logout. | Saída disponível no sidebar desktop/móvel, erro de rede e sessão já revogada tratados; 3 testes unitários e E2E real de recuperação/logout. |
| QA-10 / R20-01 | Alta | Arquivos publicados preservavam EXIF/GPS, inclusive na ampliação; comportamento preexistente contrário à especificação. | Corrigido: novos arquivos são orientados e reencodificados sem metadados; JPEG/PNG/WebP validados, visualização até 2048px e miniatura 480px. Legados são sanitizados no download sem alteração em disco. Fixture sintética com câmera/GPS/orientação, teste unitário e HTTP autenticado para novos/legados, WebP e rejeição acima de 40MP. |
| QA-11 / R20-02 | Média | O limite de três workers era por lote, e retries iniciavam fora desse limite. | Corrigido: fila de vagas compartilhada entre seleções/reenvios; teste mantém quatro retries e seleção nova, liberando respostas gradualmente e verificando três simultâneos, incluindo erro. |
| QA-12 / R20-03 | Média | Fieldset desabilitado em OS encerrada também bloqueava o botão de recarga da consulta de fotos. | Corrigido: leitura/recarga fora da região desabilitada; envio/remover permanecem bloqueados. Teste DOM e jornada integrada com primeiro GET abortado e repetição real. |
| QA-13 | Alta | Angular 19 sem patches para os advisories identificados pelo npm audit de produção. | Corrigido com autorização do responsável: Angular core 20.3.32, CLI/SSR 20.3.37, TypeScript 5.9.3 e migrações oficiais. Auditoria npm de produção sem alertas; build/111 testes frontend e regressões repetidos. Cinco alertas moderados de desenvolvimento permanecem como risco residual no relatório, não como correção declarada. |

Todos os problemas acima corrigidos e retestados. Os seletores de quatro E2Es antigos foram atualizados para a navegação atual e Mailpit/baseURL configuráveis. As asserções funcionais foram preservadas. O scan de contraste de autenticação aguarda a animação de entrada terminar, evitando medir uma tela transitoriamente transparente.

Preparação QA: rede interna exclusiva não publicava portas no Docker Desktop. Rede de preview adicional somente para app/Mailpit; API/Postgres continuam internos. Portas vinculadas a loopback. Mailpit sem relay SMTP.

Harness após Angular 20: uma execução Android falhou ao ler o corpo de resposta já descartado pelo CDP, apesar da OS criada na interface. O teste agora verifica POST 201, heading e a única OS persistida pela API autenticada. Não repete o POST nem simula sucesso; preserva as asserções funcionais.
