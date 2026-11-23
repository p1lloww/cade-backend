package br.ucsal.cade.lossreports.api.event;

import java.time.LocalDate;
import java.util.UUID;

/**
 * O estudante corrigiu um registro de perda (PER-07). Publicado na transação da edição, só quando algum campo mudou.
 * Como o {@link LossReportRegisteredEvent}, não leva a descrição nem o detalhe secreto (RF14).
 */
public record LossReportUpdatedEvent(UUID lossReportId, UUID categoryId, LocalDate lostFrom, LocalDate lostUntil) {
}
