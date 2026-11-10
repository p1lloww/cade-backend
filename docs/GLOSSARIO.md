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
| Perfil (estudante, funcionário, administrador) | `Role` (`STUDENT`, `STAFF`, `ADMIN`) |
| Código de login | `LoginCode` |
| Objeto achado | `FoundItem` |
| Situação do objeto (Disponível, Reservado, Devolvido, Não reclamado, Destinado) | `FoundItemStatus` (`AVAILABLE`, `RESERVED`, `RETURNED`, `UNCLAIMED`, `DISPOSED`) |
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

## Interfaces e anotações citadas nos requisitos

| Requisitos | Código |
|---|---|
| `ArmazenamentoFotos` | `PhotoStorage` |
| `ServicoEmail` | `EmailSender` (`shared.mail`) |
| `PublicadorEventos` | `EventPublisher` (`shared.event`) |
| `CriterioCompatibilidade` | `CompatibilityCriterion` |
| `@RequerPerfil` | `@RequiresRole` (`shared.security`) |
