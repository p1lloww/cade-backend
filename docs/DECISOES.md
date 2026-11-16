# Decisões e Premissas — Cadê? Achados e Perdidos UCSal (POOALAB 2026.2)

Registro das decisões da equipe sobre stack, arquitetura e processo. Os requisitos ficam em [requisitos.md](requisitos.md) (v3, 10/11/2026); quando uma decisão daqui mudar algo dos requisitos, isso aparece no campo **Documento**.

Toda decisão nova entra aqui com a data. Se uma decisão mudar, não apague a antiga: marque como **substituída** e aponte a nova (ver "Histórico de mudanças" no fim).

Última atualização: 10/11/2026

---

## D01 — Back-end em Java 25 com Spring Boot
- **Origem:** o rascunho dos requisitos previa Kotlin + Spring Boot.
- **Decisão (10/11/2026):** Java 25 (LTS) com Spring Boot 4.1.
- **Motivo:** a disciplina é dada em Java e a equipe já conhece a linguagem; aprender Kotlin em 13 dias seria um risco. Java moderno já tem o que importa aqui (`record`, `sealed interface`, pattern matching, Virtual Threads), e os padrões de POOA ficam explícitos no código, o que ajuda no bônus.
- **Documento:** seção 7 dos requisitos (v3).
- **Situação:** fechada.

## D02 — Login por código enviado ao e-mail institucional, com JWT
- **Origem:** os requisitos v2 previam login com Google OAuth.
- **Decisão (10/11/2026):** sem senha e sem Google. O usuário informa o e-mail @ucsal.edu.br, recebe um código de uso único e troca o código por um JWT emitido pelo próprio back-end.
- **Motivo:** prova a mesma coisa que o Google (acesso à caixa institucional) sem depender de configuração no Google Cloud nem do risco de o administrador do Workspace da UCSal bloquear o app.
- **Consequência:** o envio de e-mail passa a ser P1, porque o login depende dele.
- **Documento:** RF01, seção 2 e seção 7 dos requisitos (v3).
- **Situação:** fechada.

## D03 — Regras do código de login
- **Decisão (10/11/2026):** código de 6 dígitos gerado com `SecureRandom`, válido por 10 minutos, de uso único e guardado só como hash; no máximo 5 tentativas por código e 1 envio por minuto por e-mail.
- **Motivo:** impedir tentativa e erro (1 milhão de combinações) e o uso do sistema para lotar a caixa de outra pessoa.
- **Documento:** RNF05 (v3).
- **Situação:** fechada.

