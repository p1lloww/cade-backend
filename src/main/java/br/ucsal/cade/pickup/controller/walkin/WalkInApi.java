package br.ucsal.cade.pickup.controller.walkin;

import br.ucsal.cade.pickup.controller.walkin.dto.WalkInCheckResponseDTO;
import br.ucsal.cade.pickup.controller.walkin.dto.WalkInPickupRequestDTO;
import br.ucsal.cade.pickup.controller.walkin.dto.WalkInPickupResponseDTO;
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

import java.util.UUID;

/** Documentação OpenAPI do atalho de balcão (RF32); as rotas ficam no {@link WalkInController}. */
@Tag(name = "Balcão")
public interface WalkInApi {

    @Operation(
            summary = "O que conferir para entregar sem registro de perda",
            description = """
                    Para quem veio direto à sala, sem registro de perda (RF32). Mostra se o objeto pode sair pelo \
                    atalho (só Disponível ou Não reclamado) e o checklist com as verificações que bastam para o \
                    risco dele, as mesmas da retirada com convocação (RF28).""")
    @ApiResponse(responseCode = "200", description = "O que conferir.")
    @ApiResponse(responseCode = "404", description = "Objeto não encontrado.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    WalkInCheckResponseDTO check(@Parameter(description = "Id do objeto.") UUID id);

    @Operation(
            summary = "Registrar a retirada sem registro de perda (atalho de balcão)",
            description = """
                    Para um estudante ou uma pessoa sem conta, identificada por nome e documento (RF32, RN20). \
                    Estudante também informa a matrícula (RN10). A verificação precisa ser uma das aceitas para o \
                    risco do objeto. Na mesma operação, o objeto passa a Devolvido e as sugestões Novas ou Em espera \
                    dele a Encerrada (RF33), tudo no histórico (RF43). Objeto Reservado não sai pelo atalho: ele está \
                    prometido a um estudante convocado (RN18).""")
    @ApiResponse(responseCode = "201", description = "Retirada registrada.",
            content = @Content(schema = @Schema(implementation = WalkInPickupResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = """
            Campo ausente ou inválido, estudante sem matrícula, ou a verificação não basta para o risco do objeto.""",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Objeto não encontrado.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = """
            O objeto está Reservado para um convocado, já foi entregue ou destinado, ou foi convocado ao mesmo \
            tempo (tente de novo).""",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<WalkInPickupResponseDTO> register(WalkInPickupRequestDTO request,
                                                     @Parameter(hidden = true) Jwt token);
}
