package br.ucsal.cade.lossreports.controller.report;

import br.ucsal.cade.lossreports.controller.report.dto.LossReportAnswerRequestDTO;
import br.ucsal.cade.lossreports.controller.report.dto.LossReportResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/** Documentação OpenAPI do cancelamento, da resposta ao pedido de informações e da renovação (PER-06). */
@Tag(name = "Registros de perda", description = """
        O estudante cancela, responde a um pedido de mais informações e renova o registro antes de vencer.""")
public interface LossReportLifecycleApi {

    @Operation(
            summary = "Cancelar um registro (\"já achei\")",
            description = """
                    Só para estudantes, só no próprio registro, e só enquanto ele está **Aberto** ou **Aguardando \
                    informações** (RF17). As sugestões abertas do registro são encerradas e o cancelamento vai para \
                    o histórico.""")
    @ApiResponse(responseCode = "200", description = "Registro cancelado.",
            content = @Content(schema = @Schema(implementation = LossReportResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "O registro não existe ou é de outro estudante.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "O registro não está Aberto nem Aguardando informações.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    LossReportResponseDTO cancel(@Parameter(description = "Id do registro.") UUID id,
                                 @Parameter(hidden = true) Jwt token);

    @Operation(
            summary = "Responder a um pedido de mais informações",
            description = """
                    Só para estudantes, só no próprio registro, e só enquanto ele está **Aguardando informações** \
                    (RF18). O registro volta a Aberto. O estudante tem 5 dias para responder (RN13); sem resposta, o \
                    registro volta a Aberto sozinho e a sugestão que originou o pedido é descartada. Quem tinha o \
                    pedido recebe a resposta junto do registro.""")
    @ApiResponse(responseCode = "200", description = "Resposta registrada; o registro voltou a Aberto.",
            content = @Content(schema = @Schema(implementation = LossReportResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Resposta em branco ou maior que 1000 caracteres.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "O registro não existe ou é de outro estudante.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "O registro não está Aguardando informações.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    LossReportResponseDTO respond(@Parameter(description = "Id do registro.") UUID id,
                                  LossReportAnswerRequestDTO answer, @Parameter(hidden = true) Jwt token);

    @Operation(
            summary = "Renovar um registro",
            description = """
                    Só para estudantes, só no próprio registro, e só enquanto ele está **Aberto** ou **Aguardando \
                    informações** (RN14). A validade passa a ser 30 dias a partir de agora. O estudante é avisado por \
                    e-mail 3 dias antes de o registro vencer (RF35).""")
    @ApiResponse(responseCode = "200", description = "Registro renovado, com o novo `expiresAt`.",
            content = @Content(schema = @Schema(implementation = LossReportResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "O registro não existe ou é de outro estudante.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "O registro não está Aberto nem Aguardando informações.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    LossReportResponseDTO renew(@Parameter(description = "Id do registro.") UUID id,
                                @Parameter(hidden = true) Jwt token);
}
