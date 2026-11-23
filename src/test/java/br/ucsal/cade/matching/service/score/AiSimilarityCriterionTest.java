package br.ucsal.cade.matching.service.score;

import br.ucsal.cade.matching.service.score.ai.AiSimilarityCriterion;
import br.ucsal.cade.matching.service.score.ai.AiTextSimilarity;
import br.ucsal.cade.matching.service.score.ai.PersonalDataRedactor;
import br.ucsal.cade.matching.service.score.candidate.Candidates;
import br.ucsal.cade.matching.service.score.candidate.FoundItemProfile;
import br.ucsal.cade.matching.service.score.candidate.LossReportProfile;
import br.ucsal.cade.matching.service.score.candidate.MatchCandidate;
import br.ucsal.cade.matching.service.score.candidate.Place;
import br.ucsal.cade.matching.service.score.config.MatchingConfiguration;
import br.ucsal.cade.matching.service.score.config.MatchingProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static br.ucsal.cade.matching.service.score.candidate.Candidates.CELULAR;
import static br.ucsal.cade.matching.service.score.candidate.Candidates.DAY;
import static br.ucsal.cade.matching.service.score.candidate.Candidates.candidate;
import static br.ucsal.cade.matching.service.score.candidate.Candidates.found;
import static br.ucsal.cade.matching.service.score.candidate.Candidates.lost;
import static org.assertj.core.api.Assertions.assertThat;

/** A IA como critério extra (RF25): tempo limite, falha isolada, sem dado pessoal e sem mudar os outros critérios. */
class AiSimilarityCriterionTest {

    private static final Duration TIMEOUT = Duration.ofMillis(200);
    private static final int MAX_POINTS = 15;

    private final MatchingProperties properties = Candidates.properties();
    private final MatchCandidate candidate = candidate(found(DAY, "Bloco 400", "Sala 421", "fone preto"),
            lost(DAY, DAY, "fone JBL preto", new Place("Bloco 400", "Sala 421")));

    @Test
    void theSimilarityBecomesPointsUpToTheMaximum() {
        assertThat(criterion((a, b) -> 0.8).score(candidate)).isEqualTo(12);
        assertThat(criterion((a, b) -> 1.0).score(candidate)).isEqualTo(MAX_POINTS);
        assertThat(criterion((a, b) -> 7.0).score(candidate)).isEqualTo(MAX_POINTS);
        assertThat(criterion((a, b) -> -1.0).score(candidate)).isZero();
        assertThat(criterion((a, b) -> Double.NaN).score(candidate)).isZero();
    }

    @Test
    void aSlowAiIsCutAtTheTimeoutAndScoresZero() {
        long start = System.nanoTime();

        int points = criterion((a, b) -> {
            sleep(5_000);
            return 1.0;
        }).score(candidate);

        assertThat(points).isZero();
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(2));
    }

    @Test
    void aFailingAiScoresZeroAndTheOtherCriteriaKeepTheirScore() {
        AiTextSimilarity broken = (a, b) -> {
            throw new IllegalStateException("serviço fora do ar");
        };
        CompatibilityScorer withBrokenAi = new CompatibilityScorer(MatchingConfiguration.criteriaOf(properties,
                List.of(new AiSimilarityCriterion(broken, TIMEOUT, MAX_POINTS))), properties);
        CompatibilityScorer withoutAi = new CompatibilityScorer(MatchingConfiguration.criteriaOf(properties),
                properties);

        assertThat(withBrokenAi.score(candidate).score()).isEqualTo(withoutAi.score(candidate).score()).isEqualTo(80);
    }

    @Test
    void aWorkingAiAddsItsPointsWithoutChangingTheOthersAndTheTotalStaysAt100() {
        CompatibilityScorer withAi = new CompatibilityScorer(MatchingConfiguration.criteriaOf(properties,
                List.of(new AiSimilarityCriterion((a, b) -> 0.8, TIMEOUT, MAX_POINTS))), properties);
        MatchCandidate everything = candidate(found(DAY, "Bloco 400", "Sala 421", "JBL preto adesivo estrela"),
                lost(DAY, DAY, "JBL preto com adesivo de estrela", new Place("Bloco 400", "Sala 421")));

        assertThat(withAi.score(candidate).score()).isEqualTo(92);
        assertThat(withAi.score(everything).score()).isEqualTo(100);
    }

    @Test
    void aDifferentCategoryNeverCallsTheAi() {
        AtomicInteger calls = new AtomicInteger();
        CompatibilityScorer withAi = new CompatibilityScorer(MatchingConfiguration.criteriaOf(properties,
                List.of(new AiSimilarityCriterion((a, b) -> {
                    calls.incrementAndGet();
                    return 1.0;
                }, TIMEOUT, MAX_POINTS))), properties);
        FoundItemProfile phone = new FoundItemProfile(CELULAR, "Celular", DAY, new Place("Bloco 400", "Sala 421"),
                "fone preto");

        assertThat(withAi.score(new MatchCandidate(phone, lost(DAY, DAY, "fone JBL preto",
                new Place("Bloco 400", "Sala 421")))).score()).isZero();
        assertThat(calls).hasValue(0);
    }

    @Test
    void noPersonalDataReachesTheAiAndBlankDescriptionsDoNotCallIt() {
        List<String> received = new ArrayList<>();
        AiSimilarityCriterion criterion = criterion((a, b) -> {
            received.add(a);
            received.add(b);
            return 0.5;
        });
        FoundItemProfile item = new FoundItemProfile(Candidates.FONE, "Fone", DAY,
                new Place("Bloco 400", "Sala 421"), "Fone preto, contato maria.souza@ucsal.edu.br");
        LossReportProfile report = new LossReportProfile(item.categoryId(), DAY, DAY, List.of(),
                "Fone JBL, liguem para (71) 99999-1234 ou matrícula 202012345");

        criterion.score(new MatchCandidate(item, report));

        assertThat(received).hasSize(2);
        assertThat(received.get(0)).contains("Fone preto").doesNotContain("maria").doesNotContain("ucsal");
        assertThat(received.get(1)).contains("Fone JBL").doesNotContain("99999").doesNotContain("202012345");

        received.clear();
        FoundItemProfile noDescription = new FoundItemProfile(item.categoryId(), "Fone", DAY,
                new Place("Bloco 400", "Sala 421"), null);
        assertThat(criterion.score(new MatchCandidate(noDescription, report))).isZero();
        assertThat(received).isEmpty();
    }

    @Test
    void theRedactorKeepsWhatDescribesTheObject() {
        assertThat(PersonalDataRedactor.redact("Garrafa azul 500 ml, adesivo de estrela"))
                .isEqualTo("Garrafa azul 500 ml, adesivo de estrela");
        assertThat(PersonalDataRedactor.redact(null)).isEmpty();
    }

    private static AiSimilarityCriterion criterion(AiTextSimilarity ai) {
        return new AiSimilarityCriterion(ai, TIMEOUT, MAX_POINTS);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
