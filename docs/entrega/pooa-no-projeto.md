# POOA no Cadê?

Onde os conteúdos de Programação Orientada a Objetos Avançada aparecem no código, **qual problema cada um resolve** e como mostrar na apresentação. O manual do POOALAB dá até 5 pontos de bônus pela "aplicação pertinente e explicada" desses conteúdos, e diz que "a inserção artificial de padrões não gera pontuação". Por isso, cada item abaixo começa pelo problema, e não pelo padrão.

**Para a equipe:** antes da mostra, cada integrante deve conseguir explicar pelo menos os itens marcados com ★, abrindo o arquivo indicado. As perguntas no fim de cada seção são as que o professor provavelmente vai fazer.

**Para manter atualizado:** toda PR que usar um conteúdo de POOA acrescenta uma linha aqui. Os links apontam para os arquivos na `main`.

## Resumo

| Conteúdo | Onde | Problema que resolve | Situação |
|---|---|---|---|
| ★ State | `FoundItemStatus`, `LossReportStatus` | O objeto e o registro de perda só mudam de situação pelas transições permitidas (RN01) | Na `main` (ACH-01); perda no PER-01 |
| ★ Observer (eventos) | `LoginCodeRequestedEvent` → `LoginCodeEmailListener`; `AuditEvent` → `AuditRecorder`; `LossReportRegisteredEvent` | Um módulo avisa que algo aconteceu sem conhecer quem reage | Na `main` (AVI-01, ADM-01); perda no PER-01 |
| ★ Concorrência: lock otimista | `LossReporter` + `OPTIMISTIC_FORCE_INCREMENT` | Dois registros simultâneos não passam do limite de 5 em andamento (RN05) | PER-01 |
| ★ Anotações e reflexão | `@RequiresRole`, `RoleRequirement`, `RouteAuthorizationCoverageTest` | Cada rota declara quem pode usá-la, e um teste impede que uma rota fique aberta por esquecimento | Na `main` (ACE-03) |
| Virtual Threads | `AsyncConfiguration` + `spring.threads.virtual.enabled` | O e-mail é enviado sem prender a resposta ao usuário | Na `main` (AVI-01) |
| ★ SOLID: inversão de dependência | `EmailSender`, `PhotoStorage`, `Clock` | Trocar o SMTP ou o disco sem mexer em quem usa; testar prazos sem esperar | Na `main`; `PhotoStorage` na PR #65 |
| SOLID: aberto/fechado | `ApiException` e subclasses | Um tipo novo de erro não exige mudar o tratador | Na `main` |
| SOLID: segregação de interfaces | `AuditRecordRepository` | O histórico não tem método de exclusão (RN19) | Na `main` (ADM-01) |
| Collections e Streams | `RoleRequirement`, `AuthorizationConfiguration`, `FoundItemStatus` | Calcular os perfis aceitos e a hierarquia a partir do enum | Na `main` |
| Records e enums com comportamento | `AuditEvent`, `Role`, `RiskLevel`, `LostPeriod`, `LossLocation` | Dados imutáveis validados na criação; regra junto do dado | Na `main`; `RiskLevel` na PR #65; período e local no PER-01 |
| GRASP: especialista na informação | `FoundItemStatus`, `RiskLevel.canBeRaisedTo`, `FoundItem.register` | A regra fica na classe que tem os dados para decidir | Na `main`; `FoundItem` na PR #65 |
| Baixo acoplamento (módulos) | `ModuleBoundariesTest` (ArchUnit) | Módulos só se falam pela `api` e nas direções da D10 | Na `main` (GES-01) |
| Strategy + Composite | `CompatibilityCriterion` | A nota do cruzamento soma critérios configuráveis (RN07) | Planejado (CRU-01) |
| Strategy por risco | regras de conferência no balcão | O que conferir depende do risco do objeto (RN09) | Planejado (RET-03) |

---

## ★ State: situações do objeto achado

**Problema.** Um objeto passa por Disponível, Reservado, Devolvido, Não reclamado e Destinado (seção 6.1 dos requisitos), e só pode mudar pelas transições permitidas (RN01). Por exemplo, um objeto Devolvido não pode ser convocado de novo. Com um `if` por situação espalhado pelos serviços, cada regra nova seria um lugar a mais para esquecer.

