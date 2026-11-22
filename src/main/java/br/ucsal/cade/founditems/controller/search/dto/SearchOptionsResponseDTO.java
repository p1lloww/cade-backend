package br.ucsal.cade.founditems.controller.search.dto;

import br.ucsal.cade.founditems.persistence.status.FoundItemStatus;
import br.ucsal.cade.founditems.service.search.SearchOptions;
import br.ucsal.cade.shared.category.dto.CategoryResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Valores para os filtros da busca: só o que tem algum objeto.")
public record SearchOptionsResponseDTO(
        List<CategoryResponseDTO> categories,
        List<PlaceOption> places,
        @Schema(description = "Meses com objeto, do mais recente para o mais antigo.", example = "[\"2026-11\", \"2026-10\"]")
        List<String> months,
        List<StatusOption> statuses) {

    public static SearchOptionsResponseDTO from(SearchOptions options) {
        return new SearchOptionsResponseDTO(
                options.categories().stream().map(CategoryResponseDTO::from).toList(),
                options.places().stream().map(place -> new PlaceOption(place.block(), place.rooms())).toList(),
                options.months().stream().map(Object::toString).toList(),
                options.statuses().stream().map(StatusOption::from).toList());
    }

    @Schema(description = "Um bloco e as salas dele onde algum objeto foi achado.")
    public record PlaceOption(
            @Schema(example = "Bloco 400") String block,
            @Schema(example = "[\"Sala 421\", \"Sala 422\"]") List<String> rooms) {
    }

    @Schema(description = "Uma situação do objeto.")
    public record StatusOption(
            @Schema(example = "AVAILABLE") String code,
            @Schema(example = "Disponível") String label) {

        static StatusOption from(FoundItemStatus status) {
            return new StatusOption(status.code(), status.label());
        }
    }
}
