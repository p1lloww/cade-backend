package br.ucsal.cade.lossreports.controller.report;

import br.ucsal.cade.lossreports.controller.report.dto.LossReportProofRequestDTO;
import br.ucsal.cade.lossreports.controller.report.dto.LossReportResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** Documentação OpenAPI da prova do registro de perda (RF15, PER-05). */
@Tag(name = "Registros de perda", description = """
        O estudante pode anexar uma prova de que o objeto é dele: foto antiga, nota fiscal ou número de série.""")
public interface LossReportProofApi {

    @Operation(
            summary = "Anexar ou trocar a prova do registro",
            description = """
                    Só para estudantes, só no próprio registro, e em qualquer situação que não seja final (RF15). \
                    Envie como `multipart/form-data` com a parte `proof` (JSON, com `Content-Type: \
                    application/json`, e o `serialNumber`) e/ou a parte `file` (foto antiga ou nota fiscal em JPEG, \
                    PNG ou WebP, até 5 MB; PDF não é aceito). Pelo menos um dos dois. O que não for enviado \
                    continua como estava. A foto trocada é apagada. A prova só é vista pelo dono e pelos \
                    funcionários, na conferência (RN03).""")
    @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            encoding = @Encoding(name = "proof", contentType = MediaType.APPLICATION_JSON_VALUE)))
    @ApiResponse(responseCode = "200", description = "Prova anexada; o registro volta com `proofPath` e/ou "
            + "`proofSerialNumber`.", content = @Content(schema = @Schema(implementation = LossReportResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = """
            Nem foto nem número de série, número de série longo demais, ou arquivo que não é JPEG, PNG ou WebP.""",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "O registro não existe ou é de outro estudante.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "O registro já está concluído, vencido, cancelado ou recusado.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "413", description = "Arquivo maior que o limite.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    LossReportResponseDTO attach(@Parameter(description = "Id do registro.") UUID id,
                                 LossReportProofRequestDTO proof,
                                 @Parameter(description = "Foto antiga ou nota fiscal (opcional).") MultipartFile file,
                                 @Parameter(hidden = true) Jwt token);

    @Operation(
            summary = "Ver a foto da prova",
            description = """
                    O estudante vê a prova do próprio registro; o funcionário vê a de qualquer registro, para \
                    conferir no balcão (RF23). A foto nunca tem URL pública: a API só a serve com o token, e a \
                    resposta não é guardada em cache (D21, RN03).""")
    @ApiResponse(responseCode = "200", description = "A foto da prova.",
            content = @Content(mediaType = "image/*"))
    @ApiResponse(responseCode = "404", description = "O registro não existe, é de outro estudante ou não tem foto "
            + "de prova.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<byte[]> proof(@Parameter(description = "Id do registro.") UUID id,
                                 @Parameter(hidden = true) Jwt token);
}
