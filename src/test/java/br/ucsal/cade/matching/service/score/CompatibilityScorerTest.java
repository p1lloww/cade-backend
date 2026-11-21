package br.ucsal.cade.matching.service.score;

import br.ucsal.cade.matching.service.score.candidate.Candidates;
import br.ucsal.cade.matching.service.score.candidate.FoundItemProfile;
import br.ucsal.cade.matching.service.score.candidate.LossReportProfile;
import br.ucsal.cade.matching.service.score.candidate.MatchCandidate;
import br.ucsal.cade.matching.service.score.candidate.Place;
import br.ucsal.cade.matching.service.score.config.MatchingConfiguration;
import br.ucsal.cade.matching.service.score.config.MatchingProperties;
import br.ucsal.cade.matching.service.score.result.Compatibility;
import br.ucsal.cade.matching.service.score.result.CompatibilityBand;

import org.junit.jupiter.api.Test;

import java.util.List;

import static br.ucsal.cade.matching.service.score.candidate.Candidates.CELULAR;
import static br.ucsal.cade.matching.service.score.candidate.Candidates.DAY;
import static br.ucsal.cade.matching.service.score.candidate.Candidates.candidate;
import static br.ucsal.cade.matching.service.score.candidate.Candidates.found;
import static br.ucsal.cade.matching.service.score.candidate.Candidates.lost;
import static org.assertj.core.api.Assertions.assertThat;

class CompatibilityScorerTest {

    private final MatchingProperties properties = Candidates.properties();
    private final CompatibilityScorer scorer =
            new CompatibilityScorer(MatchingConfiguration.criteriaOf(properties), properties);

    @Test
    void theHeadphoneExampleOfRn07Scores80AndIsHigh() {
        MatchCandidate candidate = candidate(found(DAY, "Bloco 400", "Sala 421", "fone preto"),
                lost(DAY, DAY, "fone JBL preto", new Place("Bloco 400", "Sala 421")));

        Compatibility compatibility = scorer.score(candidate);

        assertThat(compatibility.score()).isEqualTo(80);
        assertThat(compatibility.band()).isEqualTo(CompatibilityBand.HIGH);
    }

    @Test
    void aReportWithOnlyIphoneScoresAtMost50AndIsMedium() {
        FoundItemProfile phone = new FoundItemProfile(CELULAR, "Celular", DAY, new Place("Bloco 400", "Sala 421"),
                "iPhone 13 azul com capinha transparente");
        LossReportProfile report = new LossReportProfile(CELULAR, DAY, DAY, List.of(), "iPhone");

        Compatibility compatibility = scorer.score(new MatchCandidate(phone, report));

        assertThat(compatibility.score()).isEqualTo(50);
        assertThat(compatibility.band()).isEqualTo(CompatibilityBand.MEDIUM);
    }

    @Test
    void aDifferentCategoryZeroesEverything() {
        FoundItemProfile phone = new FoundItemProfile(CELULAR, "Celular", DAY, new Place("Bloco 400", "Sala 421"),
                "fone preto");
        LossReportProfile headphone = lost(DAY, DAY, "fone JBL preto", new Place("Bloco 400", "Sala 421"));

        Compatibility compatibility = scorer.score(new MatchCandidate(phone, headphone));

        assertThat(compatibility.score()).isZero();
        assertThat(compatibility.band()).isEqualTo(CompatibilityBand.LOW);
        assertThat(compatibility.band().isShownByDefault()).isFalse();
    }

    @Test
    void everythingMatchingScores100() {
        MatchCandidate candidate = candidate(found(DAY, "Bloco 400", "Sala 421", "JBL preto adesivo estrela"),
                lost(DAY, DAY, "JBL preto com adesivo de estrela", new Place("Bloco 400", "Sala 421")));

        assertThat(scorer.score(candidate).score()).isEqualTo(100);
    }

    @Test
    void theBandsFollowTheThresholds() {
        assertThat(bandFor(70)).isEqualTo(CompatibilityBand.HIGH);
        assertThat(bandFor(69)).isEqualTo(CompatibilityBand.MEDIUM);
        assertThat(bandFor(40)).isEqualTo(CompatibilityBand.MEDIUM);
        assertThat(bandFor(39)).isEqualTo(CompatibilityBand.LOW);
    }

    @Test
    void theWeightsComeFromTheConfiguration() {
        MatchingProperties doubledRoom = new MatchingProperties(properties.date(),
                new MatchingProperties.Location(60, 15), properties.words(), properties.bands());
        CompatibilityScorer custom = new CompatibilityScorer(MatchingConfiguration.criteriaOf(doubledRoom), doubledRoom);
        MatchCandidate candidate = candidate(found(DAY.plusDays(30), "Bloco 400", "Sala 421", null),
                lost(DAY, DAY, "fone", new Place("Bloco 400", "Sala 421")));

        assertThat(scorer.score(candidate).score()).isEqualTo(30);
        assertThat(custom.score(candidate).score()).isEqualTo(60);
    }

    private CompatibilityBand bandFor(int score) {
        return new CompatibilityScorer(_ -> score, properties).score(null).band();
    }
}
