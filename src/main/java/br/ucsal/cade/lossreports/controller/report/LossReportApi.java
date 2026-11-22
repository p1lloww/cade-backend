package br.ucsal.cade.lossreports.controller.report;

import br.ucsal.cade.lossreports.controller.report.dto.LossReportRequestDTO;
import br.ucsal.cade.lossreports.controller.report.dto.LossReportResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

import java.util.List;
import java.util.UUID;

@Tag(name = "Registros de perda", description = """
        O estudante registra de casa o que perdeu. O detalhe secreto nunca aparece para outros estudantes (RF14).""")
public interface LossReportApi {

    @Operation(
            summary = "Registrar uma perda",
            description = """
                    Só para estudantes. Envie como `multipart/form-data` com a parte `report` (JSON, com \
                    `Content-Type: application/json`) e, se quiser, a parte `photo` (JPEG, PNG ou WebP, até 5 MB). \
                    O registro entra na situação Aberto e vence em 30 dias (RN14). Local e turno são opcionais: o \
                    que não for informado volta com o texto "não informado" nos campos `...Label`. Cada estudante \
                    pode ter no máximo 5 registros em andamento (RN05). O registro fica no histórico e dispara o \
                    cruzamento com os objetos disponíveis.""")
    @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            encoding = @Encoding(name = "report", contentType = MediaType.APPLICATION_JSON_VALUE)))
    @ApiResponse(responseCode = "201", description = "Perda registrada.",
            content = @Content(schema = @Schema(implementation = LossReportResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = """
            Campo obrigatório ausente, categoria inexistente, data no futuro, período invertido ou maior que 30 \
            dias, mais de 5 locais, ou foto que não é JPEG, PNG ou WebP.""",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = """
            O estudante já tem 5 registros em andamento, ou outro registro dele foi salvo ao mesmo tempo (tente \
            de novo).""",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "413", description = "Foto maior que o limite.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<LossReportResponseDTO> register(LossReportRequestDTO report,
                                                   @Parameter(description = "Foto do objeto (opcional).")
                                                   MultipartFile photo,
                                                   @Parameter(hidden = true) Jwt token);

    @Operation(
            summary = "Listar os meus registros",
            description = """
                    Só para estudantes. Devolve os registros de perda de quem está logado, em qualquer situação, do \
                    mais recente para o mais antigo (RF16). Cada um traz a situação explicada em linguagem simples \
                    (`statusDescription`). Não traz nada de objeto achado (RN03). No registro Convocado, onde e até \
                    quando retirar vêm de `GET /summons/mine`, pelo `lossReportId`.""")
    @ApiResponse(responseCode = "200", description = "Os registros do estudante; lista vazia se não houver nenhum.",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = LossReportResponseDTO.class))))
    List<LossReportResponseDTO> mine(@Parameter(hidden = true) Jwt token);

    @Operation(summary = "Baixar a foto de um registro", description = """
            Só o dono do registro. A foto não tem URL pública: o front pede com o token e mostra como blob (D09).""")
    @ApiResponse(responseCode = "200", description = "A foto (JPEG, PNG ou WebP).",
            content = @Content(mediaType = "image/*", schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "404", description = "Registro inexistente, de outro estudante, ou sem foto.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<byte[]> photo(UUID id, @Parameter(hidden = true) Jwt token);
}
