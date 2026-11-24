package br.ucsal.cade.pickup.service.verification;

import br.ucsal.cade.pickup.exception.VerificationNotAcceptedException;
import br.ucsal.cade.pickup.persistence.pickup.VerificationMethod;
import br.ucsal.cade.shared.category.RiskLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerificationRulesTest {

    private final VerificationRules rules = new VerificationRules(List.of(new LowRiskRule(), new MediumRiskRule(),
            new HighRiskRule(), new DocumentRule()));

    @ParameterizedTest
    @EnumSource(RiskLevel.class)
    void everyRiskHasItsOwnRule(RiskLevel risk) {
        assertThat(rules.forRisk(risk).risk()).isEqualTo(risk);
    }

    @ParameterizedTest
    @EnumSource(RiskLevel.class)
    void everyRiskStartsWithThePhotoIdAndTheEnrollment(RiskLevel risk) {
        VerificationRule rule = rules.forRisk(risk);

        assertThat(rule.checklist()).first().isEqualTo(VerificationRule.PHOTO_ID_CHECK);
        assertThat(rule.checklist()).hasSizeGreaterThan(1);
        assertThat(rule.whatToBring()).startsWith(VerificationRule.PHOTO_ID_TO_BRING);
    }

    @Test
    void aLowRiskItemAcceptsTheDescriptionOrADetail() {
        VerificationRule rule = rules.forRisk(RiskLevel.LOW);

        assertThat(rule.acceptedMethods()).containsExactlyInAnyOrder(VerificationMethod.DESCRIPTION_MATCHED,
                VerificationMethod.DETAIL_CONFIRMED);
        assertThat(rule.whatToBring()).isEqualTo(VerificationRule.PHOTO_ID_TO_BRING);
    }

    @Test
    void aMediumRiskItemNeedsADetailThatIsNotInThePhoto() {
        VerificationRule rule = rules.forRisk(RiskLevel.MEDIUM);

        assertThat(rule.acceptedMethods()).containsExactly(VerificationMethod.DETAIL_CONFIRMED);
        assertThatThrownBy(() -> rule.requireAccepted(VerificationMethod.DESCRIPTION_MATCHED))
                .isInstanceOf(VerificationNotAcceptedException.class)
                .hasMessageContaining("risco Médio");
    }

    @Test
    void aHighRiskItemNeedsTheUnlockOrAProof() {
        VerificationRule rule = rules.forRisk(RiskLevel.HIGH);

        assertThatCode(() -> rule.requireAccepted(VerificationMethod.DEVICE_UNLOCKED)).doesNotThrowAnyException();
        assertThatCode(() -> rule.requireAccepted(VerificationMethod.PROOF_OF_OWNERSHIP)).doesNotThrowAnyException();
        assertThatThrownBy(() -> rule.requireAccepted(VerificationMethod.DETAIL_CONFIRMED))
                .isInstanceOf(VerificationNotAcceptedException.class)
                .hasMessageContaining("Desbloqueio do aparelho").hasMessageContaining("nota fiscal");
        assertThat(rule.whatToBring()).contains("desbloqueá-lo", "nota fiscal");
    }

    @Test
    void aDocumentGoesOnlyToItsHolder() {
        VerificationRule rule = rules.forRisk(RiskLevel.DOCUMENT);

        assertThat(rule.acceptedMethods()).containsExactly(VerificationMethod.HOLDER_PHOTO_MATCHED);
        assertThatThrownBy(() -> rule.requireAccepted(VerificationMethod.DEVICE_UNLOCKED))
                .isInstanceOf(VerificationNotAcceptedException.class);
    }

    @Test
    void refusesToStartWithARiskWithoutARule() {
        assertThatThrownBy(() -> new VerificationRules(List.of(new LowRiskRule(), new MediumRiskRule())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HIGH").hasMessageContaining("DOCUMENT");
    }

    @Test
    void refusesToStartWithTwoRulesForTheSameRisk() {
        assertThatThrownBy(() -> new VerificationRules(List.of(new LowRiskRule(), new LowRiskRule(),
                new MediumRiskRule(), new HighRiskRule(), new DocumentRule())))
                .isInstanceOf(IllegalStateException.class);
    }
}
