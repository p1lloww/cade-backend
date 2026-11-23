package br.ucsal.cade.pickup.controller.student;

import br.ucsal.cade.pickup.controller.student.dto.StudentPickupResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

/** Documentação OpenAPI da confirmação da retirada pelo estudante (RF30); as rotas ficam no {@link StudentPickupController}. */
@Tag(name = "Retirada")
public interface StudentPickupApi {

    @Operation(
            summary = "Listar as minhas retiradas",
            description = """
                    Só para estudantes. As retiradas que o funcionário registrou no balcão para quem está logado, as \
                    mais recentes primeiro, cada uma com a confirmação do estudante, se houver (RF30). Não traz nada \
                    do objeto (RN03). O front mostra as ainda não confirmadas como "Confirme que você retirou".""")
    @ApiResponse(responseCode = "200", description = "As retiradas; lista vazia se não houver nenhuma.",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = StudentPickupResponseDTO.class))))
    List<StudentPickupResponseDTO> mine(@Parameter(hidden = true) Jwt token);

    @Operation(
            summary = "Confirmar a retirada no celular",
            description = """
                    Só para estudantes, só na própria retirada. O estudante confirma o que o funcionário registrou no \
                    balcão, no lugar da assinatura no papel (RF30). A confirmação vai para o histórico e não altera a \
                    retirada. Confirmar duas vezes responde 409.""")
    @ApiResponse(responseCode = "200", description = "Retirada confirmada.",
            content = @Content(schema = @Schema(implementation = StudentPickupResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "A retirada não existe ou é de outro estudante.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "A retirada já foi confirmada.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    StudentPickupResponseDTO confirm(@Parameter(description = "Id da retirada.") UUID id,
                                     @Parameter(hidden = true) Jwt token);
}
