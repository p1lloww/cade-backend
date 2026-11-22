package br.ucsal.cade.lossreports.controller.reporter;

import br.ucsal.cade.lossreports.controller.reporter.dto.BlockedStudentResponseDTO;
import br.ucsal.cade.lossreports.controller.reporter.dto.StudentReleaseRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

/** Documentação OpenAPI da liberação dos bloqueados (RN12); as rotas ficam no {@link BlockedStudentsController}. */
@Tag(name = "Estudantes bloqueados", description = """
        Depois de 3 retiradas recusadas no balcão, o estudante não registra novas perdas até o administrador liberar \
        (RN12).""")
public interface BlockedStudentsApi {

    @Operation(
            summary = "Estudantes bloqueados",
            description = """
                    Só para o administrador. Os estudantes com 3 recusas ou mais, os com mais recusas primeiro. Traz o \
                    id da conta; o e-mail está na lista de usuários (`GET /users`).""")
    @ApiResponse(responseCode = "200", description = "Os bloqueados; lista vazia se não houver nenhum.")
    List<BlockedStudentResponseDTO> blocked();

    @Operation(
            summary = "Liberar um estudante bloqueado",
            description = """
                    Só para o administrador. As recusas voltam a zero, o estudante volta a registrar perdas e as \
                    próximas recusas contam do começo. A liberação vai para o histórico (RF43) com o administrador \
                    como autor e o motivo.""")
    @ApiResponse(responseCode = "200", description = "Estudante liberado.",
            content = @Content(schema = @Schema(implementation = BlockedStudentResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Motivo ausente ou longo demais.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Estudante que nunca registrou perda nem teve recusa.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = """
            O estudante não está bloqueado, ou foi recusado no balcão ao mesmo tempo (tente de novo).""",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    BlockedStudentResponseDTO release(@Parameter(description = "Id da conta do estudante.") UUID studentId,
                                      StudentReleaseRequestDTO request, @Parameter(hidden = true) Jwt token);
}
