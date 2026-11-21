package br.ucsal.cade.matching.service.score.criterion;

import br.ucsal.cade.matching.service.score.candidate.Candidates;
import br.ucsal.cade.matching.service.score.candidate.Place;

import org.junit.jupiter.api.Test;

import static br.ucsal.cade.matching.service.score.candidate.Candidates.DAY;
import static br.ucsal.cade.matching.service.score.candidate.Candidates.candidate;
import static br.ucsal.cade.matching.service.score.candidate.Candidates.found;
import static br.ucsal.cade.matching.service.score.candidate.Candidates.lost;
import static org.assertj.core.api.Assertions.assertThat;

class LocationCriterionTest {

    private final LocationCriterion criterion = new LocationCriterion(Candidates.properties().location());

    @Test
    void theSameRoomScores30() {
        assertThat(score(new Place("Bloco 400", "Sala 421"))).isEqualTo(30);
    }

    @Test
    void ignoresCaseAccentsAndExtraSpaces() {
        assertThat(score(new Place("  bloco  400 ", "SALA 421"))).isEqualTo(30);
        assertThat(scoreAt(new Place("Bloco Améric", "Auditório"), new Place("bloco americ", "auditorio")))
                .isEqualTo(30);
    }

    @Test
    void anotherRoomInTheSameBlockScores15() {
        assertThat(score(new Place("Bloco 400", "Sala 422"))).isEqualTo(15);
    }

    @Test
    void anUnknownRoomInTheSameBlockScores15() {
        assertThat(score(new Place("Bloco 400", null))).isEqualTo(15);
    }

    @Test
    void anotherBlockScoresNothing() {
        assertThat(score(new Place("Bloco B", "Sala 421"))).isZero();
    }

    @Test
    void theBestOfTheInformedPlacesCounts() {
        assertThat(score(new Place("Bloco B", null), new Place("Bloco 400", "Sala 420"),
                new Place("Bloco 400", "Sala 421"))).isEqualTo(30);
    }

    @Test
    void noPlaceInformedScoresNothing() {
        assertThat(score()).isZero();
    }

    private int score(Place... places) {
        return scoreAt(new Place("Bloco 400", "Sala 421"), places);
    }

    private int scoreAt(Place foundAt, Place... places) {
        return criterion.score(candidate(found(DAY, foundAt.block(), foundAt.room(), null),
                lost(DAY, DAY, "fone", places)));
    }
}
