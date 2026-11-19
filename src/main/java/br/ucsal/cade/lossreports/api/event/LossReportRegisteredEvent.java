package br.ucsal.cade.lossreports.api.event;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Um estudante registrou uma perda (o {@code perda.registrada} da seção 7.2 dos requisitos). Publicado na transação
 * do registro; o cruzamento com os objetos disponíveis ouve depois do commit (CRU), sem atrasar o estudante.
 *
 * <p>Leva só o necessário para o cruzamento achar os objetos candidatos; a descrição e o detalhe secreto não saem do
 * módulo (RF14).
 *
 * @param lostFrom  início do período em que o estudante acha que perdeu
 * @param lostUntil fim do período; igual a {@code lostFrom} quando ele informou uma data só
 */
public record LossReportRegisteredEvent(UUID lossReportId, UUID categoryId, LocalDate lostFrom, LocalDate lostUntil) {

    public LossReportRegisteredEvent {
        Objects.requireNonNull(lossReportId);
        Objects.requireNonNull(categoryId);
        Objects.requireNonNull(lostFrom);
        Objects.requireNonNull(lostUntil);
    }
}
