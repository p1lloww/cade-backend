# Avisos por e-mail com Transactional Outbox (AVI-02)

Como o módulo `notifications` manda os e-mails ao estudante (RF34): na convocação, 1 dia antes de a convocação vencer e na retirada. A garantia é que **nenhum aviso se perde e nenhum sai em dobro**, mesmo com o servidor de e-mail fora do ar ou a aplicação caindo no meio do caminho (RNF08). Para isso, os avisos passam por uma **caixa de saída no banco**, o padrão *Transactional Outbox*. As escolhas estão na D30.

## 1. O problema

Quando o funcionário convoca um estudante, duas coisas precisam acontecer: **gravar a convocação** no PostgreSQL e **mandar o e-mail** pelo SMTP. São dois sistemas diferentes, e não existe uma transação que abranja os dois.

| Jeito | O que dá errado |
|---|---|
| Enviar o e-mail **dentro** da transação | O SMTP é lento e a requisição espera. Pior: se o commit falhar depois do envio, o estudante recebe um e-mail de uma convocação que não existe. |
| Enviar **depois** do commit, direto (`AFTER_COMMIT` + `@Async`, como o código de login) | Se o SMTP estiver fora, o e-mail se perde: só fica uma linha no log. Se a aplicação cair entre o commit e o envio, também se perde, sem rastro. |
| Enviar depois do commit e **gravar o aviso** para reenviar | Resolve a falha do SMTP, mas a gravação acontece noutra transação, depois do commit. Uma queda entre o commit e a gravação ainda perde o aviso. |
| **Gravar o aviso na mesma transação** e enviar depois (Outbox) | O aviso existe **se e somente se** a convocação existe. O envio pode falhar e ser repetido quantas vezes for preciso. |

A última linha é o Transactional Outbox. A transação do banco, que já garante a convocação, passa a garantir também o aviso. O envio vira uma tarefa separada, que pode ser repetida sem medo.

## 2. A ideia em uma figura

```mermaid
flowchart LR
    subgraph T["Uma transação do PostgreSQL"]
        A["Ação de negócio<br/>(ex.: convocar)"] --> E["Evento<br/>SummonsCreatedEvent"]
        E --> O["Outbox.enqueue<br/>INSERT notifications (PENDING)"]
    end
    T -->|commit| R1["OutboxRelay.deliverSoon<br/>(logo após o commit)"]
    R1 --> D["NotificationDelivery.deliver"]
    R2["OutboxRelay.relayPending<br/>(a cada 1 min)"] --> D
    D -->|e-mail da conta| U["access: UserEmails"]
    D -->|envia| S["SMTP<br/>(Mailpit / Gmail)"]
```

Três papéis, cada um numa pasta de `notifications.service.outbox`:

| Papel | Pasta | Classes | O que faz |
|---|---|---|---|
| A caixa | `persistence/outbox` | `Notification`, `NotificationRepository` | A tabela `notifications`: cada linha é um aviso esperando ou já enviado |
| A entrada | `service/outbox/enqueue` | `Outbox`, `NotificationContent` | Grava o aviso **na transação de quem chama** (`MANDATORY`) |
| A saída (o *relay*) | `service/outbox/relay` | `OutboxRelay`, `NotificationDelivery` | Tira da caixa e envia: logo após o commit e, de novo, a cada minuto |

## 3. Diagrama de classes

