package br.ucsal.cade.matching.service.score;

import br.ucsal.cade.matching.service.score.candidate.MatchCandidate;
import br.ucsal.cade.matching.service.score.config.MatchingProperties;
import br.ucsal.cade.matching.service.score.criterion.CompatibilityCriterion;
import br.ucsal.cade.matching.service.score.result.Compatibility;
import br.ucsal.cade.matching.service.score.result.CompatibilityBand;

import org.springframework.stereotype.Service;

/** Calcula a compatibilidade de um par objeto × perda (RF21). A sugestão é apoio: quem decide é o funcionário (RN08). */
@Service
public class CompatibilityScorer {

    private final CompatibilityCriterion criteria;
    private final MatchingProperties.Bands bands;

    CompatibilityScorer(CompatibilityCriterion compatibilityCriteria, MatchingProperties properties) {
        this.criteria = compatibilityCriteria;
        this.bands = properties.bands();
    }

    public Compatibility score(MatchCandidate candidate) {
        int score = criteria.score(candidate);
        return new Compatibility(score, bandOf(score));
    }

    /** A faixa de uma nota já gravada, pelas faixas atuais de {@code cade.matching.bands}. */
    public CompatibilityBand bandOf(int score) {
        if (score >= bands.high()) {
            return CompatibilityBand.HIGH;
        }
        return score >= bands.medium() ? CompatibilityBand.MEDIUM : CompatibilityBand.LOW;
    }
}
