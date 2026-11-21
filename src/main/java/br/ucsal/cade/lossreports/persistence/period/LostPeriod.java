package br.ucsal.cade.lossreports.persistence.period;

import br.ucsal.cade.shared.exception.BadRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Uma data só é um período com início igual ao fim. */
@Embeddable
public record LostPeriod(

        @Column(nullable = false)
        LocalDate lostFrom,

        @Column(nullable = false)
        LocalDate lostUntil,

        @Enumerated(EnumType.STRING)
        @Column(length = 20)
        Shift shift) {

    public static final int MAX_DAYS = 30;

    static final String FUTURE_MESSAGE = "A data da perda não pode ser no futuro.";
    static final String INVERTED_MESSAGE = "O início do período não pode ser depois do fim.";
    static final String TOO_LONG_MESSAGE = "O período pode ter no máximo " + MAX_DAYS + " dias.";

    public static LostPeriod of(LocalDate lostFrom, LocalDate lostUntil, Shift shift, LocalDate today) {
        LocalDate until = lostUntil == null ? lostFrom : lostUntil;
        if (until.isAfter(today)) {
            throw new BadRequestException(FUTURE_MESSAGE);
        }
        if (lostFrom.isAfter(until)) {
            throw new BadRequestException(INVERTED_MESSAGE);
        }
        if (ChronoUnit.DAYS.between(lostFrom, until) > MAX_DAYS) {
            throw new BadRequestException(TOO_LONG_MESSAGE);
        }
        return new LostPeriod(lostFrom, until, shift);
    }
}
