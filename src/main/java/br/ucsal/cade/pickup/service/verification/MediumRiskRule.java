package br.ucsal.cade.pickup.service.verification;

import br.ucsal.cade.pickup.persistence.pickup.VerificationMethod;
import br.ucsal.cade.shared.category.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/** Mochila, casaco, fone, carregador, óculos. */
@Component
class MediumRiskRule implements VerificationRule {

    @Override
    public RiskLevel risk() {
        return RiskLevel.MEDIUM;
    }

    @Override
    public List<String> specificChecks() {
        return List.of("O estudante descreve um detalhe que não aparece na foto, ou o detalhe reservado do objeto.");
    }

    @Override
    public Set<VerificationMethod> acceptedMethods() {
        return Set.of(VerificationMethod.DETAIL_CONFIRMED);
    }

    @Override
    public String extraToBring() {
        return "Esteja pronto para descrever um detalhe do objeto.";
    }
}
