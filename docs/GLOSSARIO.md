# Glossário — termos dos requisitos e nomes no código

O código do back-end é em inglês (D11). Esta tabela liga os termos de [requisitos.md](requisitos.md) aos nomes usados no código. Termo novo entra aqui no mesmo PR que cria a classe.

## Módulos (D10)

| Requisitos | Pacote |
|---|---|
| M1 Acesso | `access` |
| M2 Achados | `founditems` |
| M3 Perdas | `lossreports` |
| M4 Cruzamento | `matching` |
| M5 Convocação e retirada | `pickup` |
| M6 Avisos | `notifications` |
| M7 Prazos e destino | `retention` |
| M8 Página pública | `publicpage` |
| M9 Administração | `administration` |
| M10 Modo demonstração | `demo` |
| Núcleo compartilhado | `shared` |

## Conceitos

| Requisitos | Código |
|---|---|
| Usuário | `User` |
| Perfil (estudante, funcionário, administrador) | `Role` (`STUDENT`, `STAFF`, `ADMIN`), em `shared.security` porque `@RequiresRole` é usada por todos os módulos |
| Administrador inicial (criado na instalação) | `InitialAdminInitializer` (variável `ADMIN_EMAIL`) |
| Código de login | `LoginCode` (tabela `login_codes`), gravado pelo `LoginCodeStore` (D18) |
| Bloqueio por tentativas erradas | `LoginCode.blockedUntil`, `LoginCodeStore.registerFailure` (D18) |
| Tempo de espera entre envios de código | `resend-interval` (`LoginCodeProperties.resendInterval`), conferido pelo `LoginCodeStore.save` |
| Token de acesso (JWT) | `AccessToken`, emitido pelo `AccessTokenIssuer` |
| Mensagem de erro da API | `ProblemDetail`, montado pelo `ApiExceptionHandler` (D17) |
| Objeto achado | `FoundItem` |
| Situação do objeto (Disponível, Reservado, Devolvido, Não reclamado, Destinado) | `FoundItemStatus`, sealed interface com os records `Available`, `Reserved`, `Returned`, `Unclaimed`, `Disposed` (gravados como `AVAILABLE`, `RESERVED`, `RETURNED`, `UNCLAIMED`, `DISPOSED`) |
| Convocar, liberar, entregar, marcar como não reclamado, destinar (ações sobre o objeto) | `reserve`, `release`, `handOver`, `markUnclaimed`, `dispose` |
| Transição proibida | `InvalidStatusTransitionException` (HTTP 409, `ConflictException`) |
| Caixa do mês (local de guarda) | `StorageBox` |
| Categoria | `Category` |
| Risco (baixo, médio, alto, documento) | `RiskLevel` (`LOW`, `MEDIUM`, `HIGH`, `DOCUMENT`) |
| Registro de perda | `LossReport` |
| Situação do registro (Aberto, Aguardando informações, Convocado, Concluído, Vencido, Cancelado) | `LossReportStatus` (`OPEN`, `AWAITING_INFO`, `SUMMONED`, `COMPLETED`, `EXPIRED`, `CANCELLED`) |
| Detalhe que só o dono conhece | `secretDetail` |
| Sugestão (par objeto × perda) | `MatchSuggestion` |
| Situação da sugestão (Nova, Em espera, Convocada, Concluída, Descartada, Recusada, Vencida, Encerrada) | `SuggestionStatus` (`NEW`, `ON_HOLD`, `SUMMONED`, `COMPLETED`, `DISCARDED`, `REFUSED`, `EXPIRED`, `CLOSED`) |
| Compatibilidade (nota de 0 a 100) | `score` |
| Critério de compatibilidade (`CriterioCompatibilidade`) | `CompatibilityCriterion` |
| Convocação | `Summons` |
| Retirada | `Pickup` |
| Recusa no balcão | `PickupRefusal` |
| Destino de não reclamado (bazar, descarte, outro) | `Disposal` |
| Histórico | `AuditEvent` (contrato em `shared.audit`) |
| E-mail (destinatário, assunto e texto) | `EmailMessage` (`shared.mail`) |
| Remetente dos e-mails | `cade.email.from` (`EmailProperties`, variável `MAIL_FROM`) |
| Aviso com o código de login | `LoginCodeEmail`, enviado pelo `LoginCodeEmailListener` ao receber o `LoginCodeRequestedEvent` |

## Interfaces e anotações citadas nos requisitos

| Requisitos | Código |
|---|---|
| `ArmazenamentoFotos` | `PhotoStorage` |
| `ServicoEmail` | `EmailSender` (`shared.mail`), implementado pelo `SmtpEmailSender` |
| `PublicadorEventos` | `EventPublisher` (`shared.event`) |
| `CriterioCompatibilidade` | `CompatibilityCriterion` |
| `@RequerPerfil` | `@RequiresRole` (`shared.security.authorization`), lida pelo `RoleInterceptor`; o administrador atende às rotas de funcionário (`Role.satisfies`) |
