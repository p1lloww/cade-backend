package br.ucsal.cade.pickup.service.summons;

import br.ucsal.cade.shared.category.RiskLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class WhatToBringTest {

    @ParameterizedTest
    @EnumSource(RiskLevel.class)
    void everyRiskAsksForAPhotoId(RiskLevel risk) {
        assertThat(WhatToBring.forRisk(risk)).startsWith(WhatToBring.PHOTO_ID);
    }

    @Test
    void aLowRiskItemNeedsOnlyThePhotoId() {
        assertThat(WhatToBring.forRisk(RiskLevel.LOW)).isEqualTo(WhatToBring.PHOTO_ID);
    }

    @Test
    void aHighRiskItemAlsoNeedsTheUnlockOrAProof() {
        assertThat(WhatToBring.forRisk(RiskLevel.HIGH)).contains("desbloqueá-lo", "nota fiscal");
    }
}
