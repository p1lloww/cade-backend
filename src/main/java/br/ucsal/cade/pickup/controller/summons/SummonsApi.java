package br.ucsal.cade.pickup.controller.summons;

import br.ucsal.cade.pickup.controller.summons.dto.SummonsResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/** Documentação OpenAPI da convocação (RF26); a rota fica no {@link SummonsController}. */
@Tag(name = "Convocação", description = """
        O funcionário convoca o estudante a partir de uma sugestão. O objeto fica reservado até a retirada, a recusa \
        no balcão ou o fim do prazo (RN15), e um objeto tem no máximo uma convocação ativa (RN18).""")
public interface SummonsApi {

    @Operation(
            summary = "Convocar o estudante a partir de uma sugestão",
            description = """
                    Na mesma operação, o objeto passa a Reservado, o registro de perda a Convocado (a descrição e o \
                    detalhe secreto ficam travados, RN06) e a sugestão a Convocada. As outras sugestões Novas do \
                    mesmo objeto ficam Em espera (RN18). Tudo vai para o histórico (RF43), e o estudante recebe \
                    local, horário, prazo e o que levar (RF26). Só uma sugestão Nova pode ser convocada.""")
    @ApiResponse(responseCode = "201", description = "Estudante convocado.",
            content = @Content(schema = @Schema(implementation = SummonsResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "Sugestão não encontrada.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = """
            A sugestão não está Nova, o objeto já está reservado ou entregue, o registro de perda não está Aberto, \
            ou outra convocação do mesmo objeto foi gravada ao mesmo tempo.""",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Objeto já reservado", value = """
                            {
                              "type": "about:blank",
                              "title": "Conflict",
                              "status": 409,
                              "detail": "Não é possível convocar um objeto na situação Reservado.",
                              "instance": "/suggestions/6f1c2a1e-0000-4000-8000-000000000003/summon"
                            }""")))
    ResponseEntity<SummonsResponseDTO> summon(@Parameter(description = "Id da sugestão.") UUID id,
                                              @Parameter(hidden = true) Jwt token);
}
