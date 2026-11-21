package br.ucsal.cade.matching.service.score;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Monta a nota da RN07: a porta da categoria envolvendo a soma de data, local e palavras. Um critério novo (como a
 * IA do CRU-05) entra na lista sem mudar os outros.
 */
@Configuration
@EnableConfigurationProperties(MatchingProperties.class)
class MatchingConfiguration {

    @Bean
    CompatibilityCriterion compatibilityCriteria(MatchingProperties properties) {
        return criteriaOf(properties);
    }

    static CompatibilityCriterion criteriaOf(MatchingProperties properties) {
        return new SameCategoryGate(new SumOfCriteria(List.of(
                new DateCriterion(properties.date()),
                new LocationCriterion(properties.location()),
                new WordsCriterion(properties.words()))));
    }
}