**Como.** Cada situação é um `record` que implementa a `sealed interface FoundItemStatus` e sabe para onde pode ir. A ação que a situação não permite cai no método padrão da interface, que recusa com `InvalidStatusTransitionException` (HTTP 409).

```java
public sealed interface FoundItemStatus {
    default FoundItemStatus reserve() { throw refused("convocar"); }   // padrão: recusa
    record Available() implements FoundItemStatus {
        @Override public FoundItemStatus reserve() { return RESERVED; } // Disponível pode ser convocado
    }
    record Returned() implements FoundItemStatus { }                     // final: recusa tudo
}
```

O `sealed` faz o compilador conhecer todas as situações: os `switch` de `code()` e `label()` não têm `default`, e uma situação nova faz o compilador apontar cada lugar que precisa tratá-la.

**Onde:** [`FoundItemStatus.java`](../../src/main/java/br/ucsal/cade/founditems/persistence/status/FoundItemStatus.java), [`FoundItemStatusConverter.java`](../../src/main/java/br/ucsal/cade/founditems/persistence/status/FoundItemStatusConverter.java) (grava no banco como texto) e os testes em [`FoundItemStatusTest.java`](../../src/test/java/br/ucsal/cade/founditems/persistence/status/FoundItemStatusTest.java), que cobrem todas as transições, permitidas e proibidas.

**Registro de perda (PER-01):** o [`LossReportStatus`](../../src/main/java/br/ucsal/cade/lossreports/persistence/status/LossReportStatus.java) segue o mesmo modelo para a seção 6.2, e ainda diz se o registro pode ser editado (`isEditable`, RN06) e quais situações contam para o limite de 5 (`ACTIVE`, RN05). Os testes em [`LossReportStatusTest.java`](../../src/test/java/br/ucsal/cade/lossreports/persistence/status/LossReportStatusTest.java) cobrem as 8 transições permitidas e as 34 proibidas.

**Próximo:** a sugestão do cruzamento (seção 6.3).

**Perguntas prováveis**
- *Por que não um enum com um `switch`?* O enum concentraria todas as transições num método só. Com o State, cada situação guarda as próprias regras, e o `sealed` dá a mesma garantia do enum: não existe situação fora da lista.
- *Por que records?* As situações não guardam dados; record dá `equals` por valor, então duas instâncias de Disponível são iguais.

---

## ★ Observer: eventos entre módulos

**Problema.** Quando um código de login é gerado, alguém precisa enviar o e-mail. Mas o módulo de acesso não deveria conhecer o módulo de avisos, e o envio não deveria atrasar a resposta. O mesmo vale para o histórico (RF43): toda ação relevante precisa ser registrada sem que cada módulo dependa do módulo de administração.

**Como.** Quem faz a ação publica um evento (o sujeito) e não sabe quem ouve. Os ouvintes (os observadores) reagem. O Spring faz o papel do registro de observadores (`ApplicationEventPublisher` e `@EventListener`).

```
LoginService ──publica──► LoginCodeRequestedEvent ──► LoginCodeEmailListener (depois do commit, @Async)
qualquer módulo ──publica──► AuditEvent ──► AuditRecorder (síncrono, na mesma transação)
```

Os dois ouvintes mostram as duas formas do padrão: o e-mail roda **depois do commit e em outra thread**, porque uma falha no envio não pode desfazer o login (D05). Já o histórico roda **na mesma transação**, porque uma ação sem histórico não pode ser gravada (RNF07, complemento da D10).

**Onde:** [`LoginCodeRequestedEvent.java`](../../src/main/java/br/ucsal/cade/access/api/event/LoginCodeRequestedEvent.java), [`LoginCodeEmailListener.java`](../../src/main/java/br/ucsal/cade/notifications/service/login/LoginCodeEmailListener.java), [`AuditEvent.java`](../../src/main/java/br/ucsal/cade/shared/audit/AuditEvent.java), [`AuditRecorder.java`](../../src/main/java/br/ucsal/cade/administration/service/audit/AuditRecorder.java); testes em [`AuditRecorderTest.java`](../../src/test/java/br/ucsal/cade/administration/service/audit/AuditRecorderTest.java) (commit grava, rollback não grava, sem transação falha).

