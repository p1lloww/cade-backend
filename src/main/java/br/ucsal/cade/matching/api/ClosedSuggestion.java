package br.ucsal.cade.matching.api;

import java.util.UUID;

/**
 * Uma sugestão encerrada porque o objeto dela foi entregue (RF33).
 *
 * @param suggestionId  a sugestão encerrada
 * @param lossReportId  o registro de perda que ela sugeria, para avisar o estudante dono dele
 */
public record ClosedSuggestion(UUID suggestionId, UUID lossReportId) {
}
