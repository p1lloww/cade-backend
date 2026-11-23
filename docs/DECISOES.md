# Decisões e Premissas — Cadê? Achados e Perdidos UCSal (POOALAB 2026.2)

Registro das decisões da equipe sobre stack, arquitetura e processo. Os requisitos ficam em [requisitos.md](requisitos.md) (v3, 10/11/2026); quando uma decisão daqui mudar algo dos requisitos, isso aparece no campo **Documento**.

Toda decisão nova entra aqui com a data. Se uma decisão mudar, não apague a antiga: marque como **substituída** e aponte a nova (ver "Histórico de mudanças" no fim).

Última atualização: 18/11/2026

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
  | `shared` | Núcleo compartilhado (inclui categorias e fotos, D21) | nenhum módulo |

- **Camadas dentro do módulo:** `api` (interfaces, DTOs e eventos públicos), `controller`, `service` e `persistence`. As entidades JPA ficam em `persistence` com as regras sobre o próprio estado (ex.: transições de situação); o que depende de outros registros ou módulos fica no `service`. Sem Clean Architecture, pelo mesmo motivo da D45 do SCEA: o objetivo é separar módulos, e isolar o domínio do Spring dobraria o código para uma equipe de quatro com prazo curto.
- **Entre módulos:** referência só por id, sem chave estrangeira cruzando módulos. Para avisar que algo aconteceu, o módulo publica um evento (D05). Quando um módulo "de baixo" precisa de algo de um "de cima", a dependência é invertida: a interface fica na `api` de quem usa.
- **Histórico (RF43):** o contrato do evento fica em `shared.audit`; os módulos publicam o evento na mesma transação da ação e o módulo `administration` grava o registro.
  - **Complemento (18/11/2026, ADM-01):** a seção 7.1 dos requisitos diz que o histórico é gravado na mesma transação, "nunca por evento". Para cumprir as duas regras, o `AuditEvent` é ouvido de forma síncrona (`@EventListener`, sem `@Async` nem `AFTER_COMMIT`) pelo `AuditRecorder`, que exige transação aberta (`Propagation.MANDATORY`). Se a gravação falhar, a ação inteira é desfeita (RNF07); publicar o evento sem transação lança `IllegalTransactionStateException`. Ação do sistema (cruzamento em `@Async`, tarefa agendada) precisa abrir a própria `@Transactional` antes de publicar, com o autor `AuditEvent.SYSTEM_ACTOR`.
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
- **Detalhes:** chaves, fluxos e o motivo dos scripts Lua estavam em `login-codigos-redis.md`, hoje [login-codigos.md](tecnico/login-codigos.md) (versão PostgreSQL).
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
- **Detalhes:** colunas, comandos, fluxos e testes em [login-codigos.md](tecnico/login-codigos.md).
- **Situação:** fechada.

## D19 — Autorização por perfil com anotação própria e interceptor
- **Decisão (17/11/2026):** a `@RequiresRole` (o `@RequerPerfil` do RNF05) é lida por reflexão num `HandlerInterceptor` próprio, primeiro no método e depois na classe; a do método substitui a da classe. O administrador atende às rotas de funcionário (`ADMIN(STAFF)` no enum `Role`); ninguém herda as rotas de estudante. Toda rota fora de `PUBLIC_ROUTES` precisa da anotação, e um teste quebra o build quando falta. O Swagger mostra os perfis de cada rota a partir da mesma anotação.
- **Motivo:** com o `@PreAuthorize` do Spring, a regra viraria texto numa string e a leitura da anotação não seria nossa; a anotação própria fica tipada pelo enum e é conteúdo de POOA (anotações e reflexão). A hierarquia segue a seção 2 dos requisitos. O teste evita que uma rota de funcionário fique aberta a estudantes por esquecimento.
- **Consequências:** o perfil vem do token, sem consulta ao banco, então uma mudança de perfil (ADM-02) só vale no próximo login.
- **Detalhes:** fluxo, CORS, exemplos e checklist de rota nova em [autorizacao.md](tecnico/autorizacao.md).
- **Situação:** substituída pela D20 em 18/11/2026.

## D20 — `@RequiresRole` aplicada pelo `@PreAuthorize` do Spring (substitui a D19)
- **Decisão (18/11/2026):** a `@RequiresRole` continua sendo a anotação dos controllers, tipada pelo enum `Role`, mas passa a ser uma meta-anotação de `@PreAuthorize("hasAnyRole({value})")`. O Spring Security troca o `{value}` pelos perfis (o `Role` implementa `ExpressionTemplateValueProvider` e vira `'STAFF'`), então `@RequiresRole(Role.STAFF)` vira `hasAnyRole('STAFF')`. A hierarquia vai para um bean `RoleHierarchy` gerado a partir do enum (`ROLE_ADMIN > ROLE_STAFF`). O `RoleInterceptor` sai. Continuam valendo as regras da D19: o método substitui a classe, o administrador atende às rotas de funcionário, toda rota fora de `PUBLIC_ROUTES` precisa da anotação e o Swagger mostra os perfis a partir dela.
- **Motivo:** quem decide o acesso passa a ser o mecanismo padrão e testado do Spring, em vez de um interceptor nosso que reimplementava a checagem das authorities. Os controllers não mudam, e a regra continua tipada: a string SpEL é gerada, nunca escrita à mão. A leitura por reflexão continua no `RoleRequirement`, usada pelo Swagger e pelo teste de cobertura das rotas.
- **Consequências:**
  - O `@PreAuthorize` vale em qualquer bean do Spring, não só em controllers.
  - A negação vira `AuthorizationDeniedException`, que herda de `AccessDeniedException`; o 403 em português do `ApiExceptionHandler` não muda.
  - Um erro na expressão gerada só aparece em execução; o `RequiresRoleTest` cobre cada perfil contra cada exigência com requisições de verdade.
  - Igual à D19: o perfil vem do token, então uma mudança de perfil (ADM-02) só vale no próximo login.
