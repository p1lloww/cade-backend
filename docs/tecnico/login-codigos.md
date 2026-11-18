# Códigos de login (`LoginCodeStore`)

Como o `LoginCodeStore` (`access.persistence.logincode`) guarda os códigos de login no PostgreSQL, como o `LoginService` usa cada operação e por que cada regra é um comando SQL só. A decisão está na D18, que substituiu a D16 (Redis). As regras do código (10 min, uso único, só hash, 5 tentativas, 1 envio por minuto) estão na D03; o bloqueio contra força bruta, na D18. Os valores ficam em `cade.login-code.*` do `application.yml`.

## 1. O que fica guardado

Tabela `login_codes`, criada pelo Hibernate a partir da entidade `LoginCode` (D06). Há **no máximo uma linha por e-mail** (o e-mail é a chave primária): um código novo sobrescreve o anterior. Por isso a tabela nunca passa do número de pessoas que já pediram um código e não precisa de rotina de limpeza.

| Coluna | Conteúdo |
|---|---|
| `email` | e-mail normalizado (chave primária) |
| `code_hash` | hash BCrypt do código; o código puro **nunca** é guardado |
| `created_at` | quando o código atual foi emitido; também marca o fim da espera de 1 min |
| `expires_at` | quando o código atual deixa de valer (10 min depois, ou na hora de um bloqueio) |
| `attempts` | tentativas com o código atual, certas ou erradas; volta a 0 a cada código novo |
| `failures` | tentativas **erradas** do e-mail, somando todos os códigos; não volta a 0 com código novo |
| `blocked_until` | fim do bloqueio; vazio quando o e-mail não está bloqueado |

- O código puro só existe na memória e no `LoginCodeRequestedEvent`, que o leva para o e-mail.
- Uma linha vencida não é apagada: ela é ignorada até o próximo pedido sobrescrevê-la.
- Um login válido apaga a linha, o que também zera as tentativas erradas.

## 2. Operações

Toda operação que confere uma regra e altera a linha é **um único comando SQL**. O PostgreSQL trava a linha durante o comando, então duas requisições ao mesmo tempo não passam juntas pela mesma conferência.

| Método | SQL | Usado em |
|---|---|---|
| `find(email)` | `SELECT` | `requestCode` (conferência rápida) e `exchangeCode` |
| `save(code, intervalo)` | `INSERT ... ON CONFLICT (email) DO UPDATE ... WHERE` a espera passou e não há bloqueio | `requestCode` |
| `registerAttempt(email, agora)` | `UPDATE ... SET attempts = attempts + 1 WHERE` o código vale `RETURNING attempts` | `exchangeCode` |
| `registerFailure(email, agora, limite, duração)` | `UPDATE` que soma 1 em `failures` e, ao chegar no limite, bloqueia e encerra o código | `exchangeCode` |
| `consume(email, hash)` | `DELETE ... WHERE email = ? AND code_hash = ?` | `exchangeCode` |

**A hora é sempre um parâmetro**, vinda do `Clock` da aplicação, e nunca o `now()` do banco. Assim os testes adiantam o relógio (`MutableClock`) para conferir expiração, espera e bloqueio sem esperar de verdade.

**Nenhum método abre transação.** Uma tentativa recusada termina com exceção, e um rollback desfaria a tentativa que acabou de ser contada. Cada comando é confirmado sozinho.

## 3. Fluxo: pedir um código

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuário
    participant S as LoginService
    participant St as LoginCodeStore
    participant DB as PostgreSQL
    participant E as Listener de e-mail

    U->>S: requestCode(email)
    S->>S: normaliza e valida o domínio @ucsal.edu.br
    alt e-mail fora do domínio
        S-->>U: 400 "Use o seu e-mail institucional"
    end
    S->>St: find(email)
    alt bloqueado ou dentro de 1 min do último código
        S-->>U: 429 com Retry-After
    end
    S->>S: gera 6 dígitos com SecureRandom e calcula o hash
    S->>St: save(LoginCode, 1 min)
    St->>DB: INSERT ... ON CONFLICT DO UPDATE ... WHERE espera passou e sem bloqueio
    alt nenhuma linha afetada (outra requisição chegou antes)
        S-->>U: 429 com Retry-After
    else linha gravada
        S->>E: publica LoginCodeRequestedEvent com o código puro
        E-->>U: e-mail com o código
    end
