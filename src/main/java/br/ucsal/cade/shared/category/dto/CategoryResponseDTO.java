package br.ucsal.cade.shared.category.dto;

import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.RiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
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
        String defaultRiskLabel,

        @Schema(description = "Perguntas de conferência da categoria, na ordem em que o funcionário as faz (RF44).")
        List<String> verificationQuestions) {

    public static CategoryResponseDTO from(Category category) {
        return new CategoryResponseDTO(category.getId(), category.getName(), category.getDefaultRisk(),
                category.getDefaultRisk().label(), category.getVerificationQuestions());
    }
}
