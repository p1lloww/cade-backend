package br.ucsal.cade.pickup.service.verification;

import br.ucsal.cade.pickup.exception.VerificationNotAcceptedException;
import br.ucsal.cade.pickup.persistence.pickup.VerificationMethod;
import br.ucsal.cade.shared.category.RiskLevel;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * O que o balcão exige de um risco (RN09), como estratégia: o checklist do funcionário, as verificações que valem
 * para entregar e o que o estudante leva. As três coisas mudam juntas com o risco, então ficam na mesma classe, e um
 * risco novo é uma regra nova, sem mexer nas outras.
 *
 * <p>Todo risco começa pelo documento com foto e pela matrícula (RN10).
 */
public interface VerificationRule {

    String PHOTO_ID_CHECK = "Documento com foto: a foto confere com quem está retirando, e a matrícula foi anotada.";

    String PHOTO_ID_TO_BRING = "Um documento com foto (RG, CNH ou carteirinha da UCSal).";

    RiskLevel risk();

    /** O que o funcionário confere além do documento. */
    List<String> specificChecks();

    /** As verificações que bastam para entregar; basta uma delas. */
    Set<VerificationMethod> acceptedMethods();

    /** O que o estudante leva além do documento. Não conta o que o funcionário vai conferir (RN03). */
    String extraToBring();

    default List<String> checklist() {
        return Stream.concat(Stream.of(PHOTO_ID_CHECK), specificChecks().stream()).toList();
    }

    default String whatToBring() {
        String extra = extraToBring();
        return extra.isEmpty() ? PHOTO_ID_TO_BRING : PHOTO_ID_TO_BRING + " " + extra;
    }

    /** @throws VerificationNotAcceptedException se a verificação não serve para este risco */
    default void requireAccepted(VerificationMethod method) {
        if (!acceptedMethods().contains(method)) {
            throw new VerificationNotAcceptedException(risk(), acceptedMethods());
        }
    }
}
