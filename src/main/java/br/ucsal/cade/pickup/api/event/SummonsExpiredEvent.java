package br.ucsal.cade.pickup.api.event;

import java.util.UUID;

/**
 * A convocação venceu sem retirada (o {@code convocacao.vencida} da seção 7.2 dos requisitos). Publicado na transação
 * do vencimento: o e-mail ao estudante entra na caixa de saída nessa mesma transação e sai depois do commit (D30).
 */
public record SummonsExpiredEvent(UUID summonsId, UUID studentId, UUID foundItemId, UUID lossReportId) {
}
