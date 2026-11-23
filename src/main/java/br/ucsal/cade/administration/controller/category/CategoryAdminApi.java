package br.ucsal.cade.administration.controller.category;

import br.ucsal.cade.administration.controller.category.dto.CategoryRequestDTO;
import br.ucsal.cade.shared.category.dto.CategoryResponseDTO;
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

/** Documentação OpenAPI da gestão de categorias (RF44); as rotas ficam no {@link CategoryAdminController}. */
@Tag(name = "Categorias (administração)", description = """
        O administrador cria categorias e ajusta nome, risco padrão e perguntas de conferência. A lista fica em \
        `GET /categories`. Não há exclusão: objetos e perdas já registrados apontam para a categoria.""")
public interface CategoryAdminApi {

    @Operation(
            summary = "Criar uma categoria",
            description = """
                    Só para o administrador. Nome único (sem diferenciar maiúsculas), risco padrão e até 10 \
                    perguntas de conferência. Vai para o histórico (RF43).""")
    @ApiResponse(responseCode = "201", description = "Categoria criada.",
            content = @Content(schema = @Schema(implementation = CategoryResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Nome em branco, pergunta em branco ou limites ultrapassados.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Já existe uma categoria com esse nome.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    CategoryResponseDTO create(CategoryRequestDTO request, @Parameter(hidden = true) Jwt token);

    @Operation(
            summary = "Alterar uma categoria",
            description = """
                    Só para o administrador. Troca nome, risco padrão e perguntas de conferência. O risco novo vale \
                    para os próximos objetos; os que já estão na sala guardam o risco que tinham (RN04). Vai para o \
                    histórico (RF43).""")
    @ApiResponse(responseCode = "200", description = "Categoria alterada.",
            content = @Content(schema = @Schema(implementation = CategoryResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Nome em branco, pergunta em branco ou limites ultrapassados.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Categoria não encontrada.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Já existe outra categoria com esse nome.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    CategoryResponseDTO update(@Parameter(description = "Id da categoria.") UUID id, CategoryRequestDTO request,
                               @Parameter(hidden = true) Jwt token);
}
