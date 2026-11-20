package br.ucsal.cade.founditems.controller.item;

import br.ucsal.cade.founditems.controller.item.dto.FoundItemRequestDTO;
import br.ucsal.cade.founditems.controller.item.dto.FoundItemResponseDTO;
import br.ucsal.cade.founditems.controller.item.dto.StorageBoxSuggestionResponseDTO;
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

import java.time.LocalDate;
import java.util.UUID;

/** Documentação OpenAPI do cadastro de objetos achados (RF04 a RF06); as rotas ficam no {@link FoundItemController}. */
@Tag(name = "Objetos achados", description = """
        Cadastro rápido pelo celular: foto, categoria, bloco, sala e data. Foto, descrição interna e detalhes \
        reservados só aparecem para funcionários (RN03).""")
public interface FoundItemApi {

    @Operation(
            summary = "Cadastrar um objeto achado",
            description = """
                    Envie como `multipart/form-data`. O objeto entra na situação Disponível. Sem `storageBox`, vale a \
                    caixa do mês da data (RF05); sem `risk`, o risco padrão da categoria, que só pode ser aumentado \
                    (RN04). O cadastro fica no histórico.""")
    @ApiResponse(responseCode = "201", description = "Objeto cadastrado.",
            content = @Content(schema = @Schema(implementation = FoundItemResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = """
            Campo obrigatório ausente, categoria inexistente, data no futuro, risco menor que o da categoria ou \
            foto que não é JPEG, PNG ou WebP.""",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "413", description = "Foto maior que o limite.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<FoundItemResponseDTO> register(FoundItemRequestDTO form, @Parameter(hidden = true) Jwt token);

    @Operation(summary = "Ver um objeto", description = "Todos os dados do objeto, inclusive os reservados.")
    @ApiResponse(responseCode = "200", description = "O objeto.")
    @ApiResponse(responseCode = "404", description = "Objeto não encontrado.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    FoundItemResponseDTO find(UUID id);

    @Operation(summary = "Baixar a foto de um objeto", description = """
            A foto não tem URL pública: o front pede com o token e mostra como blob (D09).""")
    @ApiResponse(responseCode = "200", description = "A foto (JPEG, PNG ou WebP).",
            content = @Content(mediaType = "image/*", schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "404", description = "Objeto ou foto não encontrados.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<byte[]> photo(UUID id);

    @Operation(summary = "Sugerir a caixa do mês", description = """
            A caixa sugerida para a data (RF05), para a tela já preencher o campo. Exemplo: 2026-11-04 → \
            "Caixa Novembro/2026".""")
    StorageBoxSuggestionResponseDTO suggestStorageBox(
            @Parameter(description = "Data em que o objeto foi achado.", example = "2026-11-04") LocalDate date);
}
