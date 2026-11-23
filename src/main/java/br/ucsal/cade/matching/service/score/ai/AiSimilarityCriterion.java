package br.ucsal.cade.matching.service.score.ai;

import br.ucsal.cade.matching.service.score.candidate.MatchCandidate;
import br.ucsal.cade.matching.service.score.criterion.CompatibilityCriterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Critério extra da compatibilidade: uma IA compara as descrições e a semelhança vira pontos, até {@code maxPoints}
 * (RF25). Entra na soma dos critérios sem mudar nenhum dos outros (Strategy, aberto a extensão).
 *
 * <p>A chamada roda numa Virtual Thread e o critério espera só até o {@code timeout}. Falha, demora ou resposta
 * inválida valem 0 pontos: a nota dos outros critérios nunca é afetada nem o cruzamento trava. Só vão para a IA as
 * descrições, já sem dados pessoais (RNF04); a descrição do estudante é a pública, nunca o detalhe reservado.
 */
public final class AiSimilarityCriterion implements CompatibilityCriterion {

    private static final Logger log = LoggerFactory.getLogger(AiSimilarityCriterion.class);

    private final AiTextSimilarity ai;
    private final Duration timeout;
    private final int maxPoints;

    public AiSimilarityCriterion(AiTextSimilarity ai, Duration timeout, int maxPoints) {
        this.ai = Objects.requireNonNull(ai);
        this.timeout = Objects.requireNonNull(timeout);
        this.maxPoints = maxPoints;
    }

    @Override
    public int score(MatchCandidate candidate) {
        String found = PersonalDataRedactor.redact(candidate.foundItem().description());
        String lost = PersonalDataRedactor.redact(candidate.lossReport().description());
        if (found.isBlank() || lost.isBlank()) {
            return 0;
        }
        FutureTask<Double> call = new FutureTask<>(() -> ai.similarity(found, lost));
        Thread.ofVirtual().name("ai-similarity").start(call);
        try {
            double similarity = call.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (Double.isNaN(similarity)) {
                return 0;
            }
            return (int) Math.round(Math.max(0, Math.min(1, similarity)) * maxPoints);
        } catch (TimeoutException e) {
            call.cancel(true);
            log.warn("AI similarity did not answer in {} ms; the score goes without it", timeout.toMillis());
        } catch (InterruptedException e) {
            call.cancel(true);
            Thread.currentThread().interrupt();
        } catch (ExecutionException | RuntimeException e) {
            log.warn("AI similarity failed; the score goes without it: {}", e.toString());
        }
        return 0;
    }
}
