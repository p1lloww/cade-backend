package br.ucsal.cade.pickup.service.verification;

import br.ucsal.cade.pickup.persistence.pickup.VerificationMethod;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.exception.BadRequestException;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

public class VerificationNotAcceptedException extends BadRequestException {

    public VerificationNotAcceptedException(RiskLevel risk, Set<VerificationMethod> accepted) {
        super("Essa verificação não basta para um objeto de risco " + risk.label() + ". Vale: "
                + accepted.stream().sorted(Comparator.naturalOrder()).map(VerificationMethod::label)
                .collect(Collectors.joining(" ou ")) + ".");
    }
}
