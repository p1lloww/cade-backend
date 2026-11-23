package br.ucsal.cade.lossreports.api.event;

import java.util.UUID;

/**
 * Passaram 5 dias sem resposta ao pedido de informações (RN13): o registro voltou a Aberto e a sugestão que originou
 * o pedido deve ser descartada. Publicado na transação em que o registro voltou.
 */
public record LossReportInfoOverdueEvent(UUID lossReportId, UUID suggestionId) {
}
