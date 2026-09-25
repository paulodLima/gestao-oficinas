# Tarefa 10.0: Acesso seguro do cliente

<critical>Ler prd.md e techspec.md desta pasta antes da implementação.</critical>

## Requisitos

- Código temporário não enumerável, de uso único e limitado.
- Sessão móvel limitada para identidade de cliente e seleção de veículos autorizados.
- Link exclusivo de OS com expiração e revogação, sem permissão de aprovar adicionais.
- Segurança por oficina e revalidação de vínculo.

## Subtarefas

- [x] 10.1 Solicitação e validação de código sem enumeração.
- [x] 10.2 Sessão limitada e veículos autorizados.
- [x] 10.3 Emissão, consumo e revogação de link exclusivo de OS.
- [x] 10.4 Tela móvel de acesso.
- [x] 10.5 Testes de expiração, tentativas, isolamento e privilégios.

## Evidências de conclusão

- Política de expiração, tentativas, uso único, revogação e duração de sessão coberta por testes unitários.
- Integração coberta para não enumeração, isolamento por oficina, vínculo vigente e ausência de privilégios de escrita no link.
- Portal móvel coberto para CSRF, solicitação genérica, validação e seleção de veículos autorizados.
- `mvn test` (testes unitários): 20 aprovados.
- `npm test -- --watch=false --browsers=ChromeHeadless`: 32 aprovados.
- `mvn -DskipTests compile` e `npm run build`: aprovados.
- A execução local dos testes Testcontainers depende do Docker Desktop; nesta estação o serviço está indisponível, embora a suíte de integração compile corretamente.
