package br.ucsal.cade.matching.service.score.criterion;

import br.ucsal.cade.matching.service.score.candidate.Candidates;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static br.ucsal.cade.matching.service.score.candidate.Candidates.candidate;
import static br.ucsal.cade.matching.service.score.candidate.Candidates.found;
import static br.ucsal.cade.matching.service.score.candidate.Candidates.lost;
import static org.assertj.core.api.Assertions.assertThat;

class DateCriterionTest {

    private final DateCriterion criterion = new DateCriterion(Candidates.properties().date());

    @ParameterizedTest(name = "perda em 04/11, achado em {0}: {1} pontos")
    @CsvSource({
            "2026-11-02, 0",   // 2 dias antes: não pode ser o mesmo objeto
            "2026-11-03, 40",  // 1 dia antes: tolerância
            "2026-11-04, 40",
            "2026-11-07, 40",  // 3 dias depois
            "2026-11-08, 20",  // 4 dias depois
            "2026-11-18, 20",  // 14 dias depois
            "2026-11-19, 0"    // 15 dias depois
    })
    void aSingleDate(LocalDate foundOn, int points) {
        LocalDate day = LocalDate.of(2026, 11, 4);

        assertThat(score(foundOn, day, day)).isEqualTo(points);
    }

    @ParameterizedTest(name = "perda de 03/11 a 05/11, achado em {0}: {1} pontos")
    @CsvSource({
            "2026-11-01, 0",   // 2 dias antes do início
            "2026-11-02, 40",  // tolerância antes do início
            "2026-11-04, 40",  // dentro: a duração (2 dias) é a incerteza
            "2026-11-08, 40",  // 3 dias depois do fim
            "2026-11-09, 20",  // 4 dias depois do fim
            "2026-11-19, 20",  // 14 dias depois do fim
            "2026-11-20, 0"    // 15 dias depois do fim
    })
    void aShortPeriod(LocalDate foundOn, int points) {
        assertThat(score(foundOn, LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 5))).isEqualTo(points);
    }

    @ParameterizedTest(name = "perda de {1} a {2}, achado em {0}: {3} pontos")
    @CsvSource({
            "2026-11-05, 2026-11-01, 2026-11-10, 20",  // 9 dias de incerteza, mesmo achado dentro
            "2026-11-20, 2026-11-01, 2026-11-10, 20",  // 10 dias depois do fim
            "2026-11-15, 2026-11-01, 2026-11-30, 0",   // 29 dias de incerteza: a data não pontua
            "2026-11-30, 2026-11-01, 2026-11-30, 0"
    })
    void aLongPeriodIsUncertain(LocalDate foundOn, LocalDate from, LocalDate until, int points) {
        assertThat(score(foundOn, from, until)).isEqualTo(points);
    }

    private int score(LocalDate foundOn, LocalDate from, LocalDate until) {
        return criterion.score(candidate(found(foundOn, "Bloco 400", "Sala 421", null), lost(from, until, "fone")));
    }
}
