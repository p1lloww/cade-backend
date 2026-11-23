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
| Dar e retirar o perfil de funcionário (RF03) | `User.grantStaff`, `User.revokeStaff`, chamados pelo `UserRoleService.changeRole` com o comando `ChangeRole` (perfil e motivo); rota `PUT /users/{id}/role` (só administrador) |
| Mudança de perfil recusada (já é funcionário, não é funcionário, conta do administrador) | `RoleChangeNotAllowedException` (`access.persistence.user`; HTTP 409, `ConflictException`) |
| Conta não encontrada | `UserNotFoundException` (`access.service.user`; HTTP 404, `NotFoundException`) |
| Lista de contas (gestão de funcionários) | `UserRoleService.search`, rota `GET /users` (só administrador, sempre ordenada por e-mail) |
| Código de login | `LoginCode` (tabela `login_codes`), gravado pelo `LoginCodeStore` (D18) |
| Bloqueio por tentativas erradas | `LoginCode.blockedUntil`, `LoginCodeStore.registerFailure` (D18) |
| Tempo de espera entre envios de código | `resend-interval` (`LoginCodeProperties.resendInterval`), conferido pelo `LoginCodeStore.save` |
| Token de acesso (JWT) | `AccessToken`, emitido pelo `AccessTokenIssuer` |
| Mensagem de erro da API | `ProblemDetail`, montado pelo `ApiExceptionHandler` (D17) |
| Objeto achado | `FoundItem` |
| Situação do objeto (Disponível, Reservado, Devolvido, Não reclamado, Destinado) | `FoundItemStatus`, sealed interface com os records `Available`, `Reserved`, `Returned`, `Unclaimed`, `Disposed` (gravados como `AVAILABLE`, `RESERVED`, `RETURNED`, `UNCLAIMED`, `DISPOSED`) |
| Convocar, liberar, entregar, marcar como não reclamado, destinar (ações sobre o objeto) | `reserve`, `release`, `handOver`, `markUnclaimed`, `dispose` |
| Transição proibida | `InvalidStatusTransitionException` (HTTP 409, `ConflictException`) |
| Contagens genéricas da página pública (RF41) | `PublicCountsService` (`publicpage`), a partir de `AvailableItemCounts` (`founditems.api`); rota pública `GET /public/found-item-counts` |
| Objetos disponíveis (para as contagens públicas: estão na sala e podem ser retirados) | situações Disponível e Não reclamado (`AvailableItemCountsService.IN_THE_ROOM`); Reservado fica de fora |
| Busca de objetos (RF10) | `FoundItemSearchService.search` com os filtros em `FoundItemSearch`; rota `GET /found-items` (funcionário) |
| Texto da busca | parâmetro `q`, em full-text (`FoundItemRepository.TEXT_VECTOR`, configuração `cade_pt` sem acentos e índice `found_items_text_idx`, criados pelo `FoundItemTextIndex`) |
| Opções dos filtros da busca (categorias, blocos e salas, meses, situações) | `SearchOptions`; rota `GET /found-items/search-options` |
| Mês em que o objeto foi achado (filtro) | parâmetro `month` (`YearMonth`, `yyyy-MM`) |
| Caixa do mês (local de guarda) | `StorageBox` (`StorageBox.suggestedFor(data)` sugere "Caixa Novembro/2026", RF05) |
| Categoria | `Category` (`shared.category`, D21); as iniciais são criadas pelo `DefaultCategoriesInitializer` |
| Risco (baixo, médio, alto, documento) | `RiskLevel` (`LOW`, `MEDIUM`, `HIGH`, `DOCUMENT`); só aumenta: `canBeRaisedTo` (RN04) |
| Cadastro de objeto | `FoundItemService.register`, rota `POST /found-items` |
| Descrição interna, detalhes reservados | `internalDescription`, `reservedDetails` (RF06, só funcionários) |
| Evento objeto.cadastrado | `FoundItemRegisteredEvent` (`founditems.api.event`) |
| Registro de perda | `LossReport` |
| Situação do registro (Aberto, Aguardando informações, Convocado, Concluído, Vencido, Cancelado, Recusado) | `LossReportStatus` (`OPEN`, `AWAITING_INFO`, `SUMMONED`, `COMPLETED`, `EXPIRED`, `CANCELLED`, `REFUSED`); Recusado vem da D26 |
| Detalhe que só o dono conhece | `secretDetail` |
| Pedir informações, retomar, convocar, liberar, concluir, vencer, cancelar (ações sobre o registro) | `requestInfo`, `resume`, `summon`, `release`, `complete`, `expire`, `cancel` |
| Registro em andamento (conta para o limite de 5, RN05) | `LossReportStatus.ACTIVE` (Aberto, Aguardando informações, Convocado) |
| Local da perda (bloco e sala) | `LossLocation` |
| Data ou período da perda, turno | `LostPeriod`, `Shift` (`MORNING`, `AFTERNOON`, `EVENING`) |
| Registro de perda (ação) | `LossReportService.register`, rota `POST /loss-reports` |
| Quem trouxe o objeto (RF07) | `FoundItem.broughtBy`; campo `broughtBy` do cadastro e rota `PUT /found-items/{id}/brought-by` |
| Objeto identificado: nome, e-mail ou documento encontrado nele (RF08) | `FoundItem.identify`, `identifiedName`, `identifiedEmail`, `identifiedDocument`; rotas `PUT` e `DELETE /found-items/{id}/identification` |
| Categorias gerenciáveis: nome, risco padrão e perguntas de conferência (RF44) | `Category.change`, `Category.verificationQuestions`, `CategoryAdminService`; rotas `POST /categories` e `PUT /categories/{id}` (administrador) |
| Prazos configuráveis: guarda, retirada e validade do registro (RF45) | `DeadlineSettings` (uma linha), `DeadlineValues`, `Deadlines` (leitura, `shared.settings`), `DeadlineSettingsService`; rotas `GET` e `PUT /settings/deadlines` (administrador) |
| Painel com contagens do serviço (RF46) | `DashboardService` (`administration`), porta `FoundItemOverview` (`founditems.api`); rota `GET /dashboard` (administrador) |
| Foto do registro de perda | `photoKey`, rota `GET /loss-reports/{id}/photo` (só o dono) |
| Meus registros (RF16) | `LossReportService.listOf`; rota `GET /loss-reports/mine` (estudante) |
| Situação em linguagem simples | `LossReportStatus.description()`, devolvida em `statusDescription` |
| Evento perda.registrada | `LossReportRegisteredEvent` (`lossreports.api.event`) |
| Lock do limite de registros | `LossReporter` (tabela `loss_reporters`) |
| Sugestão (par objeto × perda) | `MatchSuggestion` (tabela `match_suggestions`; um par tem no máximo uma) |
| Situação da sugestão (Nova, Em espera, Convocada, Concluída, Descartada, Recusada, Vencida, Encerrada) | `SuggestionStatus` (`NEW`, `ON_HOLD`, `SUMMONED`, `COMPLETED`, `DISCARDED`, `REFUSED`, `EXPIRED`, `CLOSED`) |
| Pôr em espera, retomar, convocar, descartar, concluir, recusar, vencer, encerrar (ações sobre a sugestão) | `putOnHold`, `resume`, `summon`, `discard`, `complete`, `refuse`, `expire`, `close` |
| Cruzamento (calcular as sugestões de um objeto ou de uma perda) | `MatchingService` (`matchFoundItem`, `matchLossReport`), disparado pelo `MatchingListener` depois do commit |
| Objetos que entram no cruzamento (Disponível, Não reclamado) | `MatchableFoundItems` (`founditems.api`), `FoundItemStatus.MATCHABLE` |
| Perdas que entram no cruzamento (Aberto, Aguardando informações) | `MatchableLossReports` (`lossreports.api`), `LossReportStatus.MATCHABLE` |
| Sugestão criada (histórico) | ação `MATCH_SUGGESTED`, autor `AuditEvent.SYSTEM_ACTOR` |
| Compatibilidade (nota de 0 a 100) | `Compatibility` (`score`), calculada pelo `CompatibilityScorer` |
| Faixa da compatibilidade (alta, média, baixa) | `CompatibilityBand` (`HIGH`, `MEDIUM`, `LOW`) |
| Par objeto × perda avaliado pelo cruzamento | `MatchCandidate` (`FoundItemProfile`, `LossReportProfile`, `Place`) |
| Categoria igual (obrigatória), data, local, palavras em comum | `SameCategoryGate`, `DateCriterion`, `LocationCriterion`, `WordsCriterion` |
| Soma dos critérios | `SumOfCriteria` |
| Pesos e faixas da compatibilidade | `MatchingProperties` (`cade.matching.*`) |
| Critério de compatibilidade (`CriterioCompatibilidade`) | `CompatibilityCriterion` |
| Sugestão ativa (aparece nas listas do funcionário: Nova, Em espera, Convocada) | `SuggestionStatus.ACTIVE` |
| Sugestões de um objeto ou de uma perda, comparação lado a lado, descarte | `SuggestionService` (`forFoundItem`, `forLossReport`, `find`, `discard`), rotas `GET /found-items/{id}/suggestions`, `GET /loss-reports/{id}/suggestions`, `GET /suggestions/{id}`, `POST /suggestions/{id}/discard` |
| Sugestão com o objeto e a perda lado a lado | `ComparedSuggestion` (service), `SuggestionResponseDTO` (resposta) |
| Objeto e perda como o funcionário os compara (RF23) | `ComparableFoundItems` (`founditems.api`), `ComparableLossReports` (`lossreports.api`) |
| Sugestão descartada (histórico) | ação `MATCH_DISCARDED`, com o motivo do funcionário |
| Convocação | `Summons` (tabela `summons`; uma por sugestão), criada pelo `SummonsService.summon`; rota `POST /suggestions/{id}/summon` (funcionário) |
| Modo demonstração, contas fictícias e login direto (RF47) | profile `demo`; `DemoAccount`, `DemoDataSeeder`; `DirectSignIn` e `SignedIn` (porta em `access.api`, só no profile); `FoundItemRegistration` e `LossReportRegistration` (portas de cadastro por código) |
| Situação da convocação (Ativa, Concluída, Recusada, Vencida) | `SummonsStatus` (`ACTIVE`, `COMPLETED`, `REFUSED`, `EXPIRED`); ações `complete`, `refuse`, `expire` |
| Convocar (a parte de cada módulo) | `FoundItemReservations.reserve` (`founditems.api`), `LossReportSummons.summon` (`lossreports.api`), `SuggestionSummons.pairOf` e `summon` (`matching.api`); `FoundItem.reserve`, `LossReport.summon`, `MatchSuggestion.summon` e `putOnHold` |
| Local, horário, prazo e o que levar (RF26) | `PickupInstructions` (`pickup.api`); valores de `cade.pickup.*` (`PickupProperties`) e texto por risco em `VerificationRule.whatToBring` |
| Prazo para retirar (RN15) | `Summons.expiresAt`, a partir de `cade.pickup.deadline` |
| Minhas convocações (onde, até quando e o que levar) | `SummonsService.activeOf`, `StudentSummons`; rota `GET /summons/mine` (estudante) |
| Evento convocacao.criada | `SummonsCreatedEvent` (`pickup.api.event`) |
| Convocação no histórico | ações `SUMMONS_CREATED`, `FOUND_ITEM_RESERVED`, `LOSS_REPORT_SUMMONED`, `MATCH_SUMMONED` e `MATCH_ON_HOLD` |
| Balcão (o que conferir, retirada e recusa) | `CounterService` (`pickup.service.counter`); rotas `GET /summons`, `GET /summons/{id}`, `POST /summons/{id}/pickup` e `POST /summons/{id}/refusal` (funcionário) |
| Atalho de balcão (retirada sem registro de perda, RF32) | `WalkInService` (`pickup.service.walkin`), `WalkInPickup` (tabela `walk_in_pickups`); rotas `GET /found-items/{id}/walk-in-check` e `POST /walk-in-pickups` (funcionário) |
| Quem retira pelo atalho (estudante, pessoa sem conta) | `WalkInHolder` com `HolderType` (`STUDENT`, `VISITOR`) |
| Entregar sem convocação (ação sobre o objeto) | `handOverWithoutSummons` (`FoundItemStatus`, `FoundItem`, `FoundItemWalkIns` em `founditems.api`) |
| Encerrar as sugestões do objeto entregue (RF33) | `FoundItemSuggestions.closeOpenOf` (`matching.api`) |
| Evento retirada.balcao.registrada | `WalkInPickupRegisteredEvent` (`pickup.api.event`) |
| Atalho de balcão no histórico | ações `FOUND_ITEM_HANDED_OVER_AT_COUNTER`, `MATCH_CLOSED` e `WALK_IN_PICKUP_REGISTERED` |
| Aviso por e-mail ao estudante (RF34, RF35) | `Notification` (tabela `notifications`), com `NotificationKind` (`SUMMONS_CREATED`, `SUMMONS_EXPIRING`, `PICKUP_REGISTERED`, `LOSS_REPORT_EXPIRING`) |
| Situação do aviso (Pendente, Enviado, Abandonado) | `NotificationStatus` (`PENDING`, `SENT`, `ABANDONED`) |
| Caixa de saída (Transactional Outbox, D30) | `Outbox.enqueue` (entrada), `OutboxRelay` e `NotificationDelivery` (saída) |
| Nova tentativa de envio (RNF08) | `OutboxRelay.relayPending`, `Notification.markFailed`; `cade.notifications.*` (`OutboxProperties`) |
| Lembrete de prazo (convocação vencendo, registro vencendo) | `DeadlineNoticeJob`; `SummonsDeadlines` (`pickup.api`), `LossReportDeadlines` (`lossreports.api`) |
| E-mail de uma conta | `UserEmails.emailOf` (`access.api`) |
| Estudante bloqueado (3 recusas no balcão, RN12) | `LossReporter.isBlocked()`; rota `GET /blocked-students` (administrador) |
| Liberar o estudante bloqueado | `LossReporter.release()`, `StudentReleaseService`; rota `POST /blocked-students/{studentId}/release`; ação `STUDENT_RELEASED` no histórico |
| Regra de conferência por risco (RN09) | `VerificationRule` (`LowRiskRule`, `MediumRiskRule`, `HighRiskRule`, `DocumentRule`), escolhida pelo `VerificationRules` |
| O que conferir (checklist do funcionário, RF28) | `VerificationRule.checklist()` |
| Verificação feita no balcão (descrição coerente, detalhe, desbloqueio, prova, foto do titular) | `VerificationMethod` (`DESCRIPTION_MATCHED`, `DETAIL_CONFIRMED`, `DEVICE_UNLOCKED`, `PROOF_OF_OWNERSHIP`, `HOLDER_PHOTO_MATCHED`) |
| Documento com foto (RG, CNH, carteirinha, passaporte, outro) | `IdDocumentType` (`RG`, `CNH`, `STUDENT_CARD`, `PASSPORT`, `OTHER`) |
| Matrícula | `enrollment` |
| Retirada | `Pickup` (tabela `pickups`; uma por convocação), com o que foi conferido em `CounterCheck` |
| Recusa no balcão | `Summons.refuse` (convocação Recusada, com `refusalReason`); `CounterService.refuse` |
| Recusas do estudante e bloqueio (RN12) | `LossReporter.refusals` e `isBlocked()` (3 recusas, `MAX_REFUSALS`); `StudentRefusals.record` (`lossreports.api`); `StudentBlockedException` |
| Fim da convocação (a parte de cada módulo) | `FoundItemReservations.handOver` e `release`, `LossReportSummons.complete`, `refuse` e `release` (este para o vencimento), `SuggestionSummons.complete`, `refuse` (`RefusedSuggestion`) e `expire` (`ExpiredSuggestion`); tarefa `SummonsExpiryJob` (vencimento da convocação, RF27) |
| Evento retirada.registrada | `PickupRegisteredEvent` (`pickup.api.event`) |
| Balcão no histórico | ações `SUMMONS_COMPLETED`, `PICKUP_REGISTERED`, `FOUND_ITEM_HANDED_OVER`, `LOSS_REPORT_COMPLETED`, `MATCH_COMPLETED`; na recusa, `SUMMONS_REFUSED`, `FOUND_ITEM_RELEASED`, `LOSS_REPORT_REFUSED`, `MATCH_REFUSED`, `MATCH_RESUMED`, `MATCH_CLOSED`, `STUDENT_REFUSAL_RECORDED` e `STUDENT_BLOCKED` |
| Destino de não reclamado (bazar, descarte, outro) | `Disposal` |
| Histórico | `AuditEvent` (contrato em `shared.audit`) |
| Perfil de funcionário dado ou retirado (no histórico) | ações `STAFF_ROLE_GRANTED` e `STAFF_ROLE_REVOKED`, com o motivo informado pelo administrador |
| E-mail (destinatário, assunto e texto) | `EmailMessage` (`shared.mail`) |
| Remetente dos e-mails | `cade.email.from` (`EmailProperties`, variável `MAIL_FROM`) |
| Aviso com o código de login | `LoginCodeEmail`, enviado pelo `LoginCodeEmailListener` ao receber o `LoginCodeRequestedEvent` |
| Registro do histórico | `AuditRecord` (tabela `audit_records`) |
| Gravação do histórico | `AuditRecorder` (ouvinte síncrono de `AuditEvent`; exige transação aberta) |
| Autor "sistema" (ação sem pessoa, como tarefa agendada) | `AuditEvent.SYSTEM_ACTOR` |

## Interfaces e anotações citadas nos requisitos

| Requisitos | Código |
|---|---|
| `ArmazenamentoFotos` | `PhotoStorage` (`shared.storage`, D21), implementado pelo `LocalPhotoStorage` (pasta `PHOTOS_DIR`) |
| `ServicoEmail` | `EmailSender` (`shared.mail`), implementado pelo `SmtpEmailSender` |
| `PublicadorEventos` | `EventPublisher` (`shared.event`), implementado pelo `SpringEventPublisher` (eventos do Spring, D05) |
| `CriterioCompatibilidade` | `CompatibilityCriterion` |
| `@RequerPerfil` | `@RequiresRole` (`shared.security.authorization`), meta-anotação de `@PreAuthorize` aplicada pelo Spring Security (D20); o administrador atende às rotas de funcionário (`Role.satisfies`, `RoleHierarchy`) |