```

A conferência com `find` antes do hash existe para uma requisição recusada não gastar o BCrypt à toa. Quem decide de verdade é o `save`: se duas requisições passam juntas pela conferência, só uma consegue gravar.

### Envio do e-mail (AVI-01)

O e-mail sai numa thread separada, depois que a requisição já respondeu:

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuário
    participant S as LoginService<br/>(thread da requisição)
    participant P as Proxy do @Async
    participant L as LoginCodeEmailListener<br/>(thread task-N)
    participant M as SmtpEmailSender
    participant SMTP as Gmail / Mailpit

    S->>P: publishEvent(LoginCodeRequestedEvent)
    P->>L: entrega a tarefa ao executor (Virtual Thread nova)
    P-->>S: volta na hora
    S-->>U: 204 No Content (~100 ms)
    L->>L: LoginCodeEmail monta assunto e texto (código e hora de expiração)
    L->>M: send(EmailMessage)
    M->>SMTP: SMTP (1–3 s no Gmail)
    alt envio aceito
        M->>M: log debug "E-mail sent to ..."
        SMTP-->>U: e-mail com o código
    else falha (rede, autenticação, servidor fora)
        M->>M: log warn "Could not send e-mail to ..." (não relança)
    end
```

- **Quem consome o evento:** o `LoginCodeEmailListener` (`notifications`), com `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)` + `@Async`. O `access` não conhece o `notifications`: ele só publica o evento (Observer), e a dependência vai de `notifications` para `access.api`, como a D10 permite.
- **Por que `fallbackExecution = true`:** o `LoginService` publica fora de transação (D18). Sem esse parâmetro, o Spring descartaria o evento em silêncio, porque não haveria commit para esperar.
- **Por que assíncrono (D05):** a resposta não depende da velocidade do Gmail, e uma falha de e-mail não chega à requisição (RNF08). Com `spring.threads.virtual.enabled`, o executor padrão cria uma Virtual Thread por tarefa. Enquanto espera a rede, a Virtual Thread não prende uma thread do sistema operacional.
- **Por que funciona:** o `@EnableAsync` da `AsyncConfiguration` (`shared.event`) faz o Spring trocar o listener por um proxy que entrega a chamada ao executor. Sem ele, o `@Async` seria ignorado em silêncio e o e-mail sairia na thread da requisição.
- **Custo aceito:** se o envio falhar, a tela já mostrou "código enviado". A falha só aparece no log, e a pessoa pede outro código depois de 1 minuto. Se o back-end cair entre a resposta e o envio, o e-mail se perde (D05).
- **Sigilo:** o código em texto puro só existe no evento e no corpo do e-mail. O `toString` do `LoginCodeRequestedEvent` e o do `EmailMessage` o escondem, e os logs registram só o destinatário e o assunto.
- **Servidor:** sem `MAIL_HOST` no `.env`, o envio vai para o Mailpit (`localhost:1025`, veja em http://localhost:8025). Para o Gmail, preencha todas as variáveis `MAIL_*` do `.env.example`, com o `MAIL_FROM` igual ao `MAIL_USERNAME`. Para ver os logs de debug, suba com `--logging.level.br.ucsal.cade.shared.mail=DEBUG --logging.level.br.ucsal.cade.notifications=DEBUG`.

## 4. Fluxo: trocar o código pelo token

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuário
    participant S as LoginService
    participant St as LoginCodeStore
    participant DB as PostgreSQL

    U->>S: exchangeCode(email, código)
    S->>St: find(email)
    alt sem linha, ou código vencido
        S-->>U: 400 "Código inválido ou expirado"
    end
    S->>St: registerAttempt(email, agora)
    St->>DB: UPDATE attempts + 1 WHERE o código vale RETURNING attempts
    alt nenhuma linha, ou mais de 5 tentativas
        S-->>U: 400 "Código inválido ou expirado"
    end
    S->>S: compara o código com o hash (BCrypt)
    alt código errado
        S->>St: registerFailure(email, agora, 15, 15 min)
        St->>DB: UPDATE failures + 1; no limite, bloqueia e encerra o código
        S-->>U: 400 "Código inválido ou expirado"
    end
    S->>St: consume(email, hash)
    St->>DB: DELETE WHERE email e hash conferem
    alt outra requisição apagou antes
        S-->>U: 400 "Código inválido ou expirado"
    else apagou
        S->>DB: busca o usuário ou cria como estudante
        S-->>U: conta (o controller emite o JWT)
    end
```

Toda recusa devolve a **mesma mensagem**, para quem tenta adivinhar não saber se o código está errado, vencido, travado ou bloqueado. O motivo real vai só para o log.

## 5. Estados de um e-mail

```mermaid
stateDiagram-v2
    [*] --> Valido: requestCode
    Valido --> Valido: tentativa errada (até 5)
    Valido --> Travado: 5 tentativas
    Valido --> [*]: código certo (linha apagada)
    Valido --> Vencido: 10 min
    Travado --> Valido: novo código, depois de 1 min
    Vencido --> Valido: novo código
    Valido --> Bloqueado: 15ª tentativa errada somando os códigos
    Bloqueado --> Valido: novo código, depois de 15 min
```

## 6. Por que cada regra é um comando só

Se a contagem de tentativas fosse "ler o número, conferir, gravar o número + 1" em passos separados, 20 palpites enviados juntos leriam todos `attempts = 0` e todos seriam comparados. Com `UPDATE ... SET attempts = attempts + 1 ... RETURNING attempts`, o PostgreSQL trava a linha durante o comando: cada requisição recebe um número diferente (1, 2, ..., 20), e só as 5 primeiras chegam a comparar o código.

O mesmo vale para o uso único: o `DELETE` informa quantas linhas apagou. Se duas requisições com o código certo chegam juntas, só uma apaga a linha, e só ela faz login. O `DELETE` também confere o hash: se um código novo substituiu o antigo no meio da conferência, o antigo não apaga o novo.

No bloqueio, o `UPDATE` usa `CASE` nas três colunas (`failures`, `blocked_until`, `expires_at`). No PostgreSQL, o lado direito do `SET` sempre lê os valores de antes da alteração, então as três colunas veem o mesmo `failures`.

## 7. Limitações conhecidas

- **Bloqueio por e-mail, não por IP.** Quem sabe o e-mail de alguém pode errar o código de propósito e bloquear essa pessoa por 15 minutos. Pedir códigos seguidos também invalida o código da vítima. Para o MVP, a proteção contra adivinhação vale mais que esse incômodo.
- **Tentativas restantes.** Cada ciclo permite 15 palpites (3 códigos de 5) e custa uns 17 minutos (2 min de espera entre os códigos e 15 min de bloqueio). São cerca de 1.300 palpites por dia contra um e-mail, ou 0,13% de chance de acertar um código de 6 dígitos. Sem o bloqueio, eram cerca de 7.200 palpites por dia (0,72%).

## 8. Testes

- `LoginCodeStoreTest` (`@DataJpaTest` com PostgreSQL no Testcontainers): gravação, espera de 1 min, sobrescrita mantendo as tentativas erradas, tentativa só com código válido, bloqueio no limite, e-mail bloqueado sem código novo e `consume` uma vez só e só com o hash conferido.
- `LoginServiceTest`: as regras da D03 e da D18 pelo serviço, incluindo 20 palpites em paralelo, 2 acertos simultâneos, bloqueio depois de 3 códigos travados e fim do bloqueio depois de 15 min.
- `AuthControllerTest`: os mesmos casos pelos endpoints, com as respostas HTTP.
- `LoginCodeEmailTest`: o texto do e-mail tem o código e a hora de expiração no fuso de Salvador.
- `LoginCodeEmailListenerTest`: o pedido de código chega ao `EmailSender` (mock) por outra thread, mesmo sem transação.
- `SmtpEmailSenderTest`: remetente, destinatário e assunto da mensagem, e uma falha do SMTP não lança exceção.