## D04 — E-mail pelo Gmail (SMTP), com Mailpit em desenvolvimento
- **Decisão (10/11/2026):** envio real pelo SMTP do Gmail, com uma conta da equipe e senha de app em variável de ambiente. Em desenvolvimento, e na mostra se a internet falhar, os e-mails vão para o Mailpit (contêiner do Compose, interface em http://localhost:8025).
- **Verificação:** em 10/11/2026 o envio do Gmail para um endereço @ucsal.edu.br chegou na caixa de entrada, sem cair no spam.
- **Documento:** seção 7 e RNF06 (v3).
- **Situação:** fechada.

## D05 — Eventos do Spring no lugar do RabbitMQ
- **Origem:** os requisitos v2 previam RabbitMQ para o cruzamento e os e-mails.
- **Decisão (10/11/2026):** eventos do Spring consumidos depois do commit (`@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`), rodando em Virtual Threads, atrás da interface `EventPublisher`. O RabbitMQ fica opcional, se sobrar tempo depois do P1.
- **Motivo:** o cruzamento leva milissegundos; um broker separado para um monólito com um único consumidor seria excesso de engenharia. O padrão Observer continua aparecendo no código.
- **Limitação aceita:** os eventos ficam na memória; se o back-end cair entre o commit e o processamento, aquele evento se perde. Declarada no README.
- **Documento:** seções 7.1 e 7.2 dos requisitos (v3).
- **Situação:** fechada.

## D06 — Esquema do banco gerado pelo JPA, sem Flyway
- **Decisão (10/11/2026):** `spring.jpa.hibernate.ddl-auto=update`, sem migrations versionadas.
- **Motivo:** com 13 dias e entidades mudando todo dia, é o caminho mais rápido e evita conflito de numeração de migrations entre integrantes. Os dados de demonstração entram por código Java no profile `demo`.
- **Limitação aceita:** o `update` só acrescenta; para zerar o banco local, `docker compose down -v`. Migrations com Flyway ficam como próximo passo no README.
- **Situação:** fechada.

## D07 — Repositórios separados para back-end e front-end
- **Decisão (10/11/2026):** `p1lloww/cade-backend` e `p1lloww/cade-frontend`, públicos (o manual exige links que abram sem pedir permissão). O repositório do back-end guarda `docs/`, `demo/` e o `compose.yaml`, e o README dele é o README principal do projeto.
- **Situação:** fechada.

## D08 — Hospedagem para o teste do professor
- **Decisão (10/11/2026):** front-end publicado na Vercel; back-end, banco e Mailpit rodando no computador da equipe, com o back-end exposto por um túnel ngrok com domínio fixo.
- **Motivo:** o professor testa pelo navegador sem instalar nada, e a equipe não paga hospedagem.
- **Consequências:** ver D09. O computador precisa ficar ligado com o túnel ativo durante a janela de teste combinada (risco na seção 9.4 dos requisitos).
- **Pendente:** configurar o ngrok.
- **Situação:** fechada.

## D09 — Autenticação sem estado e CORS
- **Decisão (10/11/2026):** o JWT vai no header `Authorization`; não há cookie de sessão. O CORS libera só as origens do front (variável `CORS_ORIGINS`) e o header `ngrok-skip-browser-warning`. As fotos são buscadas pelo front com o token (HttpClient como blob), nunca por URL pública.
- **Motivo:** front (Vercel) e back (túnel) ficam em domínios diferentes; um cookie do back seria cookie de terceiros, bloqueado pelos navegadores.
- **Situação:** fechada.

## D10 — Fronteiras dos módulos do monólito modular
- **Origem:** seção 7.1 dos requisitos (monólito modular) e o modelo do SCEA (D16, D18 e D45 de lá).
- **Decisão (10/11/2026):** um pacote por módulo em `br.ucsal.cade`, cada um dono dos seus dados e acessível pelos outros só pelo pacote `api`:

  | Pacote | Módulo | Pode depender de |
  |---|---|---|
  | `access` | M1 Acesso | — |
  | `founditems` | M2 Achados | — |
  | `lossreports` | M3 Perdas | — |
  | `matching` | M4 Cruzamento | founditems, lossreports |
  | `pickup` | M5 Convocação e retirada | founditems, lossreports, matching |
  | `notifications` | M6 Avisos | access, lossreports, pickup |
  | `retention` | M7 Prazos e destino | founditems |
  | `publicpage` | M8 Página pública | founditems |
  | `administration` | M9 Administração | access |
  | `demo` | M10 Modo demonstração | access, founditems, lossreports |
  | `shared` | Núcleo compartilhado | nenhum módulo |

- **Camadas dentro do módulo:** `api` (interfaces, DTOs e eventos públicos), `controller`, `service` e `persistence`. As entidades JPA ficam em `persistence` com as regras sobre o próprio estado (ex.: transições de situação); o que depende de outros registros ou módulos fica no `service`. Sem Clean Architecture, pelo mesmo motivo da D45 do SCEA: o objetivo é separar módulos, e isolar o domínio do Spring dobraria o código para uma equipe de quatro com prazo curto.
- **Entre módulos:** referência só por id, sem chave estrangeira cruzando módulos. Para avisar que algo aconteceu, o módulo publica um evento (D05). Quando um módulo "de baixo" precisa de algo de um "de cima", a dependência é invertida: a interface fica na `api` de quem usa.
- **Histórico (RF43):** o contrato do evento fica em `shared.audit`; os módulos publicam o evento na mesma transação da ação e o módulo `administration` grava o registro.
- **E-mail:** o envio (`EmailSender`) fica em `shared.mail`, porque o login (access) também envia e-mail; o módulo `notifications` só decide quais e-mails os eventos disparam.
- **Verificação:** `ModuleBoundariesTest` (ArchUnit) recusa acesso fora da `api`, dependência não listada acima, ciclos entre módulos e camadas fora de ordem. Mudar a tabela exige registrar a mudança aqui.
- **Documento:** seção 7.1 dos requisitos (v3).
- **Situação:** fechada.

## D11 — Código do back-end em inglês, com glossário
- **Origem:** o modelo do SCEA (D20 de lá). Os requisitos citam nomes em português (`ArmazenamentoFotos`, `CriterioCompatibilidade`, `@RequerPerfil`).
- **Decisão (10/11/2026):** classes, métodos, pacotes e rotas da API em inglês. Mensagens ao usuário, documentação e Javadoc em português. A correspondência entre os termos dos requisitos e os nomes do código fica no [GLOSSARIO.md](GLOSSARIO.md).
- **Situação:** fechada. Se a equipe preferir nomes em português, registrar a mudança aqui antes do primeiro código de negócio.

## D12 — Proteção da main e fluxo de PR
- **Decisão (10/11/2026):** ruleset "Proteção da main" nos dois repositórios: todo código entra por Pull Request com 1 aprovação de outro integrante; force push e exclusão da `main` bloqueados. O papel admin (p1lloww) pode contornar a regra; mesmo assim, o padrão é branch + PR.
- **Próximo passo:** quando os pipelines existirem (QUA-01 e QUA-02), o check do CI passa a ser obrigatório no ruleset.
- **Complemento (16/11/2026):**
  - O check `build` do CI (QUA-01) passou a ser obrigatório no ruleset do back-end.
  - Os dois repositórios aceitam só **squash merge** (um commit por card na `main`) e apagam a branch ao mesclar.
  - **Não há branch `develop`:** todo PR vai direto para a `main`. A `develop` criada no ACE-01/ACE-02 deixava PRs entrarem sem revisão (ela não tinha proteção) e impedia o `Closes #N` de fechar as issues; foi removida.
- **Situação:** fechada.

## D13 — Organização do backlog
- **Decisão (10/11/2026), no modelo do SCEA (D17 e D32 de lá):**
  - Cada card tem código no título (`[ACE-02]`), contexto, requisitos, decisões, critérios de aceite e dependências ("Depende de").
  - Fluxo contínuo, sem sprints: a entrega é em 23/11/2026.
  - Cards nascem sem responsável; cada integrante se atribui ao puxar o card. 1 responsável por card e no máximo 2 cards "Em andamento" por pessoa.
  - Puxar primeiro os cards de P1 e do **caminho crítico**, olhando a view "Prontas para começar" (dependências concluídas).
  - Os campos "Dependências" e "Aguardando" são preenchidos pela Action "Dependências do Project" (GES-02); não editar à mão.
  - **Complemento (16/11/2026):** a mesma Action move para "Concluído" todo card cuja issue foi fechada, inclusive pelo `Closes #N` de um PR. Reabrir a issue não devolve o card: quem reabre move o card à mão.
- **Situação:** fechada.

## D14 — Problema mantém a evidência S6
- **Decisão (10/11/2026):** a formulação do problema continua dizendo que muitos estudantes não sabem que a sala existe (S6), como relatado pelos funcionários. O foco do MVP continua sendo a lista pesquisável e o cruzamento de perdas com objetos.
- **Situação:** fechada.

## D15 — Nome do projeto
- **Decisão (10/11/2026):** **Cadê?** Repositórios `cade-backend` e `cade-frontend`; pacote `br.ucsal.cade`.
- **Situação:** fechada.

## D16 — Códigos de login no Redis, não no banco (substituída pela D18)
- **Decisão (14/11/2026):** os códigos de login e o tempo de espera entre envios ficam no Redis (contêiner do Compose), com expiração própria; nada disso vai para o PostgreSQL. Por e-mail existem no máximo duas chaves: `cade:login-code:<email>` (o código atual, 10 min) e `cade:login-code-cooldown:<email>` (1 min). Depois de um login válido, as duas são apagadas.
- **Motivo:** são dados que perdem o valor assim que o código é usado ou expira. No Redis eles somem sozinhos, sem tabela crescendo nem rotina de limpeza.
- **Como as regras da D03 ficam garantidas:** o código novo sobrescreve o anterior (só o último vale); a tentativa é contada (`HINCRBY` num script Lua) antes da comparação, para palpites em paralelo não furarem o limite de 5; o código é apagado por um único `DEL`, e só a requisição que o apagou recebe o token (uso único).
- **Consequências:** mais um serviço no Compose e nos testes (Testcontainers). O `requestCode` não abre transação no banco, então o listener que envia o e-mail (AVI-01) precisa de `@TransactionalEventListener(fallbackExecution = true)`. Se o Redis cair, ninguém consegue pedir código; os usuários já logados continuam, porque o JWT não depende dele.
- **Detalhes:** chaves, fluxos e o motivo dos scripts Lua estavam em `login-codigos-redis.md`, hoje [login-codigos.md](login-codigos.md) (versão PostgreSQL).
- **Situação:** substituída pela D18 em 16/11/2026.

## D17 — Documentação da API no Swagger e erros no formato RFC 9457
- **Decisão (15/11/2026):** a API é documentada com o springdoc-openapi: o Swagger UI fica em `/swagger-ui.html` e o documento OpenAPI em `/v3/api-docs`, os dois públicos. A documentação de cada controller fica numa interface (`AuthApi`, por exemplo), com as anotações do OpenAPI; as rotas e a validação ficam no controller que a implementa. Todos os erros saem como `ProblemDetail` (RFC 9457, `application/problem+json`): `detail` traz a mensagem para o usuário e, nos erros de validação, `errors` traz a mensagem de cada campo.
- **Motivo:** o front-end consulta e testa os endpoints sem depender de quem fez o back-end, e trata todo erro do mesmo jeito. Separar a documentação na interface deixa o controller curto.
- **Consequências:** toda rota nova documenta respostas e exemplos de erro na interface do controller. Uma exceção de negócio nova estende `ApiException` e não precisa mexer no `ApiExceptionHandler`.
- **Complemento (16/11/2026):** os 401 e 403 do Spring Security e os erros inesperados (500) também saem como `ProblemDetail`. O 500 traz uma mensagem neutra; o detalhe técnico vai só para o log.
- **Situação:** fechada.

## D18 — Códigos de login no PostgreSQL e bloqueio contra força bruta (substitui a D16)
- **Decisão (16/11/2026):** os códigos de login saem do Redis e vão para a tabela `login_codes` do PostgreSQL, com uma linha por e-mail (o e-mail é a chave). O Redis sai do projeto. Além das regras da D03, um e-mail que erra 15 códigos seguidos, somando todos os códigos (o equivalente a 3 códigos travados), fica **bloqueado por 15 minutos**: não pode pedir código novo, e o código atual deixa de valer. Um login válido zera a contagem.
- **Motivo:** para um MVP, o Redis era um serviço a mais para subir no deploy (D08), para os testes e para explicar no pitch, e se ele caísse ninguém conseguia logar. A tabela não cresce (um código novo sobrescreve a linha) e o PostgreSQL garante as mesmas regras com comandos atômicos. O bloqueio reduz os palpites possíveis contra um e-mail de cerca de 7.200 para cerca de 1.300 por dia.
- **Como as regras ficam garantidas:**
  - código novo com `INSERT ... ON CONFLICT DO UPDATE ... WHERE` a espera passou e não há bloqueio;
  - tentativa contada com `UPDATE ... RETURNING` antes da comparação;
  - uso único com `DELETE` que confere o hash.
  - A hora vem sempre do `Clock` da aplicação, nunca do `now()` do banco, para os testes controlarem o tempo.
- **Consequências:**
  - Os métodos do `LoginService` não abrem transação, porque um rollback desfaria a tentativa contada.
  - Como o `requestCode` não tem transação, o listener que envia o e-mail (AVI-01) precisa de `@TransactionalEventListener(fallbackExecution = true)`.
  - Quem sabe o e-mail de alguém pode bloqueá-lo por 15 min errando de propósito; para o MVP, a proteção contra adivinhação vale mais.
- **Detalhes:** colunas, comandos, fluxos e testes em [login-codigos.md](login-codigos.md).
- **Situação:** fechada.

---

## Histórico de mudanças
- **10/11/2026 — D01 a D15 registradas:** stack, login, e-mail, eventos, banco, repositórios, hospedagem, módulos, idioma do código, fluxo de PR, backlog, problema e nome, decididos na reunião de definição do projeto.
- **14/11/2026 — D16 registrada:** códigos de login no Redis, na implementação do ACE-02.
- **15/11/2026 — D17 registrada:** Swagger com springdoc e erros no formato RFC 9457, na implementação do ACE-02.
- **16/11/2026 — Revisão do ACE-01/ACE-02:** D18 registrada e D16 marcada como substituída (Redis → PostgreSQL, bloqueio contra força bruta); complementos na D12 (check do CI obrigatório, só squash merge, sem `develop`), na D13 (a Action move para "Concluído" o card com a issue fechada) e na D17 (401, 403 e 500 também como `ProblemDetail`).