**Próximos:** o `FoundItemRegisteredEvent` (PR #65) e o [`LossReportRegisteredEvent`](../../src/main/java/br/ucsal/cade/lossreports/api/event/LossReportRegisteredEvent.java) (PER-01) serão ouvidos pelo cruzamento (CRU-02), que calcula as sugestões em segundo plano. Pela D10, `lossreports` não pode chamar `matching` (é o cruzamento que depende das perdas), então o evento é o único caminho.

**Perguntas prováveis**
- *Por que eventos e não chamar o serviço de e-mail direto?* Pela D10, `access` não pode depender de `notifications`. O evento inverte a dependência: quem ouve conhece o evento, e quem publica não conhece ninguém.
- *Por que não RabbitMQ?* A D05 explica: num monólito, os eventos do Spring dão o mesmo desacoplamento sem um serviço a mais para subir na mostra.

---

## ★ Anotações e reflexão: autorização por perfil

**Problema.** Cada rota só pode ser usada por certos perfis (RF02). Por exemplo, um estudante não pode cadastrar objetos. O perigo é esquecer de proteger uma rota nova: ela ficaria aberta para qualquer um com um token.

**Como.**
1. **Uma anotação própria**, `@RequiresRole(Role.STAFF)`, vai na classe ou no método do controller. É o `@RequerPerfil` do RNF05.
2. **Ela é uma meta-anotação de `@PreAuthorize`**, então quem aplica a regra é o Spring Security. O enum `Role` implementa `ExpressionTemplateValueProvider` e vira o texto da expressão (`hasAnyRole('STAFF')`), sem ninguém escrever string à mão.
3. **A leitura por reflexão** fica no `RoleRequirement`, que procura a anotação no método, depois na classe e também nas interfaces.
4. **O teste de cobertura** percorre por reflexão todas as rotas registradas e quebra o build se alguma não for pública nem tiver `@RequiresRole`. O Swagger usa a mesma leitura para mostrar "Perfis:" em cada rota.

```java
@Retention(RetentionPolicy.RUNTIME)                  // sem isso, a reflexão não encontra a anotação
@Target({ElementType.TYPE, ElementType.METHOD})
@PreAuthorize("hasAnyRole({value})")
public @interface RequiresRole { Role[] value(); }
```

**Onde:** [`RequiresRole.java`](../../src/main/java/br/ucsal/cade/shared/security/authorization/RequiresRole.java), [`RoleRequirement.java`](../../src/main/java/br/ucsal/cade/shared/security/authorization/RoleRequirement.java), [`Role.java`](../../src/main/java/br/ucsal/cade/shared/security/Role.java), [`RoleOperationCustomizer.java`](../../src/main/java/br/ucsal/cade/shared/web/openapi/RoleOperationCustomizer.java), [`RouteAuthorizationCoverageTest.java`](../../src/test/java/br/ucsal/cade/shared/security/RouteAuthorizationCoverageTest.java). O passo a passo completo está em [autorizacao.md](../tecnico/autorizacao.md), e a decisão, na D20 (que substitui a D19).

**Perguntas prováveis**
- *Se o Spring já aplica a regra, onde está a reflexão de vocês?* No `RoleRequirement`, usado pelo Swagger e pelo teste que impede rota esquecida. A primeira versão (D19) fazia a checagem com um interceptor próprio; trocamos pelo mecanismo do Spring para não reimplementar segurança, e mantivemos a reflexão onde ela agrega.
- *O que é `RUNTIME`?* A retenção da anotação: sem ela, a anotação some depois da compilação e nem o Spring nem a reflexão a encontram.

---

## ★ Concorrência: lock otimista no limite de registros

**Problema.** Cada estudante pode ter no máximo 5 registros em andamento (RN05). Contar e depois gravar não basta: com 4 registros, duas requisições enviadas juntas contam 4 e gravam, e o estudante fica com 6.

**Como.** Uma linha por estudante (`loss_reporters`) com `@Version`. O registro lê essa linha com `OPTIMISTIC_FORCE_INCREMENT` antes de contar, e o commit sobe a versão. Das duas requisições, a segunda encontra a versão já alterada, falha e é desfeita inteira; o estudante recebe 409 "tente de novo".

**Onde:** [`LossReporter.java`](../../src/main/java/br/ucsal/cade/lossreports/persistence/reporter/LossReporter.java), [`LossReporterRepository.java`](../../src/main/java/br/ucsal/cade/lossreports/persistence/reporter/LossReporterRepository.java), [`LossReportService.java`](../../src/main/java/br/ucsal/cade/lossreports/service/report/LossReportService.java); fluxo com diagramas em [`limite-registros-abertos.md`](../tecnico/limite-registros-abertos.md); testes em [`LossReporterRepositoryTest.java`](../../src/test/java/br/ucsal/cade/lossreports/persistence/reporter/LossReporterRepositoryTest.java) (duas transações intercaladas) e [`LossReportServiceTest.java`](../../src/test/java/br/ucsal/cade/lossreports/service/report/LossReportServiceTest.java) (8 registros simultâneos em Virtual Threads terminam com exatamente 5).

**Perguntas prováveis**
- *Por que otimista e não pessimista?* Conflito de verdade é raro (o mesmo estudante enviando dois registros no mesmo instante). O otimista não deixa ninguém esperando; só quem perde a corrida tenta de novo.
- *Por que não um `@Version` no próprio registro?* Num cadastro, o registro ainda não existe: duas inserções não disputam nada. É preciso uma linha que as duas leiam.

---

## Virtual Threads

**Problema.** Enviar um e-mail pelo Gmail leva alguns segundos. Se isso acontecesse na mesma thread da requisição, o usuário esperaria o e-mail sair para ver a resposta.

**Como.** O `@EnableAsync` liga o `@Async`, e com `spring.threads.virtual.enabled: true` o Spring roda cada tarefa assíncrona (e cada requisição) numa Virtual Thread do Java 21+. Virtual Threads são baratas: dá para ter milhares esperando rede sem gastar uma thread do sistema operacional para cada uma.

**Onde:** [`AsyncConfiguration.java`](../../src/main/java/br/ucsal/cade/shared/event/AsyncConfiguration.java), [`application.yml`](../../src/main/resources/application.yml) (`spring.threads.virtual.enabled`) e o `@Async` do [`LoginCodeEmailListener.java`](../../src/main/java/br/ucsal/cade/notifications/service/login/LoginCodeEmailListener.java).

**Próximos:** o cruzamento (CRU-02) roda no mesmo esquema.

---

## ★ SOLID

**Inversão de dependência (D).** As regras dependem de interfaces, e a implementação concreta é injetada pelo Spring:

| Interface | Implementação | O que ganha |
|---|---|---|
| [`EmailSender`](../../src/main/java/br/ucsal/cade/shared/mail/EmailSender.java) (o `ServicoEmail` dos requisitos) | [`SmtpEmailSender`](../../src/main/java/br/ucsal/cade/shared/mail/SmtpEmailSender.java) | Trocar Gmail por Mailpit ou por uma caixa interna no modo demonstração (DEM) sem mexer em quem envia |
| `PhotoStorage` (o `ArmazenamentoFotos`, PR #65) | `LocalPhotoStorage` | Guardar fotos em disco hoje e em outro lugar amanhã |
| `java.time.Clock` ([`TimeConfiguration`](../../src/main/java/br/ucsal/cade/shared/time/TimeConfiguration.java)) | relógio real; nos testes, o `MutableClock` | Testar prazos (10 minutos do código, 90 dias do objeto) sem esperar |

**Aberto/fechado (O).** Todo erro de negócio herda de [`ApiException`](../../src/main/java/br/ucsal/cade/shared/exception/ApiException.java), e cada subclasse diz o próprio status HTTP (`BadRequestException`, `ConflictException`, `TooManyRequestsException`). Um único tratador, o [`ApiExceptionHandler`](../../src/main/java/br/ucsal/cade/shared/web/error/ApiExceptionHandler.java), transforma qualquer uma numa resposta em português. Um erro novo é uma classe nova, sem mudar o tratador.

**Substituição de Liskov (L).** A `InvalidStatusTransitionException` é uma `ConflictException`, que é uma `ApiException`: o tratador trata todas do mesmo jeito, sem `instanceof`.

**Segregação de interfaces (I).** O [`AuditRecordRepository`](../../src/main/java/br/ucsal/cade/administration/persistence/audit/AuditRecordRepository.java) estende `Repository`, e não `JpaRepository`, para expor só gravar e ler. O histórico nunca é apagado (RN19), e um teste confere que não existe nenhum método `delete`.

**Responsabilidade única (S).** O cadastro de objeto (PR #65) separa quem guarda a foto (`PhotoStorage`), quem decide a regra (`FoundItem`), quem coordena a transação (`FoundItemService`) e quem fala HTTP (`FoundItemController`).

**Pergunta provável:** *mostre um lugar onde a inversão de dependência ajudou de verdade.* Nos testes de login: o `MutableClock` adianta o relógio em 10 minutos para conferir que o código venceu, sem o teste esperar 10 minutos.

---

## Collections e Streams

- [`RoleRequirement`](../../src/main/java/br/ucsal/cade/shared/security/authorization/RoleRequirement.java): os perfis aceitos por uma rota, já com a hierarquia, num `EnumSet` montado por stream.
- [`AuthorizationConfiguration`](../../src/main/java/br/ucsal/cade/shared/security/authorization/AuthorizationConfiguration.java): a hierarquia de perfis do Spring (`ROLE_ADMIN > ROLE_STAFF`) gerada a partir do enum com `flatMap`, para a regra ficar num lugar só.
- [`FoundItemStatus.fromCode`](../../src/main/java/br/ucsal/cade/founditems/persistence/status/FoundItemStatus.java): a situação a partir do código gravado no banco.

**Próximos:** a ordenação e a filtragem das sugestões (CRU), as contagens do painel (ADM-06) e da página pública (PUB-02) são o uso principal previsto nos requisitos (seção 7.3).

---

## Records e enums com comportamento

- **Records** para dados imutáveis, validados no construtor: [`AuditEvent`](../../src/main/java/br/ucsal/cade/shared/audit/AuditEvent.java) recusa autor nulo e motivo vazio; [`LoginCodeRequestedEvent`](../../src/main/java/br/ucsal/cade/access/api/event/LoginCodeRequestedEvent.java) esconde o código no `toString`, para ele não vazar em log. Os DTOs da API também são records.
- **Enums com comportamento:** [`Role`](../../src/main/java/br/ucsal/cade/shared/security/Role.java) sabe a própria hierarquia (`ADMIN(STAFF)` e `satisfies`). Na PR #65, o `RiskLevel` sabe se pode ser aumentado (`canBeRaisedTo`, RN04), e o `PhotoType` reconhece cada formato de foto com um método por constante.

---

## GRASP e baixo acoplamento

- **Especialista na informação:** a regra fica na classe que tem os dados. A situação sabe as próprias transições (`FoundItemStatus`), o risco sabe se pode subir (`RiskLevel`), e o objeto confere a data e o risco ao ser cadastrado (`FoundItem.register`, PR #65).
- **Criador:** métodos de fábrica com nome dizem o que está sendo criado: `User.newStudent`, `User.newAdmin`, `AuditRecord.of` e `FoundItem.register`.
- **Baixo acoplamento:** o sistema é um monólito modular (D10). O [`ModuleBoundariesTest`](../../src/test/java/br/ucsal/cade/architecture/ModuleBoundariesTest.java) (ArchUnit) quebra o build se um módulo usar outro fora do pacote `api`, depender de um módulo não permitido ou criar ciclo.

---

## Planejados (ainda não estão no código)

| Conteúdo | Card | Ideia |
|---|---|---|
| Strategy | CRU-01 | Cada critério do cruzamento (categoria, data, local, palavras) é uma `CompatibilityCriterion`; um critério novo não muda o cálculo |
| Composite | CRU-01 | A nota combina os critérios como se fossem um critério só (RN07) |
| Strategy | RET-03 | O que conferir no balcão depende do risco do objeto (RN09) |

Se algum desses não entrar até a entrega, esta tabela fica como "limitação declarada", que o manual aceita.

---

## O que não usamos, e por quê

- **Servlets escritos à mão:** a API usa Spring MVC, que roda sobre o `DispatcherServlet`, e a segurança é uma cadeia de filtros de servlet (`SecurityFilterChain`). Não escrevemos servlets diretamente porque o framework já resolve roteamento, conversão de JSON e validação.
- **Clean Architecture:** a D10 explica. O objetivo é separar módulos, e isolar o domínio do Spring dobraria o código para uma equipe de quatro com prazo curto.
