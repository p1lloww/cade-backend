package br.ucsal.cade.pickup.controller.counter.dto;

import br.ucsal.cade.pickup.persistence.pickup.VerificationMethod;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Uma verificação que basta para entregar o objeto.")
public record VerificationOptionResponseDTO(
        @Schema(example = "DEVICE_UNLOCKED") String code,
        @Schema(example = "Desbloqueio do aparelho na frente do funcionário") String label) {

    public static VerificationOptionResponseDTO from(VerificationMethod method) {
        return new VerificationOptionResponseDTO(method.name(), method.label());
    }
}
