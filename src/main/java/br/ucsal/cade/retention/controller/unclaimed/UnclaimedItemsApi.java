package br.ucsal.cade.retention.controller.unclaimed;

import br.ucsal.cade.retention.controller.unclaimed.dto.DisposalRequestDTO;
import br.ucsal.cade.retention.controller.unclaimed.dto.DisposalResponseDTO;
import br.ucsal.cade.retention.controller.unclaimed.dto.UnclaimedBoxResponseDTO;
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

/** Documentação OpenAPI dos objetos não reclamados e do destino; as rotas ficam no {@link UnclaimedItemsController}. */
@Tag(name = "Não reclamados", description = """
        Objetos que passaram do prazo de guarda (90 dias desde a entrada na sala, RN16) sem dono, e o destino que o \
        administrador dá a eles.""")
public interface UnclaimedItemsApi {

    @Operation(
            summary = "Não reclamados por caixa",
            description = """
                    Só para funcionários (RF39). Os objetos não reclamados agrupados pela caixa em que estão \
                    guardados (a do mês), para retirar da sala de uma vez. Os mais antigos primeiro dentro de cada \
                    caixa.""")
    @ApiResponse(responseCode = "200", description = "As caixas com não reclamados; lista vazia se não houver nenhum.")
    List<UnclaimedBoxResponseDTO> byBox();

    @Operation(
            summary = "Dar destino a um não reclamado",
            description = """
                    Só para o administrador (RF38). O objeto passa a Destinado e o destino fica gravado com a data e \
                    o responsável. Sem destino no corpo, vale o bazar; \"outro\" exige dizer qual em `note`. Vai \
                    para o histórico (RF43).""")
    @ApiResponse(responseCode = "201", description = "Destino registrado.",
            content = @Content(schema = @Schema(implementation = DisposalResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Destino \"outro\" sem dizer qual, ou texto longo demais.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Objeto não encontrado.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "O objeto não está Não reclamado.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    DisposalResponseDTO dispose(@Parameter(description = "Id do objeto.") UUID id, DisposalRequestDTO request,
                                @Parameter(hidden = true) Jwt token);
}
