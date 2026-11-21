package br.ucsal.cade.pickup.controller.counter.dto;

import br.ucsal.cade.pickup.persistence.summons.Summons;
import br.ucsal.cade.pickup.service.counter.SummonsAtCounter;
import br.ucsal.cade.pickup.service.verification.VerificationRule;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** A convocação como o funcionário a vê no balcão, com o que conferir (RF28). Só funcionários recebem esta resposta. */
@Schema(description = "Convocação com o que o funcionário confere no balcão, conforme o risco do objeto (RN09).")
public record CounterSummonsResponseDTO(
        UUID id,
        UUID suggestionId,
        UUID foundItemId,
        UUID lossReportId,
        @Schema(example = "ACTIVE") String status,
        @Schema(example = "Ativa") String statusLabel,
        @Schema(example = "HIGH") String risk,
        @Schema(example = "Alto") String riskLabel,
        Instant summonedAt,
        @Schema(description = "Até quando o objeto fica reservado (RN15).") Instant expiresAt,
        @Schema(description = "O que conferir, na ordem: sempre o documento com foto e a matrícula primeiro (RN10).")
        List<String> checklist,
        @Schema(description = "As verificações que bastam para entregar; o funcionário registra uma delas.")
        List<VerificationOptionResponseDTO> acceptedVerifications) {

    public static CounterSummonsResponseDTO from(SummonsAtCounter atCounter) {
        Summons summons = atCounter.summons();
        VerificationRule rule = atCounter.rule();
        return new CounterSummonsResponseDTO(summons.getId(), summons.getSuggestionId(), summons.getFoundItemId(),
                summons.getLossReportId(), summons.getStatus().code(), summons.getStatus().label(),
                summons.getRisk().name(), summons.getRisk().label(), summons.getSummonedAt(), summons.getExpiresAt(),
                rule.checklist(), rule.acceptedMethods().stream().sorted(Comparator.naturalOrder())
                .map(VerificationOptionResponseDTO::from).toList());
    }
}