- **Detalhes:** [autorizacao.md](tecnico/autorizacao.md).
- **Situação:** fechada.

## D21 — Categorias e fotos no núcleo compartilhado
- **Decisão (18/11/2026):** a `Category` (nome e risco padrão, o `RiskLevel`) fica em `shared.category`, e a interface `PhotoStorage` (o `ArmazenamentoFotos` da seção 7), com a implementação em disco local, fica em `shared.storage`. As categorias iniciais (as mais comuns da S8, com o risco da RN09) são criadas na subida pelo `DefaultCategoriesInitializer`, que só cria as que faltam. O objeto achado guarda o próprio risco: o padrão da categoria, que o funcionário pode aumentar mas nunca reduzir (RN04); documento continua documento. A tabela da D10 não muda: o `shared` continua sem depender de nenhum módulo.
- **Motivo:** objetos achados (ACH-02), perdas (PER-01) e o cruzamento (CRU-01) usam a mesma lista de categorias, e a D10 não deixa `founditems` e `lossreports` dependerem um do outro nem do `administration`. No núcleo, todos usam a mesma tabela, e a gerência pelo administrador (ADM-04) entra depois sem mudar nenhuma direção de dependência. O mesmo vale para as fotos: objetos (ACH-02) e provas de perda (PER-05).
- **Consequências:**
  - O `shared` passa a ter uma entidade com regra (o risco). É a exceção desta decisão; outra entidade no núcleo exige uma decisão nova.
  - As fotos ficam numa pasta (`PHOTOS_DIR`; no Docker, o volume `photos-data`), com nome novo (`<uuid>.<extensão>`), formato conferido pelos primeiros bytes (JPEG, PNG ou WebP) e no máximo 5 MB. Nunca têm URL pública: a API serve a foto só a quem o perfil permite (D09, RN03). Se o cadastro for desfeito, a foto gravada é apagada.
  - Nenhum card cobria a RN04; ela entrou no ACH-02.
- **Complemento (18/11/2026), mudança de risco depois do cadastro:** a RN04 é lida em relação à categoria, não ao risco atual do objeto. O funcionário pode mudar o risco de um objeto para cima ou para baixo, desde que nunca fique abaixo do risco padrão da categoria (`RiskLevel.canBeRaisedTo` continua sendo a regra); documento continua documento. Uma garrafa cadastrada como Alto pode voltar a Baixo; um celular nunca fica abaixo de Alto. Duas proteções valem para a mudança:
  - toda mudança de risco vai para o histórico com o valor anterior, o novo e um **motivo obrigatório** (RF43);
  - o risco **não muda com o objeto Reservado**, isto é, com um estudante convocado, para ninguém afrouxar a conferência no meio de uma entrega.

  **Motivo:** a RN04 existe para que um objeto nunca tenha conferência mais fraca do que a categoria exige, e isso continua garantido. Proibir qualquer redução deixaria um erro de digitação sem correção no dia a dia da sala; o histórico com motivo e o bloqueio durante a convocação cobrem o risco de alguém reduzir de má-fé. A edição fica no ACH-07.
- **Situação:** fechada.

## D22 — Detalhes da nota de compatibilidade que a RN07 não define
- **Decisão (20/11/2026):** a nota da RN07 segue estas regras nos pontos em que a tabela não é explícita:
  - **Período:** a duração do período conta como incerteza. Os dias usados na tabela de data são o maior entre a duração (fim − início) e os dias entre o fim e o achado; a tolerância de 1 dia vale antes do início. Uma data só tem duração zero, e o resultado é exatamente o da RN07. Exemplos: perda de 03/11 a 05/11 com o objeto achado em 04/11 ou 08/11 vale 40; perda de 01/11 a 10/11 vale no máximo 20; um período de 15 dias ou mais não pontua na data.
  - **Vários locais:** vale o local da perda que mais pontua. Sala não informada no mesmo bloco conta como mesmo bloco (15). Bloco e sala são comparados sem diferença de maiúsculas, acentos e espaços extras.
  - **Palavras em comum:** contam as palavras distintas com 3 letras ou mais, sem maiúsculas nem acento, tiradas as palavras vazias ("com", "para", "cor"...) e as palavras do nome da categoria, que os dois lados já têm em comum. Do lado do objeto, a descrição comparada é a descrição interna; objeto sem descrição não pontua em palavras.
  - Os pesos, os limites de dias, o mínimo de letras e as faixas ficam em `cade.matching.*` no `application.yml` (RF21).
- **Motivo:** o exemplo da RN07 ("fone JBL preto" × "fone preto" = 80) só fecha se "fone", o nome da categoria, não contar. Contar o período a partir do melhor dia premiaria quem informa um período longo e vago, a mesma "pesca" que a RN05 tenta impedir; tratar a duração como incerteza não prejudica quem informa um período curto ("segunda a quarta") e tira os pontos de data de quem não sabe quando perdeu.
- **Consequências:**
  - Uma perda vaga (período longo, sem local, descrição genérica) fica na faixa baixa ou média, nunca no topo da lista do funcionário.
  - "421" e "Sala 421" são salas diferentes para o cruzamento; se isso aparecer na prática, a normalização de sala entra como ajuste desta decisão.
  - Não há radical nem sinônimo: "preto" e "preta" são palavras diferentes. A IA do CRU-05 é o lugar para esse tipo de semelhança.
- **Detalhes:** a seção "Strategy + Composite" de [pooa-no-projeto.md](entrega/pooa-no-projeto.md) e os testes de `matching.service.score`.
- **Situação:** fechada.

