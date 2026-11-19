package br.ucsal.cade.shared.category.dto;

import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.RiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Categoria de objeto, com o risco padrão que define o que é conferido no balcão.")
public record CategoryResponseDTO(

        @Schema(description = "Id da categoria, usado nos cadastros.")
        UUID id,

        @Schema(description = "Nome da categoria.", example = "Fone")
        String name,

        @Schema(description = "Risco padrão (RN09).", example = "MEDIUM")
        RiskLevel defaultRisk,

        @Schema(description = "Nome do risco para mostrar ao usuário.", example = "Médio")
        String defaultRiskLabel) {

    public static CategoryResponseDTO from(Category category) {
        return new CategoryResponseDTO(category.getId(), category.getName(), category.getDefaultRisk(),
                category.getDefaultRisk().label());
    }
}