```mermaid
classDiagram
    direction LR
    class PickupNoticeListener {
        <<@EventListener, MANDATORY>>
        +on(SummonsCreatedEvent)
        +on(PickupRegisteredEvent)
    }
    class DeadlineNoticeJob {
        <<@Scheduled 1 h>>
        +queueDeadlineNotices()
    }
    class Outbox {
        <<enqueue>>
        +enqueue(kind, referenceId, recipientId, content) boolean
    }
    class NotificationContent {
        <<record>>
        subject
        body
    }
    class Notification {
        <<entity: notifications>>
        id
        kind: NotificationKind
        referenceId
        recipientId
        subject, body
        status: NotificationStatus
        attempts
        nextAttemptAt, sentAt
        +markSent(at)
        +markFailed(at, maxAttempts, retryDelay)
        +abandon()
    }
    class NotificationRepository {
        +insertIfAbsent(...) int
        +findDueIds(now, limit) List~UUID~
        +lockPending(id) Optional~Notification~
    }
    class OutboxRelay {
        <<relay>>
        +deliverSoon(id)  @Async
        +relayPending()   @Scheduled 1 min
    }
    class NotificationDelivery {
        +deliver(id)  REQUIRES_NEW
    }
    class UserEmails {
        <<interface, access.api>>
        +emailOf(userId) Optional~String~
    }
    class EmailSender {
        <<interface, shared.mail>>
        +send(EmailMessage) boolean
    }
    PickupNoticeListener --> Outbox
    DeadlineNoticeJob --> Outbox
    Outbox ..> NotificationContent
    Outbox --> NotificationRepository : INSERT ... ON CONFLICT DO NOTHING
    Outbox ..> OutboxRelay : afterCommit → deliverSoon
    OutboxRelay --> NotificationDelivery
    OutboxRelay --> NotificationRepository : findDueIds
    NotificationDelivery --> NotificationRepository : lockPending (SKIP LOCKED)
    NotificationDelivery --> UserEmails
    NotificationDelivery --> EmailSender
    NotificationRepository ..> Notification
```

- **Quem escreve na caixa** são os ouvintes de evento (`PickupNoticeListener`) e a tarefa dos prazos (`DeadlineNoticeJob`). Eles só montam o texto e chamam `Outbox.enqueue`.
- **Quem lê da caixa** é só o relay. Ninguém mais chama o `EmailSender` para avisos.
- O código de login **não passa pela caixa**: ele vale 10 minutos e não deve ficar gravado no banco. Continua indo direto, como antes (`LoginCodeEmailListener`).

## 4. Sequência: o caminho normal, com uma falha e o reenvio

```mermaid
sequenceDiagram
    autonumber
    actor F as Funcionário
    participant SS as SummonsService
    participant L as PickupNoticeListener
    participant O as Outbox
    participant DB as PostgreSQL
    participant R as OutboxRelay
    participant D as NotificationDelivery
    participant A as access (UserEmails)
    participant M as SMTP

    F->>SS: POST /suggestions/{id}/summon
    rect rgb(235, 245, 255)
    note over SS,DB: uma transação
    SS->>DB: reserva objeto, convoca perda e sugestão, grava Summons
    SS->>L: publica SummonsCreatedEvent (síncrono)
    L->>O: enqueue(SUMMONS_CREATED, convocação, estudante, texto)
    O->>DB: INSERT notifications (PENDING) ON CONFLICT DO NOTHING
    end
    SS->>DB: commit
    SS-->>F: 201 (sem esperar o e-mail)

    O-)R: afterCommit → deliverSoon(id) numa Virtual Thread
    R->>D: deliver(id)
    rect rgb(245, 245, 245)
    note over D,DB: outra transação
    D->>DB: SELECT ... FOR UPDATE SKIP LOCKED
    D->>A: emailOf(estudante)
    D->>M: send(e-mail)
    M--xD: falhou (servidor fora do ar)
    D->>DB: attempts = 1, next_attempt_at = agora + 5 min
    end

    note over R: 5 minutos depois
    R->>DB: findDueIds(agora)
    R->>D: deliver(id)
    D->>DB: SELECT ... FOR UPDATE SKIP LOCKED
    D->>M: send(e-mail)
    M-->>D: aceito
    D->>DB: status = SENT, attempts = 2
```

O funcionário recebe a resposta no passo 8, antes de qualquer tentativa de envio. Quando nada falha, o e-mail sai no mesmo segundo pelo `deliverSoon`, e o `relayPending` não tem trabalho. Ele existe para o que dá errado.

### E se a aplicação cair entre o commit e o envio?

```mermaid
sequenceDiagram
    autonumber
    participant SS as SummonsService
    participant DB as PostgreSQL
    participant R as OutboxRelay (depois de reiniciar)
    participant D as NotificationDelivery

    SS->>DB: convocação + aviso (PENDING), commit
    note over SS: a aplicação cai antes do deliverSoon
    note over R: a aplicação volta
    R->>DB: findDueIds(agora) → o aviso continua lá
    R->>D: deliver(id)
    D->>DB: status = SENT
```

