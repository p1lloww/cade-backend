package br.ucsal.cade.matching.controller.suggestion;

import br.ucsal.cade.matching.controller.suggestion.dto.SuggestionDiscardRequestDTO;
import br.ucsal.cade.matching.controller.suggestion.dto.SuggestionInfoRequestDTO;
import br.ucsal.cade.matching.controller.suggestion.dto.SuggestionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

/** Documentação OpenAPI das sugestões do cruzamento (RF22 a RF24); as rotas ficam no {@link SuggestionController}. */
@Tag(name = "Sugestões", description = """
        Pares objeto × perda que o cruzamento achou compatíveis (nota média ou alta). A sugestão é apoio: quem decide \
        é o funcionário (RN08). Cada sugestão traz o objeto e a perda lado a lado, inclusive a descrição interna, os \
        detalhes reservados e o detalhe secreto, por isso só funcionários acessam (RN03).""")
public interface SuggestionApi {

    @Operation(
            summary = "Sugestões de um objeto",
            description = """
                    As sugestões ativas do objeto (Nova, Em espera, Convocada), da maior nota para a menor. Lista \
                    vazia se o objeto não tem sugestões ou não existe.""")
    @ApiResponse(responseCode = "200", description = "Sugestões do objeto.")
    List<SuggestionResponseDTO> forFoundItem(@Parameter(description = "Id do objeto achado.") UUID foundItemId);

    @Operation(
            summary = "Sugestões de uma perda",
            description = """
                    As sugestões ativas do registro de perda (Nova, Em espera, Convocada), da maior nota para a \
                    menor. Lista vazia se o registro não tem sugestões ou não existe.""")
    @ApiResponse(responseCode = "200", description = "Sugestões do registro de perda.")
    List<SuggestionResponseDTO> forLossReport(@Parameter(description = "Id do registro de perda.") UUID lossReportId);

    @Operation(summary = "Uma sugestão", description = "A sugestão em qualquer situação, inclusive as finais.")
    @ApiResponse(responseCode = "200", description = "A sugestão.")
    @ApiResponse(responseCode = "404", description = "Sugestão não encontrada.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    SuggestionResponseDTO find(UUID id);

    @Operation(
            summary = "Descartar uma sugestão",
            description = """
                    O funcionário discorda da sugestão (RN08). Só uma sugestão Nova pode ser descartada; ela passa \
                    a Descartada e sai das listas. O motivo vai para o histórico (RF24, RF43).""")
    @ApiResponse(responseCode = "200", description = "Sugestão descartada.",
            content = @Content(schema = @Schema(implementation = SuggestionResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Motivo ausente ou longo demais.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Sugestão não encontrada.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409",
            description = "A sugestão não está Nova, ou outra decisão sobre ela foi gravada ao mesmo tempo.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Já descartada", value = """
                            {
                              "type": "about:blank",
                              "title": "Conflict",
                              "status": 409,
                              "detail": "Não é possível descartar uma sugestão na situação Descartada.",
                              "instance": "/suggestions/6f1c2a1e-0000-4000-8000-000000000003/discard"
                            }""")))
    SuggestionResponseDTO discard(UUID id, SuggestionDiscardRequestDTO request, @Parameter(hidden = true) Jwt token);

    @Operation(
            summary = "Pedir mais informações ao estudante",
            description = """
                    O funcionário não consegue decidir só com o que o estudante escreveu e pergunta mais (RF24). O                     registro de perda passa a **Aguardando informações**, o estudante recebe o pedido por e-mail                     (RF35) e a sugestão continua Nova. Se o estudante não responder em 5 dias, o registro volta a                     Aberto e a sugestão é descartada (RN13). O texto vai ao estudante e ao histórico: não cite o                     objeto achado (RN03). Só uma sugestão Nova admite o pedido, e o registro precisa estar Aberto.""")
    @ApiResponse(responseCode = "200", description = "Pedido feito; o registro está Aguardando informações.",
            content = @Content(schema = @Schema(implementation = SuggestionResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Pedido ausente ou longo demais.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Sugestão não encontrada.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409",
            description = "A sugestão não está Nova, o registro não está Aberto (por exemplo, já Aguardando "
                    + "informações), ou outra alteração foi gravada ao mesmo tempo.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    SuggestionResponseDTO requestInfo(UUID id, SuggestionInfoRequestDTO request, @Parameter(hidden = true) Jwt token);
}
