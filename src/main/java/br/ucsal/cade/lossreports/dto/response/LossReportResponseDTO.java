package br.ucsal.cade.lossreports.dto.response;

import br.ucsal.cade.lossreports.persistence.period.LostPeriod;
import br.ucsal.cade.lossreports.persistence.period.Shift;
import br.ucsal.cade.lossreports.persistence.report.LossReport;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** Inclui o detalhe secreto: só o dono recebe esta resposta (RF14). */
@Schema(description = "Registro de perda com todos os dados, inclusive o detalhe secreto. Só o dono recebe.")
public record LossReportResponseDTO(
        UUID id,
        UUID categoryId,
        @Schema(example = "Fone") String categoryName,
        @Schema(example = "Fone JBL preto") String description,
        String secretDetail,
        List<LossLocationResponseDTO> locations,
        @Schema(example = "Bloco 400, Sala 421; Bloco B, sala não informada") String locationsLabel,
        @Schema(example = "2026-11-04") LocalDate lostFrom,
        @Schema(example = "2026-11-04") LocalDate lostUntil,
        Shift shift,
        @Schema(example = "Noite") String shiftLabel,
        @Schema(example = "OPEN") String status,
        @Schema(example = "Aberto") String statusLabel,
        @Schema(description = "Caminho da foto na API, pedir com o token; nulo quando não há foto.",
                example = "/loss-reports/{id}/photo")
        String photoPath,
        Instant registeredAt,
        Instant expiresAt) {

    static final String UNKNOWN_LOCATION = "Local não informado";
    static final String UNKNOWN_SHIFT = "Turno não informado";

    public static LossReportResponseDTO from(LossReport report) {
        List<LossLocationResponseDTO> locations = report.getLocations().stream()
                .map(LossLocationResponseDTO::from)
                .toList();
        LostPeriod period = report.getPeriod();
        return new LossReportResponseDTO(report.getId(), report.getCategory().getId(),
                report.getCategory().getName(), report.getDescription(), report.getSecretDetail(), locations,
                locationsLabel(locations), period.lostFrom(), period.lostUntil(), period.shift(),
                period.shift() == null ? UNKNOWN_SHIFT : period.shift().label(), report.getStatus().code(),
                report.getStatus().label(), report.hasPhoto() ? "/loss-reports/" + report.getId() + "/photo" : null,
                report.getRegisteredAt(), report.getExpiresAt());
    }

    private static String locationsLabel(List<LossLocationResponseDTO> locations) {
        return locations.isEmpty()
                ? UNKNOWN_LOCATION
                : locations.stream().map(LossLocationResponseDTO::label).collect(Collectors.joining("; "));
    }
}
