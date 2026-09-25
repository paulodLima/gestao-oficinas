# PRD · Validação móvel e entrega do MVP

Detalhamento da tarefa 20 aprovada em `docs/prompt.md`, `docs/techspec.md` e `docs/tasks.md`. Não acrescenta módulos ao produto.

## Requisitos funcionais

1. RF-01: jornada integrada de proprietário e cliente: entrada/OS, vistoria, fotos, adicional/decisão, previsão/atraso, entrega, avaliação e retorno em nova OS.
2. RF-02: desktop Chromium, Android/Chrome emulado, WebKit/iPhone emulado e 320px, sem rolagem horizontal indevida. O PRD global permite celulares reais ou ambiente equivalente; emulação não comprova câmera física/Safari instalado no iOS.
3. RF-03: alternativa de galeria quando câmera não seleciona arquivo, orientação de imagem, rede lenta/falha e reenvio sem duplicar fotos concluídas.
4. RF-04: labels, navegação por teclado, alvos de toque de 44px, contraste e estados vazio/carregando/erro.
5. RF-05: regressão automatizada, imagens Docker novas e SMTP real capturado localmente; nenhuma mensagem enviada a cliente real.
6. RF-06: instruções reproduzíveis, evidências visuais e ensaio de backup/restauração coordenado de PostgreSQL e arquivos privados em ambiente descartável.

## Limites

Não inclui produção, serviço pago de aparelhos remotos, SMTP externo/domínio real, publicação automática de avaliações, disparos WhatsApp, suporte irrestrito HEIC ou garantia de hardware móvel não testado. Registrar todas as limitações no relatório, sem afirmar validação física.
