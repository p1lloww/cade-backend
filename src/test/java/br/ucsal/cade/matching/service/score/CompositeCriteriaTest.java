package br.ucsal.cade.matching.service.score;

import org.junit.jupiter.api.Test;

import java.util.List;

import static br.ucsal.cade.matching.service.score.Candidates.CELULAR;
import static br.ucsal.cade.matching.service.score.Candidates.DAY;
import static br.ucsal.cade.matching.service.score.Candidates.candidate;
import static br.ucsal.cade.matching.service.score.Candidates.found;
import static br.ucsal.cade.matching.service.score.Candidates.lost;
import static org.assertj.core.api.Assertions.assertThat;

class CompositeCriteriaTest {

    private final MatchCandidate sameCategory = candidate(found(DAY, "Bloco 400", "Sala 421", null),
            lost(DAY, DAY, "fone"));

    @Test
    void theSumAddsEveryCriterion() {
        assertThat(new SumOfCriteria(List.of(_ -> 10, _ -> 25, _ -> 5)).score(sameCategory)).isEqualTo(40);
    }

    @Test
    void theSumNeverPasses100() {
        assertThat(new SumOfCriteria(List.of(_ -> 80, _ -> 80)).score(sameCategory)).isEqualTo(100);
    }

    @Test
    void aNewCriterionEntersWithoutChangingTheOthers() {
        CompatibilityCriterion sum = new SumOfCriteria(List.of(_ -> 40, new SumOfCriteria(List.of(_ -> 10))));

        assertThat(sum.score(sameCategory)).isEqualTo(50);
    }

    @Test
    void theGateLetsTheSameCategoryThrough() {
        assertThat(new SameCategoryGate(_ -> 70).score(sameCategory)).isEqualTo(70);
    }

    @Test
    void theGateZeroesAnotherCategory() {
        FoundItemProfile phone = new FoundItemProfile(CELULAR, "Celular", DAY, new Place("Bloco 400", "Sala 421"), null);

        assertThat(new SameCategoryGate(_ -> 70).score(new MatchCandidate(phone, lost(DAY, DAY, "fone")))).isZero();
    }
}
