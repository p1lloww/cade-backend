package br.ucsal.cade.lossreports.api.event;

import java.time.LocalDate;
import java.util.UUID;

/** Não leva a descrição nem o detalhe secreto (RF14). */
public record LossReportRegisteredEvent(UUID lossReportId, UUID categoryId, LocalDate lostFrom, LocalDate lostUntil) {
}