Esse é o caso que só o Outbox resolve: o aviso foi gravado junto com a convocação, então sobrevive à queda.

## 5. Estados do aviso

```mermaid
stateDiagram-v2
    [*] --> PENDING : enqueue (na transação da ação)
    PENDING --> SENT : servidor aceitou
    PENDING --> PENDING : falhou e ainda há tentativa (espera 5, 10, 15, 20 min)
    PENDING --> ABANDONED : 5ª falha
    PENDING --> ABANDONED : conta não existe mais
    SENT --> [*]
    ABANDONED --> [*]
```

- A espera **cresce** a cada falha (`retryDelay × tentativas`), para não insistir num servidor fora do ar.
- Os números vêm de `cade.notifications.*` no `application.yml`: 5 tentativas, 5 min de espera, reenvio a cada 1 min e 50 avisos por rodada.
- Um aviso `ABANDONED` fica na tabela para consulta: dá para saber quem não recebeu e por quê, olhando o log com o id.

## 6. As três garantias e quem cuida de cada uma

| Garantia | Como | Onde |
|---|---|---|
| **Nada se perde:** aviso gravado ⇔ ação gravada | `enqueue` com `Propagation.MANDATORY`, chamado por um ouvinte síncrono na transação da ação | `Outbox`, `PickupNoticeListener` |
| **Nada sai em dobro (mesmo aviso):** um evento repetido ou várias rodadas da tarefa dos prazos | Par (`kind`, `reference_id`) único + `INSERT ... ON CONFLICT DO NOTHING` | `Notification`, `NotificationRepository.insertIfAbsent` |
| **Nada sai em dobro (mesmo envio):** o `deliverSoon` e o `relayPending` pegam o mesmo aviso ao mesmo tempo, ou dois servidores rodam o relay | `SELECT ... FOR UPDATE SKIP LOCKED`: quem chega depois recebe vazio e desiste | `NotificationRepository.lockPending`, `NotificationDelivery` |

### A disputa pelo mesmo aviso, em detalhe

```mermaid
sequenceDiagram
    autonumber
    participant A as deliverSoon
    participant DB as PostgreSQL
    participant B as relayPending

    A->>DB: SELECT ... WHERE id = X AND status = 'PENDING' FOR UPDATE SKIP LOCKED
    DB-->>A: a linha (A segura a trava)
    B->>DB: SELECT ... WHERE id = X AND status = 'PENDING' FOR UPDATE SKIP LOCKED
    DB-->>B: vazio (a linha está travada e é pulada)
    note over B: desiste sem enviar
    A->>A: envia o e-mail
    A->>DB: status = SENT, commit (solta a trava)
```

Sem o `SKIP LOCKED`, o B esperaria a trava e depois encontraria `status = SENT`. Funcionaria igual, mas prenderia uma thread à toa. O teste `twoDeliveriesOfTheSameNotificationAtTheSameTimeSendItOnce` dispara duas entregas simultâneas em Virtual Threads e confere que sai um e-mail só.

## 7. Por que a linha guarda o texto, mas não o endereço

- **O texto é montado na transação da ação**, a partir do evento. É uma função pura dos dados do evento, e não falha. Montar depois exigiria guardar os dados do evento e reconstruí-los.
- **O endereço é buscado na hora do envio** (`UserEmails`, no `access`). Assim, nada que dependa de outro módulo ou possa falhar roda dentro da transação da convocação. Se o e-mail da conta mudar, o aviso vai para o endereço atual.
- Por isso o texto **nunca leva segredo**: ele fica gravado no banco. O código de login é o único e-mail com segredo, e não passa pela caixa.

## 8. Lembretes de prazo (`DeadlineNoticeJob`)

```mermaid
sequenceDiagram
    autonumber
    participant J as DeadlineNoticeJob (de hora em hora)
    participant P as pickup (SummonsDeadlines)
    participant O as Outbox
    participant DB as PostgreSQL

    rect rgb(235, 245, 255)
    note over J,DB: uma transação
    J->>P: expiringBetween(agora, agora + 1 dia)
    P-->>J: convocações Ativas nessa janela
    loop cada convocação
        J->>O: enqueue(SUMMONS_EXPIRING, convocação, estudante, texto)
        O->>DB: INSERT ... ON CONFLICT DO NOTHING
    end
    end
    note over J: commit → relay envia, como na seção 4
```

