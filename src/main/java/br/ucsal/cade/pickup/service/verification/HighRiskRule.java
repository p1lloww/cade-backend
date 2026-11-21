package br.ucsal.cade.pickup.service.verification;

import br.ucsal.cade.pickup.persistence.pickup.VerificationMethod;
import br.ucsal.cade.shared.category.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/** Celular, notebook, carteira, chaves. Uma descrição, por mais certa, não basta: pode ter sido observada (E4). */
@Component
class HighRiskRule implements VerificationRule {

    @Override
    public RiskLevel risk() {
        return RiskLevel.HIGH;
    }

    @Override
    public List<String> specificChecks() {
        return List.of("Aparelho: o estudante desbloqueia na sua frente.",
                "Sem desbloqueio (ou se não for aparelho): nota fiscal ou número de série que confira com o objeto.");
    }

    @Override
    public Set<VerificationMethod> acceptedMethods() {
        return Set.of(VerificationMethod.DEVICE_UNLOCKED, VerificationMethod.PROOF_OF_OWNERSHIP);
    }

    @Override
    public String extraToBring() {
        return "Se for um aparelho, você vai desbloqueá-lo na frente do funcionário; se não puder, leve uma prova de"
                + " que é seu, como a nota fiscal ou o número de série.";
    }
}
