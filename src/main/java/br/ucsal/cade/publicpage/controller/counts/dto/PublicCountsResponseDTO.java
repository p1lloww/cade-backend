package br.ucsal.cade.publicpage.controller.counts.dto;

import br.ucsal.cade.publicpage.service.counts.MonthCounts;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/** Só contagens: nenhuma foto, descrição ou local sai daqui (RF41, RN03). */
@Schema(description = "Quantos objetos disponíveis há na sala, por mês em que foram achados e por categoria.")
public record PublicCountsResponseDTO(
        @Schema(description = "Do mês mais recente para o mais antigo; só meses com objeto disponível.")
        List<MonthResponse> months) {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    public static PublicCountsResponseDTO from(List<MonthCounts> months) {
        return new PublicCountsResponseDTO(months.stream().map(MonthResponse::from).toList());
    }

    @Schema(description = "Os objetos disponíveis achados num mês.")
    public record MonthResponse(
            @Schema(example = "2026-11") String month,
            @Schema(example = "Novembro/2026") String label,
            @Schema(example = "17") long total,
            @Schema(description = "Da categoria com mais objetos para a com menos.")
            List<CategoryResponse> categories) {

        static MonthResponse from(MonthCounts counts) {
            return new MonthResponse(counts.month().toString(), label(counts.month()), counts.total(),
                    counts.categories().stream()
                            .map(category -> new CategoryResponse(category.category(), category.count()))
                            .toList());
        }

        private static String label(YearMonth month) {
            String name = month.getMonth().getDisplayName(TextStyle.FULL, PT_BR);
            return Character.toUpperCase(name.charAt(0)) + name.substring(1) + "/" + month.getYear();
        }
    }

    public record CategoryResponse(
            @Schema(example = "Garrafa") String category,
            @Schema(example = "12") long count) {
    }
}
