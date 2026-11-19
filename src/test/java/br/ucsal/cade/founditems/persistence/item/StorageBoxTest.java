package br.ucsal.cade.founditems.persistence.item;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class StorageBoxTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "2026-01-01, Caixa Janeiro/2026",
            "2026-03-15, Caixa Março/2026",
            "2026-11-04, Caixa Novembro/2026",
            "2026-12-31, Caixa Dezembro/2026",
            "2027-01-01, Caixa Janeiro/2027"})
    void suggestsTheBoxOfTheMonth(LocalDate date, String expected) {
        assertThat(StorageBox.suggestedFor(date)).isEqualTo(expected);
    }

    @Test
    void everySuggestionFitsTheColumn() {
        for (int month = 1; month <= 12; month++) {
            assertThat(StorageBox.suggestedFor(LocalDate.of(2026, month, 1)))
                    .hasSizeLessThanOrEqualTo(StorageBox.MAX_LENGTH);
        }
    }
}
