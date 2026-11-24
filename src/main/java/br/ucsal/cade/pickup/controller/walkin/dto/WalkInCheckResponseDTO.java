package br.ucsal.cade.pickup.controller.walkin.dto;

import br.ucsal.cade.founditems.api.reservation.WalkInFoundItem;
import br.ucsal.cade.pickup.controller.counter.dto.VerificationOptionResponseDTO;
import br.ucsal.cade.pickup.service.verification.VerificationRule;
import br.ucsal.cade.pickup.service.walkin.WalkInAtCounter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** O objeto no atalho de balcão, com o que conferir (RF28). Só funcionários recebem esta resposta. */
@Schema(description = "O que conferir para entregar o objeto sem registro de perda, conforme o risco (RN09).")
public record WalkInCheckResponseDTO(
        UUID foundItemId,
        @Schema(example = "AVAILABLE") String status,
        @Schema(example = "Disponível") String statusLabel,
        @Schema(description = "Se o objeto pode sair pelo atalho: só Disponível ou Não reclamado. Reservado está "
                + "prometido a um convocado (RN18).")
        boolean canHandOver,
        @Schema(example = "HIGH") String risk,
        @Schema(example = "Alto") String riskLabel,
        @Schema(description = "O que conferir, na ordem: sempre o documento com foto e a matrícula primeiro (RN10).")
        List<String> checklist,
        @Schema(description = "As verificações que bastam para entregar; o funcionário registra uma delas.")
        List<VerificationOptionResponseDTO> acceptedVerifications) {

    public static WalkInCheckResponseDTO from(WalkInAtCounter atCounter) {
        WalkInFoundItem item = atCounter.item();
        VerificationRule rule = atCounter.rule();
        return new WalkInCheckResponseDTO(item.id(), item.status(), item.statusLabel(), item.handOverWithoutSummons(),
                item.risk().name(), item.risk().label(), rule.checklist(),
                rule.acceptedMethods().stream().sorted(Comparator.naturalOrder())
                        .map(VerificationOptionResponseDTO::from).toList());
    }
}
