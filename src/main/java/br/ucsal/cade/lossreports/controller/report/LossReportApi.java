package br.ucsal.cade.lossreports.controller.report;

import br.ucsal.cade.lossreports.dto.request.LossReportRequestDTO;
import br.ucsal.cade.lossreports.dto.response.LossReportResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

/** Documentação OpenAPI do registro de perdas (RF13, RF14); as rotas ficam no {@link LossReportController}. */
@Tag(name = "Registros de perda", description = """
        O estudante registra de casa o que perdeu. O detalhe secreto nunca aparece para outros estudantes (RF14).""")
public interface LossReportApi {

    @Operation(
            summary = "Registrar uma perda",
            description = """
                    Só para estudantes. O registro entra na situação Aberto e vence em 30 dias (RN14). Local e \
                    turno são opcionais: o que não for informado volta com o texto "não informado" nos campos \
                    `...Label`. Cada estudante pode ter no máximo 5 registros em andamento (RN05). O registro fica \
                    no histórico e dispara o cruzamento com os objetos disponíveis.""")
    @ApiResponse(responseCode = "201", description = "Perda registrada.",
            content = @Content(schema = @Schema(implementation = LossReportResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = """
            Campo obrigatório ausente, categoria inexistente, data no futuro, período invertido ou maior que 30 \
            dias, ou mais de 5 locais.""",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = """
            O estudante já tem 5 registros em andamento, ou outro registro dele foi salvo ao mesmo tempo (tente \
            de novo).""",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<LossReportResponseDTO> register(LossReportRequestDTO request, @Parameter(hidden = true) Jwt token);
}