## D23 — Cruzamento por evento: quem entra e como a sugestão é gravada
- **Origem:** o RF20 diz que o cruzamento compara com "os registros abertos do outro lado", e a seção 6.3 diz que só compatibilidade média ou alta vira sugestão. Faltava definir quais situações contam como abertas, o que acontece com um par repetido e com uma falha.
- **Decisão (21/11/2026):**
  - **Quem entra:** objetos Disponível ou Não reclamado (os que ainda podem ser convocados, seção 6.1) e registros de perda Aberto ou Aguardando informações (o estudante ainda procura e ninguém foi convocado por ele). Só a mesma categoria é consultada, porque categoria diferente zera a nota (RN07).
  - **O que é gravado:** a `MatchSuggestion` (situação Nova) com a nota, para os pares de faixa média ou alta. A faixa não é gravada: sai da nota pelas faixas de `cade.matching.bands`. Cada sugestão criada vai para o histórico como `MATCH_SUGGESTED`, com o autor `AuditEvent.SYSTEM_ACTOR`.
  - **Par repetido:** cada par objeto × perda tem no máximo uma sugestão (chave única), e a gravação é um `INSERT ... ON CONFLICT DO NOTHING`. Um objeto e uma perda cadastrados no mesmo instante disparam dois cruzamentos que podem achar o mesmo par; o segundo não grava nada e não falha.
  - **Falha:** o ouvinte registra o erro no log e não tenta de novo. O cadastro já foi confirmado e não é afetado (RNF08); as sugestões daquela rodada se perdem, como na limitação da D05.
  - **Interfaces:** o cruzamento lê os dois lados pelas interfaces `MatchableFoundItems` e `MatchableLossReports`, na `api` de cada módulo, que devolvem só o que a nota usa (sem foto, detalhe reservado, detalhe secreto nem estudante). Os serviços publicam eventos pela `EventPublisher` (`shared.event`), e não direto pelo Spring.
- **Motivo:** o cruzamento só roda quando alguém cadastra; uma sugestão para um objeto que não pode mais ser convocado ou para uma perda que não está mais em busca só ocuparia a lista do funcionário. Guardar a nota, e não a faixa, deixa o ajuste das faixas valer para as sugestões que já existem.
- **Consequências:**
  - Um registro que volta de Convocado para Aberto não é cruzado de novo com os objetos cadastrados enquanto estava convocado; as sugestões antigas dele continuam valendo.
  - Uma sugestão baixa nunca é gravada: baixar a faixa média depois não traz de volta pares que já foram calculados.
  - Ainda não há recálculo quando o objeto ou a perda mudam (edição, novo local); se entrar, ele usa o mesmo `MatchingService`.
- **Detalhes:** [`docs/tecnico/cruzamento.md`](tecnico/cruzamento.md).
- **Situação:** fechada.

## D24 — Mudança de perfil vale no próximo login
- **Origem:** o ADM-02 dá e retira o perfil de funcionário (RF03), mas o perfil viaja dentro do JWT, que não tem estado (D03, D09).
- **Decisão (21/11/2026):** a mudança de perfil é gravada na conta na hora, mas o token que a pessoa já tem continua com o perfil antigo até vencer (no máximo 8 h, `cade.jwt.ttl`). O perfil novo vale a partir do próximo login.
- **Motivo:** conferir o perfil no banco a cada requisição tiraria a vantagem do token sem estado e exigiria mudar a validação do token, perto da entrega.
- **Consequências:**
  - Quem perde o perfil de funcionário ainda pode usar as rotas de funcionário por até 8 h. Para o MVP isso é aceito.
  - Quem ganha o perfil precisa sair e entrar de novo para ver as telas de funcionário.
  - O Swagger da rota `PUT /users/{id}/role` e o [`autorizacao.md`](tecnico/autorizacao.md) avisam isso ao front.
  - Se precisar valer na hora, o caminho é conferir o perfil no banco ao validar o token, ou encurtar a validade do token.
- **Situação:** fechada.

## D25 — Convocação: ordem das mudanças, sugestões em espera e o que o estudante recebe
- **Origem:** o RET-01 junta numa transação objeto, perda e sugestão (seção 7.1) e exige uma convocação ativa por objeto (RN18) com lock otimista. Os requisitos não definiam em que ordem as mudanças são gravadas, o que acontece com as outras sugestões da mesma perda nem o texto de "o que levar" (RF26).
- **Decisão (21/11/2026):**
  - **Módulos:** a convocação (`Summons`) fica no `pickup`, que muda as entidades dos outros módulos pelas interfaces `FoundItemReservations`, `LossReportSummons` e `SuggestionSummons`, na `api` de cada um. Cada módulo muda a própria entidade e grava o próprio histórico; as implementações exigem a transação da convocação (`Propagation.MANDATORY`).
  - **Lock:** o `FoundItem` ganha `@Version`. O objeto é reservado e gravado (flush) antes de qualquer sugestão mudar, para que convocações simultâneas do mesmo objeto disputem sempre a mesma linha primeiro: a segunda perde no `@Version` (409) em vez de travar em deadlock nas sugestões.
  - **Em espera:** só as outras sugestões **Novas do mesmo objeto** vão para Em espera (RN18). As outras sugestões da mesma perda, com outros objetos, continuam Novas; convocar uma delas é recusado enquanto a perda está Convocada, porque a seção 6.2 só convoca a partir de Aberto.
  - **O que o estudante recebe:** local e horário da sala vêm de `cade.pickup.place` e `cade.pickup.hours`, e o prazo de `cade.pickup.deadline` (7 dias, RN15). O "o que levar" depende do risco gravado na convocação: sempre documento com foto (RN10), mais o desbloqueio ou a prova para risco alto e o aviso de entrega só ao titular para documentos (RN09). O texto não conta o que o funcionário vai conferir (RN03).
