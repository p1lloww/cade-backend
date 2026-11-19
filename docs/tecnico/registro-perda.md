# Registro de perda (PER-01)

O caminho de um registro de perda, do formulário do estudante até os eventos que o resto do sistema ouve. As regras estão nos requisitos (RF13, RF14, RN05, RN14, seção 6.2). O limite de 5 registros, com o lock otimista, tem o próprio documento: [limite-registros-abertos.md](limite-registros-abertos.md).

## 1. Classes envolvidas

```mermaid
classDiagram
    direction LR

    class LossReportController {
        +register(LossReportRequestDTO, MultipartFile, Jwt) ResponseEntity
        +photo(UUID, Jwt) ResponseEntity
    }
    class LossReportService {
        +register(LossReportRequestDTO, MultipartFile, UUID) LossReportResponseDTO
        +photoOf(UUID, UUID) StoredPhoto
    }
    class LossReportRequestDTO {
        <<record>>
        categoryId
        description
        secretDetail
        locations
        lostFrom
        lostUntil
        shift
    }
    class LossReportResponseDTO {
        <<record>>
    }
    class LossReport {
        <<entity>>
        studentId
        description
        secretDetail
        photoKey
        registeredAt
        expiresAt
        version
        +register(NewLossReport, UUID, Instant)$ LossReport
    }
    class LostPeriod {
        <<record>>
        lostFrom
        lostUntil
        shift
        +of(...)$ LostPeriod
    }
    class LossLocation {
        <<record>>
        block
        room
    }
    class LossReportStatus {
        <<sealed interface>>
        +summon() LossReportStatus
        +cancel() LossReportStatus
        +isEditable() boolean
    }
    class LossReporter {
        <<entity>>
        studentId
        version
    }
    class Category {
        <<entity>>
    }
    class PhotoStorage {
        <<interface>>
    }
    class AuditEvent {
        <<record>>
    }
    class LossReportRegisteredEvent {
        <<record>>
        lossReportId
        categoryId
        lostFrom
        lostUntil
    }

    LossReportController --> LossReportService
    LossReportController ..> LossReportRequestDTO
    LossReportService ..> LossReportResponseDTO : devolve
    LossReportService --> PhotoStorage : guarda a foto
    LossReportService ..> LossReporter : trava (RN05)
    LossReportService ..> AuditEvent : publica
    LossReportService ..> LossReportRegisteredEvent : publica
    LossReport --> Category
    LossReport *-- LostPeriod
    LossReport *-- "0..5" LossLocation
    LossReport --> LossReportStatus
```

- `LostPeriod` e `LossLocation` ficam dentro da tabela do registro (`@Embeddable`); os locais, numa tabela própria (`loss_report_locations`).
- O `LossReportRegisteredEvent` fica em `lossreports.api.event`, o único pacote que outros módulos podem ver (D10).

## 2. Fluxo do registro

```mermaid
sequenceDiagram
    autonumber
    actor E as Estudante
    participant C as LossReportController
    participant S as LossReportService
    participant DB as PostgreSQL
    participant P as PhotoStorage
    participant A as AuditRecorder<br/>(administration)
    participant X as Ouvintes depois do commit<br/>(cruzamento, CRU-02)

    E->>C: POST /loss-reports (multipart: report + photo)
    C->>C: @Valid no report
    alt campo obrigatório ausente ou inválido
        C-->>E: 400 com o erro de cada campo
    end
    C->>S: register(report, photo, studentId)

    rect rgba(127, 127, 127, 0.12)
        Note over S,A: uma transação
        S->>DB: categoria existe?
        S->>S: LostPeriod.of (sem data futura, até 30 dias)
        S->>DB: trava o estudante e conta os registros em andamento
        alt 5 ou mais
            S-->>E: 409 limite de registros (RN05)
        end
        opt foto enviada
            S->>P: save(foto)
        end
        S->>DB: INSERT do registro (Aberto, vence em 30 dias)
        S->>A: AuditEvent (síncrono)
        A->>DB: INSERT no histórico (RF43)
        S->>S: publica LossReportRegisteredEvent
    end

    alt commit
        S-->>C: LossReportResponseDTO
        C-->>E: 201 com o registro
        S-)X: LossReportRegisteredEvent (@Async, AFTER_COMMIT)
    else rollback
        S->>P: apaga a foto guardada
        S-->>E: 409 "tente de novo" (lock otimista, RN05) ou 500 (erro inesperado)
    end
```

## 3. Os dois eventos

O registro publica dois eventos, e cada um é ouvido de um jeito:

| Evento | Quem ouve | Quando | Se o ouvinte falhar |
|---|---|---|---|
| `AuditEvent` | `AuditRecorder` (`administration`) | Na hora, na **mesma transação** | O registro é desfeito: nenhuma ação sem histórico (RF43) |
| `LossReportRegisteredEvent` | Cruzamento (CRU-02, ainda não implementado) | **Depois do commit**, em outra thread (`@Async`, Virtual Threads) | O registro continua salvo: o cruzamento nunca desfaz nem atrasa o estudante (D05) |

Por que evento e não chamada direta: pela D10, `lossreports` não depende de ninguém. Quem conhece as perdas é o `matching` (e o `administration` conhece só o `AuditEvent` do `shared`). O evento deixa o registro sem saber quem reage, e um ouvinte novo (um aviso por e-mail, por exemplo) entra sem mudar o `LossReportService`.

```mermaid
flowchart LR
    S[LossReportService] -- AuditEvent --> A[AuditRecorder<br/>mesma transação]
    S -- LossReportRegisteredEvent --> B{commit?}
    B -- sim --> M[Cruzamento CRU-02<br/>@Async]
    B -- não --> N[ninguém recebe]
```

O evento só leva ids e datas: a descrição, o detalhe secreto e a foto não saem do módulo (RF14).

## 4. Situações do registro

As transições da seção 6.2, implementadas no `LossReportStatus`. O PER-01 cria o registro em Aberto; as outras transições são usadas pelos cards indicados.

```mermaid
stateDiagram-v2
    state "Aguardando informações" as Aguardando
    state "Concluído" as Concluido
    [*] --> Aberto: estudante registra (PER-01)
    Aberto --> Aguardando: funcionário pede informações (PER-06)
    Aguardando --> Aberto: estudante responde ou 5 dias (RN13)
    Aberto --> Convocado: funcionário convoca (CRU-03)
    Convocado --> Aberto: convocação vence ou verificação falha (RN15)
    Convocado --> Concluido: retirada registrada (RET-03)
    Aberto --> Vencido: 30 dias sem renovação (RN14)
    Aberto --> Cancelado: estudante cancela (PER-06)
    Aguardando --> Cancelado: estudante cancela (PER-06)
    Concluido --> [*]
    Vencido --> [*]
    Cancelado --> [*]
```

- **Em andamento** (contam para o limite de 5, RN05): Aberto, Aguardando informações e Convocado.
- **Editáveis** (RN06, edição no PER-07): Aberto e Aguardando informações.

## 5. Foto

- Opcional, na parte `photo` do multipart: JPEG, PNG ou WebP, até 5 MB (mesmas regras do cadastro de objetos).
- Guardada na pasta pela `PhotoStorage` (D21), só **depois** de todas as regras conferidas; se a transação for desfeita, é apagada.
- `GET /loss-reports/{id}/photo`: só o dono. Registro de outro estudante responde 404, igual a um inexistente (RF14).
