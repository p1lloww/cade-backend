package br.ucsal.cade.lossreports.api.event;

import java.util.UUID;

/**
 * O funcionário pediu mais informações ao estudante (o {@code perda.informacao-pedida} da seção 7.2 dos requisitos).
 * Publicado na transação do pedido: o e-mail entra na caixa de saída nessa mesma transação (D30).
 *
 * @param requestId identifica este pedido (um registro pode ter vários, um de cada vez); é a referência do aviso
 * @param reason    o que o funcionário precisa saber, escrito para o estudante
 */
public record LossReportInfoRequestedEvent(UUID requestId, UUID lossReportId, UUID studentId, String reason) {
}
