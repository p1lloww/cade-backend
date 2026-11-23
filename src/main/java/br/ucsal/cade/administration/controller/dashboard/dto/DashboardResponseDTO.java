package br.ucsal.cade.administration.controller.dashboard.dto;

import br.ucsal.cade.administration.service.dashboard.Dashboard;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "As contagens do painel do administrador (RF46). Só números.")
public record DashboardResponseDTO(

        @Schema(description = "Objetos por situação e categoria; só as combinações que têm objeto.")
        List<StatusCategoryResponseDTO> byStatusAndCategory,

        @Schema(description = "Objetos por situação, somando as categorias.")
        List<StatusTotalResponseDTO> byStatus,

        @Schema(description = "Quantos objetos estão não reclamados agora.", example = "4")
        long unclaimed,

        @Schema(description = "Devoluções feitas no mês corrente (fuso de Salvador).", example = "12")
        long returnsThisMonth,

        @Schema(description = "Tempo médio, em dias, da entrada na sala até a devolução; nulo se não houve devolução.",
                example = "6.5")
        Double averageDaysToReturn,

        Instant generatedAt) {

    public static DashboardResponseDTO from(Dashboard dashboard) {
        return new DashboardResponseDTO(
                dashboard.byStatusAndCategory().stream()
                        .map(row -> new StatusCategoryResponseDTO(row.statusCode(), row.statusLabel(), row.category(),
                                row.total()))
                        .toList(),
                dashboard.byStatus().stream()
                        .map(row -> new StatusTotalResponseDTO(row.statusCode(), row.statusLabel(), row.total()))
                        .toList(),
                dashboard.unclaimed(), dashboard.returnsThisMonth(), dashboard.averageDaysToReturn(),
                dashboard.generatedAt());
    }

    public record StatusCategoryResponseDTO(
            @Schema(example = "AVAILABLE") String status,
            @Schema(example = "Disponível") String statusLabel,
            @Schema(example = "Fone") String category,
            long total) {
    }

    public record StatusTotalResponseDTO(
            @Schema(example = "AVAILABLE") String status,
            @Schema(example = "Disponível") String statusLabel,
            long total) {
    }
}
