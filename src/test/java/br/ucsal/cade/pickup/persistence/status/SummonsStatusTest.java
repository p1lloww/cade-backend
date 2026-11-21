package br.ucsal.cade.pickup.persistence.status;

import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SummonsStatusTest {

    static final List<SummonsStatus> FINAL = List.of(SummonsStatus.COMPLETED, SummonsStatus.REFUSED,
            SummonsStatus.EXPIRED);

    @Test
    void aSummonsStartsActive() {
        assertThat(SummonsStatus.initial()).isEqualTo(SummonsStatus.ACTIVE);
        assertThat(SummonsStatus.ACTIVE.isFinal()).isFalse();
    }

    @Test
    void anActiveSummonsEndsByPickupRefusalOrDeadline() {
        assertThat(SummonsStatus.ACTIVE.complete()).isEqualTo(SummonsStatus.COMPLETED);
        assertThat(SummonsStatus.ACTIVE.refuse()).isEqualTo(SummonsStatus.REFUSED);
        assertThat(SummonsStatus.ACTIVE.expire()).isEqualTo(SummonsStatus.EXPIRED);
    }

    @ParameterizedTest
    @FieldSource("FINAL")
    void aFinalSummonsAllowsNothing(SummonsStatus status) {
        assertThat(status.isFinal()).isTrue();
        assertThatThrownBy(status::complete).isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(status::refuse).isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(status::expire).isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("Não é possível vencer uma convocação na situação " + status.label() + ".");
    }

    @Test
    void everyStatusComesBackFromItsCode() {
        SummonsStatus.ALL.forEach(status -> assertThat(SummonsStatus.fromCode(status.code())).isEqualTo(status));
        assertThatThrownBy(() -> SummonsStatus.fromCode("UNKNOWN")).isInstanceOf(IllegalArgumentException.class);
    }
}