Uma convocação fica até 24 h dentro da janela, então aparece em várias rodadas. **A tarefa não guarda o que já avisou.** Quem garante um lembrete só é a unicidade da caixa: da segunda rodada em diante, o `INSERT` não grava nada. Se a aplicação ficar fora do ar numa rodada, a seguinte cobre.

O lembrete de **registro de perda perto de vencer** (RF35) já está pronto (`LossReportDeadlines`, `LossReportExpiringNotice`), mas fica **desligado** (`cade.notifications.deadlines.loss-report-notice-enabled: false`) até o PER-06. Hoje o registro ainda não vence nem é renovado, e o e-mail avisaria de algo que não acontece (D30).

## 9. Quais e-mails existem

| Tipo (`NotificationKind`) | Quando | Referência | Texto |
|---|---|---|---|
| `SUMMONS_CREATED` | na convocação (RF34) | a convocação | onde, horário, até quando e o que levar |
| `SUMMONS_EXPIRING` | menos de 1 dia para a convocação vencer (RF34) | a convocação | os mesmos dados, com o prazo em destaque |
| `SUGGESTION_CLOSED` | com a retirada, uma análise de objeto para o registro foi encerrada (RF33, RET-06) | a sugestão encerrada | texto neutro: uma análise foi encerrada e o registro continua aberto; não cita o objeto (RN03) |
| `SUMMONS_EXPIRED` | a convocação venceu sem retirada (RF27, RET-02) | a convocação | o prazo terminou, o objeto deixou de ficar reservado e o registro continua aberto; não cita o objeto |
| `PICKUP_REGISTERED` | na retirada (RF34) | a retirada | data e hora; "se não foi você, procure a sala" |
| `LOSS_REPORT_EXPIRING` | 3 dias antes de o registro vencer (RF35), **desligado até o PER-06** | o registro | a descrição do próprio estudante e a data |

Nenhum e-mail fala do objeto achado: nem foto, nem descrição interna, nem detalhes reservados, nem local (RN03). Os testes conferem que esses dados não aparecem no texto.

**Fora deste card:**
- **E-mail do pedido de mais informações (RF35):** o pedido ainda não existe; entra no PER-06.
- **Aviso das sugestões encerradas pelo atalho de balcão (D29):** não existe de propósito. O estudante nunca vê as sugestões, e avisar que uma foi encerrada revelaria que apareceu na sala um objeto parecido com o dele (RN03). Quem ainda procura é lembrado pelo aviso de registro perto de vencer.

## 10. Onde está testado

- [`OutboxTest`](../../src/test/java/br/ucsal/cade/notifications/service/outbox/enqueue/OutboxTest.java), a entrada:
  - fora de transação, é recusado;
  - uma ação desfeita não deixa aviso nem e-mail;
  - uma ação gravada envia logo após o commit;
  - o mesmo aviso entra uma vez só;
  - uma falha fica Pendente, e o relay só tenta de novo quando a espera passa.
- [`NotificationDeliveryTest`](../../src/test/java/br/ucsal/cade/notifications/service/outbox/relay/NotificationDeliveryTest.java), a saída:
  - a espera cresce, e o aviso é abandonado na 5ª falha;
  - uma conta inexistente é abandonada sem envio;
  - duas entregas simultâneas mandam um e-mail só.
- [`PickupNoticeListenerTest`](../../src/test/java/br/ucsal/cade/notifications/service/pickup/PickupNoticeListenerTest.java), com a convocação e a retirada de verdade:
  - o e-mail chega ao estudante, sem dados do objeto;
  - uma convocação recusada (409) não deixa aviso.
- [`DeadlineNoticeJobTest`](../../src/test/java/br/ucsal/cade/notifications/service/deadline/DeadlineNoticeJobTest.java) e `DeadlineNoticeJobDefaultsTest`:
  - um lembrete só por convocação, mesmo com a tarefa rodando várias vezes;
  - o lembrete sai quando a convocação entra na janela;
  - nada para convocação concluída ou vencida;
  - o lembrete do registro desligado por padrão.
- `PickupNoticesTest` e `DeadlineNoticesTest`: os textos, com datas no horário de Salvador.
