package br.ucsal.cade.lossreports.persistence.location;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class LossLocationTest {

    @Test
    void stripsTheBlockAndTheRoom() {
        assertThat(LossLocation.of("  Bloco 400 ", " Sala 421  ")).isEqualTo(new LossLocation("Bloco 400", "Sala 421"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void anUnknownRoomIsStoredAsNullNotAsText(String room) {
        assertThat(LossLocation.of("Bloco 400", room).room()).isNull();
    }
}
