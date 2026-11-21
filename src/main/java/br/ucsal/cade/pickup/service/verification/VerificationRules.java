package br.ucsal.cade.pickup.service.verification;

import br.ucsal.cade.shared.category.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Escolhe a regra do balcão pelo risco (RN09). Recebe todas as {@link VerificationRule} do Spring e recusa subir se um
 * risco ficar sem regra ou com duas, para que um risco novo no {@link RiskLevel} não passe despercebido.
 */
@Component
public class VerificationRules {

    private final Map<RiskLevel, VerificationRule> byRisk = new EnumMap<>(RiskLevel.class);

    VerificationRules(List<VerificationRule> rules) {
        for (VerificationRule rule : rules) {
            if (byRisk.put(rule.risk(), rule) != null) {
                throw new IllegalStateException("Mais de uma regra de conferência para o risco " + rule.risk());
            }
        }
        List<RiskLevel> missing = Arrays.stream(RiskLevel.values()).filter(risk -> !byRisk.containsKey(risk)).toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Riscos sem regra de conferência: " + missing);
        }
    }

    public VerificationRule forRisk(RiskLevel risk) {
        return byRisk.get(risk);
    }
}
