package br.ucsal.cade.matching.service.score.criterion;

import br.ucsal.cade.matching.service.score.candidate.MatchCandidate;

/**
 * Um critério da compatibilidade entre um objeto achado e uma perda (RN07, Strategy). A nota inteira também é um
 * critério: a porta da categoria envolvendo a soma dos demais (Composite), montada pela
 * {@code MatchingConfiguration}.
 */
@FunctionalInterface
public interface CompatibilityCriterion {

    int score(MatchCandidate candidate);
}
