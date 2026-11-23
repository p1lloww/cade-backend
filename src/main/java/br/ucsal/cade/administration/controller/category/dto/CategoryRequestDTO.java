package br.ucsal.cade.administration.controller.category.dto;

import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.RiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Os dados de uma categoria (RF44).")
public record CategoryRequestDTO(

        @Schema(description = "Nome da categoria; único, sem diferenciar maiúsculas.", example = "Instrumento musical",
                maxLength = Category.MAX_NAME_LENGTH)
        @NotBlank(message = "Informe o nome da categoria.")
        @Size(max = Category.MAX_NAME_LENGTH, message = "O nome pode ter no máximo 60 caracteres.")
        String name,

        @Schema(description = "Risco padrão: define o que é conferido no balcão (RN09).", example = "MEDIUM",
                allowableValues = {"LOW", "MEDIUM", "HIGH"})
        @NotNull(message = "Informe o risco padrão.")
        RiskLevel defaultRisk,

        @Schema(description = "Perguntas de conferência, na ordem em que o funcionário as faz; até 10, de até 200 "
                + "caracteres. Vazio apaga todas.", example = "[\"Qual a cor da capa?\"]")
        @Size(max = Category.MAX_QUESTIONS, message = "No máximo 10 perguntas de conferência.")
        List<@Size(max = Category.MAX_QUESTION_LENGTH, message = "Cada pergunta pode ter no máximo 200 caracteres.")
                String> verificationQuestions) {
}
