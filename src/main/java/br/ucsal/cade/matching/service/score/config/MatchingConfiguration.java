package br.ucsal.cade.matching.service.score.config;

import br.ucsal.cade.matching.service.score.ai.AiMatchingProperties;
import br.ucsal.cade.matching.service.score.ai.AiSimilarityCriterion;
import br.ucsal.cade.matching.service.score.ai.AiTextSimilarity;
import br.ucsal.cade.matching.service.score.composite.SameCategoryGate;
import br.ucsal.cade.matching.service.score.composite.SumOfCriteria;
import br.ucsal.cade.matching.service.score.criterion.CompatibilityCriterion;
import br.ucsal.cade.matching.service.score.criterion.DateCriterion;
import br.ucsal.cade.matching.service.score.criterion.LocationCriterion;
import br.ucsal.cade.matching.service.score.criterion.WordsCriterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Monta a nota da RN07: a porta da categoria envolvendo a soma de data, local e palavras. Um critério novo entra na
 * lista sem mudar os outros: a IA do CRU-05 entra assim, só quando está ligada e há uma implementação dela.
 */
@Configuration
@EnableConfigurationProperties({MatchingProperties.class, AiMatchingProperties.class})
public class MatchingConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MatchingConfiguration.class);

    @Bean
    CompatibilityCriterion compatibilityCriteria(MatchingProperties properties, AiMatchingProperties ai,
                                                 ObjectProvider<AiTextSimilarity> aiProvider) {
        List<CompatibilityCriterion> extra = new ArrayList<>();
        if (ai.enabled()) {
            AiTextSimilarity similarity = aiProvider.getIfAvailable();
            if (similarity == null) {
                log.warn("cade.matching.ai.enabled is true but there is no AiTextSimilarity bean: the AI criterion "
                        + "stays off");
            } else {
                extra.add(new AiSimilarityCriterion(similarity, ai.timeout(), ai.maxPoints()));
            }
        }
        return criteriaOf(properties, extra);
    }

    public static CompatibilityCriterion criteriaOf(MatchingProperties properties) {
        return criteriaOf(properties, List.of());
    }

    /** Os critérios da RN07 mais os extras, na mesma soma e atrás da mesma porta da categoria. */
    public static CompatibilityCriterion criteriaOf(MatchingProperties properties,
                                                    List<CompatibilityCriterion> extra) {
        List<CompatibilityCriterion> criteria = new ArrayList<>(List.of(
                new DateCriterion(properties.date()),
                new LocationCriterion(properties.location()),
                new WordsCriterion(properties.words())));
        criteria.addAll(extra);
        return new SameCategoryGate(new SumOfCriteria(criteria));
    }
}
