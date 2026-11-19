package br.ucsal.cade.lossreports.persistence.location;

import br.ucsal.cade.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void refusesALocationWithoutBlock(String block) {
        assertThatThrownBy(() -> LossLocation.of(block, "Sala 421"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(LossLocation.BLANK_BLOCK_MESSAGE);
    }
}
