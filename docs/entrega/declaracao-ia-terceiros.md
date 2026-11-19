# Declaração de uso de IA e de componentes de terceiros

Baseado no Anexo E do manual do POOALAB. Mantenha esta declaração atualizada durante o projeto.

**Solução:** Cadê? — Achados e Perdidos UCSal
**Turma:** _A preencher_ · **Equipe:** _A preencher_
**Integrantes:** _A preencher_

## 1. Uso de inteligência artificial

Houve uso de IA? (X) Sim ( ) Não

| Ferramenta ou modelo | Finalidade ou tarefa | Parte afetada | Revisão e validação realizadas |
|---|---|---|---|
| Claude (Claude Code) | Revisão do documento de requisitos e da stack; criação da estrutura inicial dos repositórios, do backlog e da organização do Project (modelo do SCEA) | Documento de requisitos v3; estrutura inicial do back-end e do front-end | Revisado pela equipe; build e testes executados |
| Claude (Claude Code) | Implementação do ACE-01: entidade de usuário, perfis, criação do administrador inicial e testes | Módulo `access` (persistência e serviço), `shared.security.Role`, `shared.time` | Revisão no PR; testes com Testcontainers executados |
| Claude (Claude Code) | Implementação do ACE-02 (versão inicial): login por código no Redis, emissão e validação do JWT, endpoints de login, tratador de erros, documentação no Swagger e testes | Módulo `access` (persistência, serviço, controller), `shared.security`, `shared.exception`, `shared.web`; `docs/login-codigos-redis.md` | Revisão no PR; testes com Testcontainers (PostgreSQL e Redis) executados |
| Claude (Claude Code) | Revisão do ACE-01/ACE-02 e CI: troca do Redis pelo PostgreSQL, bloqueio contra força bruta, chave do JWT obrigatória, rotas públicas explícitas, erros 401/403/500 como `ProblemDetail`, workflow de CI e automação do Status no Project | `access.persistence.logincode`, `access.service.login`, `shared.security`, `shared.web.error`; `.github/`; `docs/` | Revisão no PR; `./mvnw verify` local e no CI; aplicação executada sem e com `JWT_SECRET` |
| Claude (Claude Code) | Implementação do ACH-01: situação do objeto com o padrão State (sealed interface + records), exceção de transição proibida, conversor JPA e testes de todas as transições; tradução do Javadoc para o português | `founditems.persistence.status`, `shared.exception` | Revisão no PR; testes executados localmente e no CI |
| Claude (Claude Code) | Implementação do ACE-03: anotação `@RequiresRole` como meta-anotação de `@PreAuthorize` (D20, que substituiu o interceptor da D19), leitura por reflexão para o Swagger, hierarquia de perfis, CORS, teste que exige a anotação em toda rota não pública, perfis no Swagger, testes e documentação do fluxo | `shared.security`, `shared.web.openapi`; `docs/tecnico/autorizacao.md`, D19, D20 | Revisão no PR; `./mvnw verify` executado |
| Claude (Claude Code) | AVI-01: plano e código de referência do envio de e-mail (`EmailSender` por SMTP, execução assíncrona e e-mail do código de login), revisão e documentação | `shared.mail`, `shared.event`, `notifications.service.login`; `docs/`, `README.md` | Código digitado e revisado pelo integrante; `./mvnw verify` executado |
| Claude (chat do claude.ai, não o Claude Code) | ADM-01: desenho e revisão (uma conversa), geração do código e dos testes (outra) e revisão independente (terceira): contrato do evento de histórico, registro imutável, repositório estreito e ouvinte síncrono que grava na mesma transação | `shared.audit`, `administration.persistence.audit`, `administration.service.audit` | Código revisado por outros dois chats (ADM e revisor independente); ajustes de acento e de dados de teste aplicados; ./mvnw verify executado (134 testes, 0 falhas); revisão no PR ainda pendente |
| Claude (Claude Code) | ACH-02: categorias com risco padrão (RN04), armazenamento de fotos (`PhotoStorage`), cadastro de objeto achado com caixa sugerida, histórico e evento, rotas e testes; D21 | `shared.category`, `shared.storage`, `founditems`, `shared.web.error`; `docs/` | Revisão no PR; `./mvnw verify` executado |

## 2. Componentes e recursos de terceiros

Houve uso de componentes de terceiros? (X) Sim ( ) Não

| Componente ou recurso | Origem ou link | Versão e licença | Função e alterações realizadas |
|---|---|---|---|
| Spring Boot | https://spring.io/projects/spring-boot | 4.1.2, Apache 2.0 | Framework do back-end; projeto gerado pelo Spring Initializr |
| PostgreSQL | https://www.postgresql.org | 17, PostgreSQL License | Banco de dados |
| Mailpit | https://github.com/axllent/mailpit | 1.27, MIT | Captura de e-mails em desenvolvimento |
| springdoc-openapi | https://springdoc.org | 3.0.2, Apache 2.0 | Gera o documento OpenAPI e serve o Swagger UI (D17) |
| Testcontainers | https://testcontainers.com | MIT | Banco temporário nos testes |
| ArchUnit | https://www.archunit.org | 1.4.1, Apache 2.0 | Teste das fronteiras entre módulos (D10) |
| GitHub Actions (`actions/checkout`, `actions/setup-java`, `actions/upload-artifact`) | https://github.com/actions | v7, v6 e v7, MIT | CI: build e testes a cada PR (QUA-01) |
| Angular | https://angular.dev | 22, MIT | Framework do front-end |

## 3. Materiais ou código preexistentes

Houve material produzido antes do PooaLAB? ( ) Sim (X) Não

## 4. Declaração da equipe

Declaramos que as informações acima são completas e verdadeiras; que revisamos e testamos as contribuições apoiadas por IA; que identificamos as fontes, licenças e partes preexistentes relevantes; que não apresentamos como autoria própria material de terceiros; e que não compartilhamos dados pessoais, credenciais ou informações restritas de forma indevida.

**Responsável pela submissão:** _A preencher_
