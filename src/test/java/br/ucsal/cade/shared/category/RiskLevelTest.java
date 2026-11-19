package br.ucsal.cade.shared.category;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class RiskLevelTest {

    @ParameterizedTest(name = "{0} → {1}: {2}")
    @CsvSource({
            "LOW,      LOW,      true",
            "LOW,      MEDIUM,   true",
            "LOW,      HIGH,     true",
            "MEDIUM,   LOW,      false",
            "MEDIUM,   HIGH,     true",
            "HIGH,     MEDIUM,   false",
            "HIGH,     LOW,      false",
            "HIGH,     HIGH,     true",
            "LOW,      DOCUMENT, false",
            "HIGH,     DOCUMENT, false",
            "DOCUMENT, HIGH,     false",
            "DOCUMENT, LOW,      false",
            "DOCUMENT, DOCUMENT, true"})
    void onlyRaisesTheRiskAndNeverTurnsAnItemIntoADocument(RiskLevel defaultRisk, RiskLevel requested,
                                                           boolean allowed) {
        assertThat(defaultRisk.canBeRaisedTo(requested)).isEqualTo(allowed);
    }
}
