package br.ucsal.cade.founditems.controller.search;

import br.ucsal.cade.founditems.controller.item.dto.FoundItemResponseDTO;
import br.ucsal.cade.founditems.controller.search.dto.SearchOptionsResponseDTO;
import br.ucsal.cade.founditems.persistence.status.FoundItemStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/** Documentação OpenAPI da busca de objetos (RF10); as rotas ficam no {@link FoundItemSearchController}. */
@Tag(name = "Busca de objetos", description = """
        O funcionário procura objetos por texto e filtros (RF10). O texto é livre; os outros filtros vêm de \
        /found-items/search-options, que só lista valores que têm algum objeto.""")
public interface FoundItemSearchApi {

    @Operation(
            summary = "Buscar objetos",
            description = """
                    Todos os filtros são opcionais e se combinam (o objeto precisa atender a todos). O texto procura \
                    na descrição interna e nos detalhes reservados, com plural e variações ("chaves" acha "chave"); \
                    todas as palavras precisam aparecer, frases vão entre aspas e "-palavra" exclui. Com texto, os \
                    mais relevantes vêm primeiro; sem texto, os achados mais recentemente. Sem situação, valem \
                    todas. A ordem pedida na página é ignorada, e uma página tem no máximo 100 objetos. Cada objeto \
                    traz o photoPath para a miniatura.""")
    @Parameter(name = "page", in = ParameterIn.QUERY, description = "Número da página, a partir de 0.",
            schema = @Schema(type = "integer", minimum = "0", defaultValue = "0"))
    @Parameter(name = "size", in = ParameterIn.QUERY, description = "Objetos por página (no máximo 100).",
            schema = @Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "20"))
    @ApiResponse(responseCode = "200", description = "Uma página de objetos.")
    @ApiResponse(responseCode = "400", description = "Um filtro veio em formato inválido (id, mês ou situação).",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    PagedModel<FoundItemResponseDTO> search(
            @Parameter(description = "Texto livre.", example = "fone preto") String q,
            @Parameter(description = "Id de uma categoria das opções.") UUID categoryId,
            @Parameter(description = "Mês em que o objeto foi achado, das opções.", example = "2026-11",
                    schema = @Schema(type = "string", pattern = "\\d{4}-\\d{2}")) YearMonth month,
            @Parameter(description = "Bloco, das opções.", example = "Bloco 400") String block,
            @Parameter(description = "Sala do bloco, das opções.", example = "Sala 421") String room,
            @Parameter(description = "Uma ou mais situações; repita o parâmetro para várias.",
                    array = @ArraySchema(schema = @Schema(type = "string",
                            allowableValues = {"AVAILABLE", "RESERVED", "RETURNED", "UNCLAIMED", "DISPOSED"})))
            List<FoundItemStatus> status,
            @Parameter(hidden = true) Pageable pageable);

    @Operation(
            summary = "Opções dos filtros da busca",
            description = """
                    As categorias, os blocos com as salas e os meses que têm algum objeto, e todas as situações. O \
                    front monta os seletores com esta resposta e manda na busca só o valor escolhido.""")
    @ApiResponse(responseCode = "200", description = "As opções.")
    SearchOptionsResponseDTO options();
}
