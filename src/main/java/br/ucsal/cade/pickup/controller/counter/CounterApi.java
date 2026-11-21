package br.ucsal.cade.pickup.controller.counter;

import br.ucsal.cade.pickup.controller.counter.dto.CounterSummonsResponseDTO;
import br.ucsal.cade.pickup.controller.counter.dto.PickupRequestDTO;
import br.ucsal.cade.pickup.controller.counter.dto.PickupResponseDTO;
import br.ucsal.cade.pickup.controller.counter.dto.RefusalRequestDTO;
import br.ucsal.cade.pickup.controller.counter.dto.RefusalResponseDTO;
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

import java.util.List;
import java.util.UUID;

/** Documentação OpenAPI do balcão (RF28, RF29, RF31); as rotas ficam no {@link CounterController}. */
@Tag(name = "Balcão", description = """
        O estudante convocado vai à sala. O funcionário vê o que conferir conforme o risco do objeto (RN09) e registra \
        a retirada ou, se a verificação falhar, a recusa. Nenhuma entrega sem documento com foto (RN10).""")
public interface CounterApi {

    @Operation(
            summary = "Convocações ativas",
            description = "As convocações à espera de retirada, das que vencem primeiro para as últimas, cada uma com "
                    + "o que conferir.")
    @ApiResponse(responseCode = "200", description = "Convocações ativas.")
    List<CounterSummonsResponseDTO> active();

    @Operation(
            summary = "Uma convocação, com o que conferir",
            description = """
                    A convocação em qualquer situação, com o checklist e as verificações que bastam para o risco do \
                    objeto (RF28): baixo pede descrição coerente; médio, um detalhe que não aparece na foto; alto, \
                    desbloqueio ou prova (nota fiscal, número de série); documento, só ao titular.""")
    @ApiResponse(responseCode = "200", description = "A convocação.")
    @ApiResponse(responseCode = "404", description = "Convocação não encontrada.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    CounterSummonsResponseDTO find(@Parameter(description = "Id da convocação.") UUID id);

    @Operation(
            summary = "Registrar a retirada",
            description = """
                    O funcionário registra documento, matrícula e a verificação feita (RF29). A verificação precisa \
                    ser uma das aceitas para o risco do objeto. Na mesma operação, a convocação passa a Concluída, o \
                    objeto a Devolvido, o registro de perda a Concluído e a sugestão a Concluída, tudo no histórico \
                    (RF43). Só uma convocação Ativa pode ser retirada.""")
    @ApiResponse(responseCode = "201", description = "Retirada registrada.",
            content = @Content(schema = @Schema(implementation = PickupResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = """
            Campo ausente ou inválido, ou a verificação não basta para o risco do objeto.""",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Verificação insuficiente", value = """
                            {
                              "type": "about:blank",
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "Essa verificação não basta para um objeto de risco Alto. Vale: Desbloqueio \
                            do aparelho na frente do funcionário ou Prova de que o objeto é dele (nota fiscal ou \
                            número de série).",
                              "instance": "/summons/6f1c2a1e-0000-4000-8000-000000000003/pickup"
                            }""")))
    @ApiResponse(responseCode = "404", description = "Convocação não encontrada.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409",
            description = "A convocação não está Ativa, ou outra decisão sobre ela foi gravada ao mesmo tempo.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Já recusada", value = """
                            {
                              "type": "about:blank",
                              "title": "Conflict",
                              "status": 409,
                              "detail": "Não é possível concluir uma convocação na situação Recusada.",
                              "instance": "/summons/6f1c2a1e-0000-4000-8000-000000000003/pickup"
                            }""")))
    ResponseEntity<PickupResponseDTO> registerPickup(@Parameter(description = "Id da convocação.") UUID id,
                                                     PickupRequestDTO request, @Parameter(hidden = true) Jwt token);

    @Operation(
            summary = "Registrar a recusa",
            description = """
                    A verificação falhou no balcão (RF31). A convocação passa a Recusada, o objeto volta a \
                    Disponível, o registro de perda a Aberto e a sugestão a Recusada; as sugestões Em espera do \
                    mesmo objeto voltam a Nova (RN18). A recusa conta para o estudante: com 3, ele não abre novos \
                    registros até o administrador liberar (RN12). O motivo vai para o histórico (RF43).""")
    @ApiResponse(responseCode = "200", description = "Recusa registrada.",
            content = @Content(schema = @Schema(implementation = RefusalResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Motivo ausente ou longo demais.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Convocação não encontrada.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409",
            description = "A convocação não está Ativa, ou outra decisão sobre ela foi gravada ao mesmo tempo.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    RefusalResponseDTO refuse(@Parameter(description = "Id da convocação.") UUID id, RefusalRequestDTO request,
                              @Parameter(hidden = true) Jwt token);
}
