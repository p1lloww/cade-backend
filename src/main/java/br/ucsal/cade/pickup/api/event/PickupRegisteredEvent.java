package br.ucsal.cade.pickup.api.event;

import java.util.UUID;

/**
 * O estudante retirou o objeto no balcão (o {@code retirada.registrada} da seção 7.2 dos requisitos). Publicado na
 * transação da retirada: o e-mail de confirmação entra na caixa de saída nessa mesma transação e sai depois do commit
 * (D30). O encerramento das outras sugestões do objeto (RF33) é do RET-06.
 */
public record PickupRegisteredEvent(UUID pickupId, UUID summonsId, UUID studentId, UUID foundItemId,
                                    UUID lossReportId) {
}
