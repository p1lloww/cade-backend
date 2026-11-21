package br.ucsal.cade.pickup.controller.counter.dto;

import br.ucsal.cade.pickup.persistence.pickup.CounterCheck;
import br.ucsal.cade.pickup.persistence.pickup.IdDocumentType;
import br.ucsal.cade.pickup.persistence.pickup.VerificationMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** O que o funcionário conferiu antes de entregar (RF29). */
@Schema(description = "Documento, matrícula e verificação conferidos no balcão.")
public record PickupRequestDTO(
        @NotNull(message = "Informe o tipo do documento com foto.")
        @Schema(example = "STUDENT_CARD") IdDocumentType documentType,

        @NotBlank(message = "Informe o número do documento.")
        @Size(max = 30, message = "O número do documento pode ter no máximo 30 caracteres.")
        String documentNumber,

        @NotBlank(message = "Informe a matrícula.")
        @Size(max = 20, message = "A matrícula pode ter no máximo 20 caracteres.")
        String enrollment,

        @NotNull(message = "Informe a verificação feita.")
        @Schema(example = "DETAIL_CONFIRMED", description = "Uma das verificações aceitas para o risco do objeto.")
        VerificationMethod verification,

        @Size(max = 500, message = "A observação pode ter no máximo 500 caracteres.")
        @Schema(description = "Opcional.")
        String notes) {

    public CounterCheck toCheck() {
        return new CounterCheck(documentType, documentNumber.strip(), enrollment.strip(), verification,
                notes == null || notes.isBlank() ? null : notes.strip());
    }
}
