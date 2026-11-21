package br.ucsal.cade.matching.controller.suggestion.dto;

import br.ucsal.cade.founditems.api.ComparableFoundItem;
import br.ucsal.cade.lossreports.api.ComparableLossReport;
import br.ucsal.cade.lossreports.api.MatchableLossReport;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestion;
import br.ucsal.cade.matching.service.score.result.CompatibilityBand;
import br.ucsal.cade.matching.service.suggestion.ComparedSuggestion;
import br.ucsal.cade.shared.category.RiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** A sugestão com o objeto e a perda lado a lado (RF22, RF23). Só funcionários recebem esta resposta (RN03). */
@Schema(description = "Sugestão do cruzamento com o objeto e a perda lado a lado. Só funcionários recebem.")
public record SuggestionResponseDTO(
        UUID id,
        @Schema(description = "Compatibilidade de 0 a 100 (RN07).", example = "80") int score,
        @Schema(example = "HIGH") CompatibilityBand band,
        @Schema(example = "Alta") String bandLabel,
        @Schema(example = "NEW") String status,
        @Schema(example = "Nova") String statusLabel,
        Instant createdAt,
        FoundItemSide foundItem,
        LossReportSide lossReport) {

    public static SuggestionResponseDTO from(ComparedSuggestion compared) {
        MatchSuggestion suggestion = compared.suggestion();
        return new SuggestionResponseDTO(suggestion.getId(), suggestion.getScore(), compared.band(),
                compared.band().label(), suggestion.getStatus().code(), suggestion.getStatus().label(),
                suggestion.getCreatedAt(), FoundItemSide.from(compared.foundItem()),
                LossReportSide.from(compared.lossReport()));
    }

    @Schema(description = "O objeto achado, com os dados reservados.")
    public record FoundItemSide(
            UUID id,
            @Schema(example = "Fone") String categoryName,
            @Schema(example = "2026-11-04") LocalDate foundOn,
            @Schema(example = "Bloco 400") String block,
            @Schema(example = "Sala 421") String room,
            @Schema(example = "Caixa Novembro/2026") String storageBox,
            @Schema(example = "MEDIUM") RiskLevel risk,
            @Schema(example = "Médio") String riskLabel,
            @Schema(example = "Disponível") String statusLabel,
            String internalDescription,
            String reservedDetails,
            @Schema(description = "Caminho da foto na API; pedir com o token.", example = "/found-items/{id}/photo")
            String photoPath) {

        static FoundItemSide from(ComparableFoundItem item) {
            return new FoundItemSide(item.id(), item.categoryName(), item.foundOn(), item.block(), item.room(),
                    item.storageBox(), item.risk(), item.risk().label(), item.status(), item.internalDescription(),
                    item.reservedDetails(), "/found-items/" + item.id() + "/photo");
        }
    }

    @Schema(description = "O registro de perda, com o detalhe secreto e sem o estudante.")
    public record LossReportSide(
            UUID id,
            @Schema(example = "Fone") String categoryName,
            @Schema(example = "2026-11-04") LocalDate lostFrom,
            @Schema(example = "2026-11-04") LocalDate lostUntil,
            @Schema(example = "Noite") String shift,
            List<Location> locations,
            @Schema(example = "Fone JBL preto") String description,
            @Schema(example = "Adesivo de estrela no estojo") String secretDetail,
            @Schema(example = "Aberto") String statusLabel,
            Instant registeredAt) {

        static LossReportSide from(ComparableLossReport report) {
            List<Location> locations = report.locations().stream().map(Location::from).toList();
            return new LossReportSide(report.id(), report.categoryName(), report.lostFrom(), report.lostUntil(),
                    report.shift(), locations, report.description(), report.secretDetail(), report.status(),
                    report.registeredAt());
        }
    }

    public record Location(
            @Schema(example = "Bloco 400") String block,
            @Schema(description = "Nula quando o estudante não sabe a sala.", example = "Sala 421") String room) {

        static Location from(MatchableLossReport.Location location) {
            return new Location(location.block(), location.room());
        }
    }
}
