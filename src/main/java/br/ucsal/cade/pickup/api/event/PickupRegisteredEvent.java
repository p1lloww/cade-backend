package br.ucsal.cade.pickup.api.event;

import java.util.UUID;

/**
 * O estudante retirou o objeto no balcão (o {@code retirada.registrada} da seção 7.2 dos requisitos). Publicado na
 * transação da retirada; o e-mail de confirmação (AVI-02) e o encerramento das outras sugestões do objeto (RF33) ouvem
 * depois do commit.
 */
public record PickupRegisteredEvent(UUID pickupId, UUID summonsId, UUID studentId, UUID foundItemId,
                                    UUID lossReportId) {
}