- **Motivo:** o lock otimista pedido no card só funciona se houver uma linha que toda convocação do mesmo objeto atualiza; a do objeto já muda de situação, então basta a versão. Gravar o objeto primeiro dá uma ordem fixa de travas.
- **Consequências:**
  - Um banco local criado antes do RET-01 não ganha a coluna `version` em `found_items` (o `ddl-auto: update` não consegue criar coluna `NOT NULL` numa tabela com linhas); é preciso `docker compose down -v`.
  - Uma sugestão criada pelo cruzamento no mesmo instante da convocação pode nascer Nova para um objeto já Reservado. Convocá-la é recusado pela situação do objeto, e ela volta a valer se a convocação vencer.
  - O que levar por risco é um `switch` simples (`WhatToBring`); as regras de conferência no balcão por risco (RET-03) podem absorvê-lo. *(Absorvido no RET-03: o texto vem de `VerificationRule.whatToBring`, D26.)*
- **Detalhes:** [`docs/tecnico/convocacao.md`](tecnico/convocacao.md).
- **Situação:** fechada.

## D26 — Balcão: regras por risco, onde contar as recusas e o que fica fora do RET-03
- **Origem:** o RET-03 pede conferência por risco como estratégias, registro da retirada e da recusa e bloqueio após 3 recusas (RN09 a RN12). Os requisitos não definiam:
  - quais verificações bastam para cada risco;
  - o que a retirada guarda do documento;
  - onde ficam as recusas;
  - como desbloquear o estudante.
- **Decisão (21/11/2026):**
  - **Regras:** uma `VerificationRule` por risco (`pickup.service.verification`), com o checklist do funcionário, as verificações que bastam (`VerificationMethod`) e o que o estudante leva. A verificação registrada é conferida contra a regra antes de qualquer mudança (400 se não basta). No risco baixo, um detalhe que não aparece na foto também vale, por ser mais forte que a descrição. No alto, vale o desbloqueio ou a prova (nota fiscal, número de série).
  - **Retirada:** a `Pickup` guarda o tipo e o número do documento com foto, a matrícula, a verificação, uma observação opcional, o funcionário, a data e a hora, como o formulário de papel de hoje (RF29). Ela nunca muda depois de gravada.
  - **Ordem e lock:** a `Summons` (com `@Version`) é gravada primeiro. Depois vêm objeto, sugestão e perda, pelas interfaces da `api` de cada módulo (`MANDATORY`, como na D25). Retirada, recusa e o vencimento do RET-02 disputam a mesma linha, e só uma decisão passa.
  - **Recusas:** contadas em `LossReporter.refusals`, a linha por estudante que já existe para a RN05. O `lossreports` não pode depender do `pickup` (D10), e o bloqueio impede justamente abrir registros de perda. Com 3 recusas, o registro de perda responde 409.
  - **Recusa e sugestões:** a convocada passa a Recusada, e as Em espera do mesmo objeto voltam a Nova (seção 6.3).
  - **Perda concluída:** na retirada, as outras sugestões Novas ou Em espera da perda (com outros objetos) também passam a Encerrada, porque a perda não pode mais ser convocada (complemento de 21/11/2026, na revisão do RET-03).
  - **A perda recusada se encerra:** a recusa leva o registro de perda a **Recusado**, uma situação final nova, em vez de voltar a Aberto como a seção 6.2 previa. As outras sugestões Novas ou Em espera dessa perda passam a Encerrada. Se o estudante ainda procura o objeto, registra a perda de novo. O vencimento da convocação (RET-02) continua devolvendo a perda a Aberto, porque nele ninguém viu o objeto.
- **Motivo:**
  - Guardar as recusas no `lossreports` deixa a regra do bloqueio junto do registro que ela barra, sem dependência nova entre módulos, e aproveita a trava que já protege o limite de 5 registros.
  - Encerrar a perda recusada evita que ela volte a ser editável (`isEditable` vale para Aberto). Sem isso, quem foi recusado poderia ajustar a descrição ao que viu no balcão, o que a RN06 proíbe. Decidido pela equipe na revisão do RET-03.
