package br.ucsal.cade.administration.controller.settings;

import br.ucsal.cade.administration.controller.settings.dto.DeadlineSettingsRequestDTO;
import br.ucsal.cade.administration.controller.settings.dto.DeadlineSettingsResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.oauth2.jwt.Jwt;

/** Documentação OpenAPI dos prazos configuráveis (RF45); as rotas ficam no {@link DeadlineSettingsController}. */
@Tag(name = "Prazos (administração)", description = """
        Os prazos de guarda, de retirada depois da convocação e de validade do registro de perda. Os valores iniciais \
        são 90, 7 e 30 dias. Uma mudança vale para o que vem depois: convocações e registros já criados guardam o \
        prazo com que nasceram.""")
public interface DeadlineSettingsApi {

    @Operation(
            summary = "Ver os prazos em vigor",
            description = "Só para o administrador. Os valores configurados ou, se nunca mudaram, os iniciais.")
    @ApiResponse(responseCode = "200", description = "Os prazos em vigor, em dias.",
            content = @Content(schema = @Schema(implementation = DeadlineSettingsResponseDTO.class)))
    DeadlineSettingsResponseDTO current();

    @Operation(
            summary = "Ajustar os prazos",
            description = """
                    Só para o administrador. Informe os três prazos, em dias: guarda de 30 a 365, retirada depois da \
                    convocação de 1 a 30 e validade do registro de 7 a 90. Vale logo, sem reiniciar. Vai para o \
                    histórico (RF43) com o que mudou.""")
    @ApiResponse(responseCode = "200", description = "Os prazos em vigor depois do ajuste.",
            content = @Content(schema = @Schema(implementation = DeadlineSettingsResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Prazo ausente ou fora dos limites.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    DeadlineSettingsResponseDTO update(DeadlineSettingsRequestDTO request, @Parameter(hidden = true) Jwt token);
}
