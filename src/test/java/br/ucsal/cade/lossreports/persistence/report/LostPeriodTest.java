package br.ucsal.cade.lossreports.persistence.report;

import br.ucsal.cade.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LostPeriodTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 19);

    @Test
    void aSingleDateIsAPeriodOfOneDay() {
        LostPeriod period = LostPeriod.of(TODAY.minusDays(2), null, Shift.EVENING, TODAY);

        assertThat(period.lostFrom()).isEqualTo(TODAY.minusDays(2));
        assertThat(period.lostUntil()).isEqualTo(TODAY.minusDays(2));
        assertThat(period.shift()).isEqualTo(Shift.EVENING);
    }

    @Test
    void acceptsAPeriodEndingToday() {
        LostPeriod period = LostPeriod.of(TODAY.minusDays(5), TODAY, null, TODAY);

        assertThat(period.lostUntil()).isEqualTo(TODAY);
        assertThat(period.shift()).isNull();
    }

    @Test
    void refusesADateInTheFuture() {
        assertThatThrownBy(() -> LostPeriod.of(TODAY.plusDays(1), null, null, TODAY))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(LostPeriod.FUTURE_MESSAGE);
        assertThatThrownBy(() -> LostPeriod.of(TODAY, TODAY.plusDays(1), null, TODAY))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(LostPeriod.FUTURE_MESSAGE);
    }

    @Test
    void refusesAnInvertedPeriod() {
        assertThatThrownBy(() -> LostPeriod.of(TODAY.minusDays(1), TODAY.minusDays(3), null, TODAY))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(LostPeriod.INVERTED_MESSAGE);
    }

    @Test
    void acceptsUpToThirtyDaysAndRefusesMore() {
        assertThat(LostPeriod.of(TODAY.minusDays(30), TODAY, null, TODAY).lostFrom()).isEqualTo(TODAY.minusDays(30));
        assertThatThrownBy(() -> LostPeriod.of(TODAY.minusDays(31), TODAY, null, TODAY))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(LostPeriod.TOO_LONG_MESSAGE);
    }
}