- **Consequências:**
  - **O desbloqueio pelo administrador não faz parte do RET-03.** Ele virou o card [#75](https://github.com/p1lloww/cade-backend/issues/75) (ADM-07, P2). Até entrar, o bloqueio só sai pelo banco (`UPDATE loss_reporters SET refusals = 0`). *(Entrou no ADM-07, em 21/11/2026: o administrador vê os bloqueados em `GET /blocked-students` e libera com motivo em `POST /blocked-students/{studentId}/release`; as recusas voltam a zero e a liberação vai para o histórico como `STUDENT_RELEASED`. Detalhes em [`docs/tecnico/retirada.md`](tecnico/retirada.md), seção 4.1.)*
  - Encerrar as outras sugestões do objeto e avisar os estudantes (RF33, RET-06), o e-mail de confirmação (AVI-02) e o atalho de balcão sem convocação (RET-04) também ficam fora. O `PickupRegisteredEvent` (`retirada.registrada`) fica pronto para eles.
  - As perguntas de conferência por categoria (RF44, ADM-04) ainda não existem. Quando entrarem, somam-se ao checklist da regra.
  - A seção 6.2 dos requisitos ganha, na prática, a transição Convocado → Recusado. A seção 6.2 não foi reescrita: vale esta decisão.
  - Uma perda cuja convocação venceu (RET-02) volta a Aberto e fica editável de novo. O PER-07 precisa decidir se a RN06 também trava esse caso.
  - Um banco local já existente ganha a coluna `refusals` sozinho (há valor padrão), mas `summons` ganha três colunas que aceitam nulo; não é preciso zerar o banco.
- **Detalhes:** [`docs/tecnico/retirada.md`](tecnico/retirada.md).
- **Situação:** fechada.

## D27 — Busca de objetos: índice full-text criado na subida e filtros escolhidos de uma lista
- **Origem:** o ACH-04 pede busca por texto, categoria, período, local e situação, em até 2 s com 1.000 objetos (RF10, RNF02). O `ddl-auto` (D06) não cria índice GIN.
- **Decisão (21/11/2026):**
  - **Texto:** full-text do PostgreSQL num índice GIN sobre `to_tsvector('cade_pt', descrição interna + detalhes reservados)`. A `cade_pt` é uma cópia da configuração `portuguese` com a extensão `unaccent` antes do `portuguese_stem`: ignora acentos ("termica" acha "térmica") e reduz plural e variações ("chaves" acha "chave"). A consulta usa `websearch_to_tsquery` e repete a mesma expressão, senão o índice não é usado.
  - **Criação na subida, sem Flyway:** a equipe decidiu não usar Flyway (D06), e o `ddl-auto` não cria extensão, configuração de busca nem índice GIN. Por isso o `FoundItemTextIndex` (um `ApplicationRunner`) cria os três a cada subida, depois das tabelas, só se ainda não existem (`IF NOT EXISTS` e um bloco `DO` para a configuração). É um contorno assumido: o esquema fica parte no Hibernate e parte nesse inicializador.
  - **Filtros:** só o texto (`q`) é digitado. Categoria, mês, bloco, sala e situação vêm de `GET /found-items/search-options`, que lista só o que tem objeto, e o front manda o valor escolhido. Não há validação além do tipo: formato inválido responde 400, e um valor que não existe só devolve a lista vazia.
  - **Período por mês** (`month=2026-11`), como a caixa de guarda, em vez de um intervalo de datas.
  - **Consulta:** uma query nativa só, com os filtros opcionais, que devolve a página de ids. Depois os objetos vêm com a categoria, na mesma ordem: com texto, por relevância (`ts_rank`); sem texto, os mais recentes. Sem situação, valem todas. No máximo 100 por página.
- **Motivo:** a equipe preferiu a implementação mais direta. Um índice full-text atende a busca combinada com filtros sem acrescentar Specification nem funções registradas no Hibernate. A criação na subida é a consequência de seguir sem Flyway, decisão discutida e mantida pela equipe.
- **Consequências:**
  - A extensão `unaccent` exige um usuário com permissão para criar extensões. O `cade` do `compose.yaml` e o do Testcontainers são superusuários; num banco gerenciado, a extensão precisa ser criada antes por quem administra o banco.
  - A busca depende do inicializador ter rodado. Ele roda antes de a aplicação aceitar requisições, então isso só importa se alguém apagar a configuração ou o índice com a aplicação no ar.
  - Se a equipe adotar Flyway depois, os três comandos do `FoundItemTextIndex` viram a primeira migração, e o inicializador sai.
  - O texto não procura no nome da categoria; para isso existe o filtro de categoria.
  - Com o índice criado na subida, um banco local já existente o ganha sozinho, sem `docker compose down -v`.
- **Detalhes:** `FoundItemRepository` (consulta e expressão), `FoundItemTextIndex` (índice) e `FoundItemSearchServiceTest` (filtros, `EXPLAIN` provando o uso do índice e RNF02 com 1.000 objetos).
- **Situação:** fechada.

## D28 — Meus registros: o prazo da convocação vem do módulo de retirada, numa rota própria
- **Origem:** o PER-03 pede que o estudante acompanhe os registros e a situação de cada um em linguagem simples (RF16). No registro Convocado, a informação útil é onde e até quando retirar, mas o prazo (`Summons.expiresAt`) e o que levar ficam na convocação, no `pickup`, e o `lossreports` não pode depender do `pickup` (D10).
- **Decisão (21/11/2026):**
  - **Duas rotas do estudante:** `GET /loss-reports/mine` (`lossreports`) lista todos os registros do estudante, do mais recente para o mais antigo, e `GET /summons/mine` (`pickup`) lista só as convocações Ativas dele, com `lossReportId`, local, horário, prazo e o que levar (o mesmo `PickupInstructions` da convocação). O front junta as duas pelo `lossReportId`.
  - **Linguagem simples:** cada situação do registro ganha um texto para o estudante (`LossReportStatus.description()`), devolvido em `statusDescription`. O texto do Convocado remete à convocação, sem data.
  - **Sem paginação:** cada estudante tem no máximo 5 registros em andamento (RN05), e os encerrados são poucos no semestre.
  - **Nada do objeto achado** em nenhuma das duas respostas, nem o id do objeto na convocação (RN03).
- **Motivo:** cada módulo responde pelo que é seu e o prazo existe num lugar só. Copiar o prazo no registro de perda obrigaria o vencimento (RET-02), a retirada e a recusa a manter a cópia em dia, e uma divergência viraria um prazo errado na tela sem erro nenhum.
- **Consequências:**
  - O front faz duas chamadas na tela "meus registros". Elas são independentes e podem ir em paralelo.
  - Quando a convocação fecha (retirada, recusa ou vencimento), ela sai de `GET /summons/mine` sem nenhuma mudança extra, e a situação do registro já conta o que houve.
  - Os e-mails da convocação (AVI-02) podem reaproveitar o mesmo `PickupInstructions`, e a tela e o e-mail mostram sempre os mesmos dados.
- **Detalhes:** [`docs/tecnico/meus-registros.md`](tecnico/meus-registros.md).
- **Situação:** fechada.

## D29 — Atalho de balcão: entidade própria, objeto Reservado de fora e o registro de perda intocado
- **Origem:** o RET-04 pede a retirada de quem veio direto à sala, sem registro de perda, inclusive pessoas sem conta (RF32, RN20), com as outras sugestões do objeto encerradas. Os requisitos não definiam onde gravar essa retirada, o que fazer com um objeto Reservado, como identificar o estudante nem o que acontece com um registro de perda que ele tenha.
- **Decisão (21/11/2026):**
  - **Entidade própria:** a retirada pelo atalho é uma `WalkInPickup` (tabela `walk_in_pickups`), separada da `Pickup`. A `Pickup` exige convocação e estudante (`NOT NULL`); afrouxar isso quebraria os bancos locais (o `ddl-auto: update` não remove `NOT NULL`) e misturaria dois casos com regras diferentes. As duas usam o mesmo `CounterCheck`, com a matrícula opcional no atalho.
  - **Quem retira:** `HolderType` é `STUDENT` ou `VISITOR` (sem conta), sempre com nome e documento com foto (RN10). Estudante também informa a matrícula; a de quem não tem conta é descartada. O estudante **não é ligado à conta**, porque o `pickup` não pode depender do `access` (D10); ficam o nome e a matrícula, como no formulário de papel.
  - **Objeto Reservado não sai pelo atalho** (409): ele está prometido a um estudante convocado (RN18). A transição nova `FoundItemStatus.handOverWithoutSummons()` aceita só Disponível e Não reclamado.
  - **Lock:** o objeto é gravado primeiro, com o `@Version` que já existe (D25). Atalho e convocação simultâneos do mesmo objeto disputam a mesma linha, e o segundo perde (409).
  - **Verificação:** as mesmas regras por risco da retirada com convocação (D26), pelo risco do objeto. O funcionário vê o que conferir em `GET /found-items/{id}/walk-in-check`.
  - **Sugestões:** as Novas e Em espera do objeto passam a Encerrada, com histórico (é o RF33 neste caso).
  - **O registro de perda não muda:** se quem retirou tinha um registro para esse objeto, ele continua Aberto, porque o atalho não sabe qual registro é. A sugestão dele é encerrada com as outras. Ao ver uma sugestão para o objeto, o funcionário deve preferir o fluxo da convocação.
- **Motivo:** o atalho reaproveita as regras do balcão e a trava do objeto sem acrescentar dependência entre módulos, e cada tipo de retirada guarda só o que tem.
- **Consequências:**
  - **Aviso aos estudantes das sugestões encerradas** (a parte do "avisar" do RF33) fica para o AVI-02. O `WalkInPickupRegisteredEvent` já leva as sugestões encerradas. A equipe sugeriu que o aviso seja por e-mail, e que o estudante cujo registro continua Aberto receba também o e-mail de quando o registro estiver perto de se encerrar (vencimento em 30 dias, RF35).
  - Um registro de perda que continua Aberto depois do atalho ocupa uma das 5 vagas do estudante (RN05) até ele cancelar (PER-06) ou o registro vencer.
  - Um banco local já existente ganha a tabela `walk_in_pickups` sozinho, sem `docker compose down -v`.
- **Detalhes:** [`docs/tecnico/retirada.md`](tecnico/retirada.md), seção 9.
- **Situação:** fechada.

## D30 — Avisos por e-mail com Transactional Outbox
- **Origem:** o AVI-02 pede e-mails na convocação, 1 dia antes de a convocação vencer e na retirada (RF34), antes de o registro vencer e no pedido de informações (RF35), com nova tentativa em falha e sem bloquear a ação (RNF08). Gravar a convocação (PostgreSQL) e mandar o e-mail (SMTP) são dois sistemas sem transação comum. Enviar depois do commit, direto, perde o e-mail se o SMTP estiver fora ou se a aplicação cair entre o commit e o envio.
- **Decisão (21/11/2026):**
  - **Transactional Outbox:** todo aviso ao estudante é gravado na tabela `notifications` **na mesma transação da ação** (`Outbox.enqueue`, `MANDATORY`), por um ouvinte síncrono (`@EventListener`, como o histórico). O aviso existe se e somente se a ação foi gravada.
  - **Relay em duas portas:** logo após o commit, `OutboxRelay.deliverSoon` tenta enviar numa Virtual Thread; a cada minuto, `relayPending` reenvia os Pendentes cuja espera passou. Cada tentativa é uma transação própria (`NotificationDelivery`).
  - **Um aviso, um envio:** o par (tipo, referência) é único, gravado com `INSERT ... ON CONFLICT DO NOTHING`; a tentativa trava a linha com `SELECT ... FOR UPDATE SKIP LOCKED`, e quem chega depois desiste.
  - **Nova tentativa:** até 5, com espera que cresce (5, 10, 15, 20 min); depois, o aviso fica Abandonado na tabela. Conta inexistente é abandonada na hora. Valores em `cade.notifications.*`.
  - **A linha guarda o texto, não o endereço:** o texto é montado do evento, dentro da transação; o e-mail é buscado no `access` (`UserEmails`, interface nova na `api`) na hora do envio, para nada que dependa de outro módulo rodar na transação da ação. O texto nunca leva segredo.
  - **O código de login continua direto** (`LoginCodeEmailListener`, `AFTER_COMMIT` + `@Async`): vale 10 minutos e não deve ficar gravado no banco.
  - **Lembretes de prazo:** `DeadlineNoticeJob`, de hora em hora, põe na caixa a convocação que vence em menos de 1 dia. A unicidade da caixa garante um lembrete só, sem coluna "já avisado".
  - **`EmailSender.send` passa a devolver `boolean`**, para o relay saber se precisa tentar de novo. Continua sem lançar exceção.
  - **`@EnableScheduling`** fica em `shared.time.SchedulingConfiguration`, para os avisos e para os vencimentos (RET-02, PRA-01).
- **Motivo:** é o jeito de ter "aviso gravado ⇔ ação gravada" sem transação distribuída nem fila externa (RabbitMQ, como previa a seção de stack, ficou opcional). A própria transação do PostgreSQL, que já garante a ação, garante o aviso.
- **Consequências:**
  - **Lembrete de registro perto de vencer (RF35):** pronto (`LossReportDeadlines`, `LossReportExpiringNotice`), mas desligado (`cade.notifications.deadlines.loss-report-notice-enabled: false`) até o PER-06, porque hoje o registro ainda não vence. Quando o PER-06 permitir **renovar**, a unicidade (tipo + registro) impediria um segundo lembrete do mesmo registro; o PER-06 precisa incluir o vencimento na referência ou apagar o aviso ao renovar.
  - **Pedido de mais informações (RF35):** fica para o PER-06, que cria o pedido e o evento. **Convocação vencida:** fica para o RET-02.
  - **Sem aviso das sugestões encerradas pelo atalho (D29):** o estudante nunca vê as sugestões, e o aviso revelaria que apareceu um objeto parecido com o dele (RN03). Decidido pela equipe; quem ainda procura é lembrado pelo aviso de registro perto de vencer, quando ligado.
  - Um erro no `INSERT` do aviso desfaz a ação, como um erro no histórico. É o preço de "aviso gravado ⇔ ação gravada", e o `INSERT` não depende de rede.
  - Um banco local já existente ganha a tabela `notifications` sozinho, sem `docker compose down -v`.
- **Detalhes:** [`docs/tecnico/avisos.md`](tecnico/avisos.md).
- **Situação:** fechada.

## D38 — Não reclamados: tarefa por objeto, porta em achados e destino em tabela própria
- **Origem:** o PRA-01 (RF37, RF38, RF39) pede a tarefa dos 90 dias, o destino do objeto e a lista por caixa.
- **Decisão (23/11/2026):**
  - **O prazo conta da entrada na sala** (`registeredAt`, RN16), não da data em que o objeto foi achado. Fica em `cade.retention.storage-period` (90 dias), e a tarefa roda de hora em hora (`cade.retention.interval`), pelo mesmo motivo das outras: uma rodada perdida é coberta pela seguinte.
  - **Só o Disponível vence.** Reservado fica como está (RN18): a convocação decide; se ela vencer, o objeto volta a Disponível e a próxima rodada o vê.
  - **Uma transação por objeto**, na porta `UnclaimedFoundItems` (`founditems.api`): uma falha não desfaz as outras. A mudança é gravada na hora, com o `@Version`, para perder a corrida contra uma convocação. O histórico leva `SYSTEM_ACTOR`.
  - **O destino é uma entidade do `retention`** (`Disposal`: objeto, destino, observação, data e administrador), sem chave estrangeira para o objeto (D10). Sem destino no corpo, vale o bazar; "outro" exige dizer qual. A mudança do objeto para Destinado e a gravação do destino acontecem na mesma transação; um objeto só é destinado uma vez.
  - **Rotas:** `GET /unclaimed-items` (funcionário; agrupado pela `storageBox`, a caixa do mês) e `POST /unclaimed-items/{id}/disposal` (administrador).
- **Motivo:** o `retention` só pode depender de `founditems` (D10), e uma porta pequena mantém a regra de situação onde ela já mora.
- **Situação:** fechada.

## D39 — Categorias gerenciáveis e prazos configuráveis lidos na hora
- **Origem:** o ADM-04 (RF44, RF45, RNF11) pede categorias com risco e perguntas de conferência e prazos ajustáveis sem mudar código.
- **Decisão (23/11/2026):**
  - **Categorias:** o administrador cria (`POST /categories`) e altera (`PUT /categories/{id}`) nome, risco padrão e até 10 perguntas de conferência; a lista continua em `GET /categories`, agora com as perguntas. **Não há exclusão**: objetos e perdas já registrados apontam para a categoria. O nome é único sem diferenciar maiúsculas (409). O risco novo vale para os próximos objetos: os que já estão na sala guardam o que tinham (RN04). As perguntas **complementam** as regras por risco do balcão (D26), não as substituem.
  - **Prazos:** uma tabela de uma linha (`deadline_settings`) em `shared.settings`, lida na hora por `Deadlines`, sem cache, para a mudança valer logo. Sem a linha, valem os valores iniciais (90, 7 e 30 dias). O administrador grava os três juntos em `PUT /settings/deadlines`, dentro de limites (guarda 30 a 365, retirada 1 a 30, validade 7 a 90 dias) para um erro de digitação não vencer tudo na hora. Cada mudança vai para o histórico com o que mudou.
  - **Mudança vale para o que vem depois:** a convocação e o registro já criados guardam o prazo com que nasceram. Por isso `cade.pickup.deadline` saiu do `application.yml`: o prazo da convocação vem só de `Deadlines`.
  - **Onde ficam as leituras:** `Deadlines` fica em `shared` (e não em `administration`) porque achados, perdas e retirada leem os mesmos prazos e não podem depender de outro módulo (D10); só `administration` grava.
  - **Guarda:** o valor fica gravado e exposto por `Deadlines.storagePeriod()`. A tarefa de não reclamados (PRA-01, D38) passa a lê-lo quando as duas entregas estiverem na `main`: troca `RetentionProperties.storagePeriod` por essa leitura.
- **Motivo:** configurar sem reiniciar nem redeployar, com o mínimo de peças novas e sem abrir dependência entre módulos.
- **Situação:** fechada.

## D41 — Painel do administrador: o `administration` passa a depender de `founditems`
- **Origem:** o ADM-06 (RF46) pede contagens de objetos por situação e categoria, devoluções no mês, tempo médio até a devolução e não reclamados. A tabela da D10 só deixava o `administration` depender de `access`.
- **Decisão (23/11/2026):**
  - **Complemento à D10:** `administration` pode depender de `founditems`, **só pela `api`**. A tabela do `CLAUDE.md`, do `README.md` e o `ModuleBoundariesTest` mudam no mesmo PR. As outras direções continuam como estavam.
  - **Uma porta de leitura**, `FoundItemOverview` (`founditems.api`), devolve as contagens por situação e categoria. Nenhum dado de objeto (foto, descrição, local) sai por ela, nem por rota: o painel só mostra números.
  - **Devoluções e tempo médio vêm do histórico (RF43)**, que é do próprio `administration` e já guarda a hora de cada devolução (`FOUND_ITEM_HANDED_OVER` e `FOUND_ITEM_HANDED_OVER_AT_COUNTER`) e do registro do objeto: não foi preciso uma coluna nova no objeto. O mês é o corrente no fuso de Salvador, e a média conta todas as devoluções que têm o registro no histórico (nula se não houve nenhuma).
  - **Collections e Streams** montam as contagens (`groupingBy`, `summingLong`, `average`), como pede o conteúdo de POOA do card.
- **Limite conhecido:** o histórico não tem as devoluções feitas antes de o ADM-01 existir, e uma ação nova com outro nome não entra na conta até ser listada em `DashboardService.RETURN_ACTIONS`.
- **Motivo:** o menor acoplamento que entrega o painel: uma porta de leitura e a fonte de verdade das datas que já existe.
- **Situação:** fechada.

---

## Histórico de mudanças
- **10/11/2026 — D01 a D15 registradas:** stack, login, e-mail, eventos, banco, repositórios, hospedagem, módulos, idioma do código, fluxo de PR, backlog, problema e nome, decididos na reunião de definição do projeto.
- **14/11/2026 — D16 registrada:** códigos de login no Redis, na implementação do ACE-02.
- **15/11/2026 — D17 registrada:** Swagger com springdoc e erros no formato RFC 9457, na implementação do ACE-02.
- **16/11/2026 — Revisão do ACE-01/ACE-02:** D18 registrada e D16 marcada como substituída (Redis → PostgreSQL, bloqueio contra força bruta); complementos na D12 (check do CI obrigatório, só squash merge, sem `develop`), na D13 (a Action move para "Concluído" o card com a issue fechada) e na D17 (401, 403 e 500 também como `ProblemDetail`).
- **17/11/2026 — D19 registrada:** autorização por perfil com `@RequiresRole` e interceptor, na implementação do ACE-03.
- **18/11/2026 — D20 registrada e D19 marcada como substituída:** a `@RequiresRole` passa a ser aplicada pelo `@PreAuthorize` do Spring Security, e o `RoleInterceptor` sai, na revisão do ACE-03.
- **18/11/2026 — Complemento na D10:** histórico gravado por evento síncrono na mesma transação (`MANDATORY`), na implementação do ADM-01.
- **18/11/2026 — D21 registrada:** categorias e fotos no núcleo compartilhado e risco do objeto (RN04), na implementação do ACH-02.
- **18/11/2026 — Complemento na D21:** o risco de um objeto pode subir e descer, nunca abaixo do risco da categoria; a mudança vai para o histórico com motivo e é proibida com o objeto Reservado (ACH-07).
- **20/11/2026 — D22 registrada:** detalhes da nota de compatibilidade (período, vários locais, palavras que contam), na implementação do CRU-01.
- **21/11/2026 — D23 registrada:** quem entra no cruzamento, sugestão única por par e falha sem efeito no cadastro, na implementação do CRU-02.
- **21/11/2026 — D24 registrada:** a mudança de perfil de funcionário vale no próximo login, na implementação do ADM-02.
- **21/11/2026 — D25 registrada:** ordem das mudanças na convocação, sugestões em espera e o que o estudante recebe, na implementação do RET-01.
- **21/11/2026 — D26 registrada:** regras de conferência por risco, retirada com documento e verificação, recusas no `LossReporter`, perda recusada encerrada (situação Recusado), outras sugestões da perda encerradas na retirada e na recusa e o que fica fora (desbloqueio pelo administrador no #75, RF33), na implementação do RET-03. Nota na D25: o `WhatToBring` foi absorvido pelas regras.
- **21/11/2026 — D27 registrada:** busca de objetos com índice full-text sem acentos criado na subida (sem Flyway), filtros escolhidos de `search-options` e período por mês, na implementação do ACH-04.
- **21/11/2026 — D28 registrada:** meus registros com duas rotas do estudante (`GET /loss-reports/mine` e `GET /summons/mine`), o prazo da convocação vindo só do `pickup` e a situação em linguagem simples, na implementação do PER-03.
- **21/11/2026 — D29 registrada:** atalho de balcão com entidade própria (`WalkInPickup`), estudante ou pessoa sem conta sem ligação com a conta, objeto Reservado recusado, mesmo lock do objeto, sugestões do objeto encerradas e o registro de perda intocado; aviso por e-mail para o AVI-02, na implementação do RET-04.
- **21/11/2026 — D30 registrada:** avisos por e-mail com Transactional Outbox (aviso gravado na transação da ação, relay logo após o commit e a cada minuto, unicidade por tipo e referência, `SKIP LOCKED`, até 5 tentativas), lembretes de prazo pela mesma caixa e o lembrete de registro desligado até o PER-06, na implementação do AVI-02.
- **21/11/2026 — Nota na D26:** a liberação do estudante bloqueado pelo administrador entrou no ADM-07 (rotas `/blocked-students`, recusas zeradas, histórico com motivo).
- **23/11/2026 — D38 registrada:** tarefa de não reclamados por objeto, porta `UnclaimedFoundItems` e destino em tabela própria, na implementação do PRA-01.
- **23/11/2026 — D39 registrada:** categorias gerenciáveis pelo administrador e prazos configuráveis lidos na hora, na implementação do ADM-04.
- **23/11/2026 — D41 registrada:** o `administration` passa a depender de `founditems` pela `api`, para o painel do ADM-06 (complemento à D10).
