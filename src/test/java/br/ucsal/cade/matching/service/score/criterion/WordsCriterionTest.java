package br.ucsal.cade.matching.service.score.criterion;

import br.ucsal.cade.matching.service.score.candidate.Candidates;

import org.junit.jupiter.api.Test;

import static br.ucsal.cade.matching.service.score.candidate.Candidates.DAY;
import static br.ucsal.cade.matching.service.score.candidate.Candidates.candidate;
import static br.ucsal.cade.matching.service.score.candidate.Candidates.found;
import static br.ucsal.cade.matching.service.score.candidate.Candidates.lost;
import static org.assertj.core.api.Assertions.assertThat;

class WordsCriterionTest {

    private final WordsCriterion criterion = new WordsCriterion(Candidates.properties().words());

    @Test
    void eachWordInCommonScores10() {
        assertThat(score("fone JBL preto", "JBL preto")).isEqualTo(20);
    }

    @Test
    void theCategoryNameDoesNotCount() {
        assertThat(score("fone preto", "fone JBL preto")).isEqualTo(10);
    }

    @Test
    void countsAtMostThreeWords() {
        assertThat(score("JBL preto adesivo estrela", "JBL preto adesivo estrela")).isEqualTo(30);
    }

    @Test
    void ignoresCaseAccentsAndPunctuation() {
        assertThat(score("Cinza-escuro, AZUL", "cinza escuro; azúl")).isEqualTo(30);
    }

    @Test
    void ignoresShortAndEmptyWords() {
        assertThat(score("de cor preta com um adesivo", "da cor branca com o logo")).isZero();
    }

    @Test
    void aRepeatedWordCountsOnce() {
        assertThat(score("preto preto preto", "preto")).isEqualTo(10);
    }

    @Test
    void anItemWithoutDescriptionScoresNothing() {
        assertThat(score(null, "fone JBL preto")).isZero();
    }

    private int score(String foundDescription, String lostDescription) {
        return criterion.score(candidate(found(DAY, "Bloco 400", "Sala 421", foundDescription),
                lost(DAY, DAY, lostDescription)));
    }
}
