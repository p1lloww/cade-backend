package br.ucsal.cade.pickup.controller.walkin.dto;

import br.ucsal.cade.pickup.persistence.pickup.CounterCheck;
import br.ucsal.cade.pickup.persistence.pickup.IdDocumentType;
import br.ucsal.cade.pickup.persistence.pickup.VerificationMethod;
import br.ucsal.cade.pickup.persistence.walkin.HolderType;
import br.ucsal.cade.pickup.persistence.walkin.WalkInHolder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Quem retira pelo atalho e o que o funcionário conferiu (RF32). */
@Schema(description = "Retirada sem registro de perda: objeto, quem retira, documento e verificação conferidos.")
public record WalkInPickupRequestDTO(
        @NotNull(message = "Informe o objeto.")
        UUID foundItemId,

        @NotNull(message = "Informe se quem retira é estudante ou pessoa sem conta.")
        @Schema(example = "STUDENT") HolderType holderType,

        @NotBlank(message = "Informe o nome de quem retira.")
        @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres.")
        @Schema(example = "Nome de quem retira") String holderName,

        @NotNull(message = "Informe o tipo do documento com foto.")
        @Schema(example = "RG") IdDocumentType documentType,

        @NotBlank(message = "Informe o número do documento.")
        @Size(max = 30, message = "O número do documento pode ter no máximo 30 caracteres.")
        String documentNumber,

        @Size(max = 20, message = "A matrícula pode ter no máximo 20 caracteres.")
        @Schema(description = "Obrigatória para estudante (RN10); ignorada para pessoa sem conta.")
        String enrollment,

        @NotNull(message = "Informe a verificação feita.")
        @Schema(example = "DESCRIPTION_MATCHED", description = "Uma das verificações aceitas para o risco do objeto.")
        VerificationMethod verification,

        @Size(max = 500, message = "A observação pode ter no máximo 500 caracteres.")
        @Schema(description = "Opcional.")
        String notes) {

    public WalkInHolder toHolder() {
        return new WalkInHolder(holderType, holderName.strip());
    }

    public CounterCheck toCheck() {
        return new CounterCheck(documentType, documentNumber.strip(),
                enrollment == null || enrollment.isBlank() ? null : enrollment.strip(), verification,
                notes == null || notes.isBlank() ? null : notes.strip());
    }
}
