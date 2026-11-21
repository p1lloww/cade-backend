package br.ucsal.cade.pickup.service.verification;

import br.ucsal.cade.pickup.persistence.pickup.VerificationMethod;
import br.ucsal.cade.shared.category.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/** RG, carteirinha, cartão: só o titular retira (RN11). */
@Component
class DocumentRule implements VerificationRule {

    @Override
    public RiskLevel risk() {
        return RiskLevel.DOCUMENT;
    }

    @Override
    public List<String> specificChecks() {
        return List.of("O documento achado é de quem está retirando: nome e foto conferem.");
    }

    @Override
    public Set<VerificationMethod> acceptedMethods() {
        return Set.of(VerificationMethod.HOLDER_PHOTO_MATCHED);
    }

    @Override
    public String extraToBring() {
        return "O documento achado é entregue só ao titular.";
    }
}
