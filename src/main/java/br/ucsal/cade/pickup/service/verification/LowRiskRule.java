package br.ucsal.cade.pickup.service.verification;

import br.ucsal.cade.pickup.persistence.pickup.VerificationMethod;
import br.ucsal.cade.shared.category.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/** Guarda-chuva, garrafa, caderno. Um detalhe que não aparece na foto é mais forte que a descrição, então também vale. */
@Component
class LowRiskRule implements VerificationRule {

    @Override
    public RiskLevel risk() {
        return RiskLevel.LOW;
    }

    @Override
    public List<String> specificChecks() {
        return List.of("A descrição que o estudante dá combina com o objeto.");
    }

    @Override
    public Set<VerificationMethod> acceptedMethods() {
        return Set.of(VerificationMethod.DESCRIPTION_MATCHED, VerificationMethod.DETAIL_CONFIRMED);
    }

    @Override
    public String extraToBring() {
        return "";
    }
}
