package br.ucsal.cade.lossreports.persistence.period;

import br.ucsal.cade.shared.exception.BadRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Quando o estudante acha que perdeu o objeto: uma data ou um período, e o turno se ele lembrar (RF13). Uma data só é
 * um período em que {@code lostFrom} e {@code lostUntil} são iguais.
 *
 * <p>Crie sempre por {@link #of}, que aplica as regras; o construtor do record existe para o Hibernate.
 *
 * @param shift turno; nulo quando o estudante não lembra
 */
@Embeddable
public record LostPeriod(

        @Column(nullable = false)
        LocalDate lostFrom,

        @Column(nullable = false)
        LocalDate lostUntil,

        @Enumerated(EnumType.STRING)
        @Column(length = 20)
        Shift shift) {

    /** Um período mais longo que isso deixa o cruzamento sem como comparar a data (RN07). */
    public static final int MAX_DAYS = 30;

    static final String FUTURE_MESSAGE = "A data da perda não pode ser no futuro.";
    static final String INVERTED_MESSAGE = "O início do período não pode ser depois do fim.";
    static final String TOO_LONG_MESSAGE = "O período pode ter no máximo " + MAX_DAYS + " dias.";

    /**
     * @param lostUntil fim do período; nulo quando o estudante informa uma data só
     * @param today     a data de hoje, para recusar datas no futuro
     * @throws BadRequestException se alguma data é futura, o período está invertido ou passa de {@value #MAX_DAYS} dias
     */
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
