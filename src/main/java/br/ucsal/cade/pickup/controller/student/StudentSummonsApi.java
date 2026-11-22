package br.ucsal.cade.pickup.controller.student;

import br.ucsal.cade.pickup.controller.student.dto.StudentSummonsResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/** Documentação OpenAPI das convocações vistas pelo estudante; a rota fica no {@link StudentSummonsController}. */
@Tag(name = "Convocação")
public interface StudentSummonsApi {

    @Operation(
            summary = "Listar as minhas convocações ativas",
            description = """
                    Só para estudantes. Devolve as convocações Ativas de quem está logado, as que vencem primeiro no \
                    topo, com onde fica a sala, o horário, até quando o objeto fica reservado e o que levar ao \
                    balcão conforme o risco do objeto (RF26). Cada uma traz o `lossReportId`, para o front mostrar \
                    junto do registro Convocado de `GET /loss-reports/mine` (RF16). Não traz nada do objeto achado \
                    (RN03). Depois da retirada, da recusa ou do fim do prazo, a convocação sai da lista.""")
    @ApiResponse(responseCode = "200", description = "As convocações ativas; lista vazia se não houver nenhuma.",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = StudentSummonsResponseDTO.class))))
    List<StudentSummonsResponseDTO> mine(@Parameter(hidden = true) Jwt token);
}
