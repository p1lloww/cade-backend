package br.ucsal.cade.founditems.controller.item;

import br.ucsal.cade.founditems.controller.item.dto.FoundItemBroughtByRequestDTO;
import br.ucsal.cade.founditems.controller.item.dto.FoundItemIdentificationRequestDTO;
import br.ucsal.cade.founditems.controller.item.dto.FoundItemRequestDTO;
import br.ucsal.cade.founditems.controller.item.dto.FoundItemResponseDTO;
import br.ucsal.cade.founditems.controller.item.dto.FoundItemUpdateRequestDTO;
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

    @Operation(
            summary = "Corrigir os dados de um objeto",
            description = """
                    Só para funcionários. Corrige categoria, local, data, caixa, risco e os textos internos de um \
                    objeto (a foto não muda). Envie os mesmos campos do cadastro, o **motivo** e a `version` da \
                    última resposta. Só vale para objeto **Disponível** ou **Não reclamado**: Reservado (há uma \
                    convocação em andamento), Devolvido e Destinado respondem 409. Se o objeto mudou desde que o \
                    funcionário o recebeu (por exemplo, foi reservado no mesmo instante), a resposta é 409 e nada é \
                    gravado. O risco nunca fica abaixo do da categoria (RN04). A correção vai para o histórico com o \
                    motivo e o que mudou (campo, antes e depois); descrição interna e detalhes reservados aparecem só \
                    pelo nome. Se nada mudou, nada é gravado.""")
    @ApiResponse(responseCode = "200", description = "Objeto corrigido, com a nova `version`.",
            content = @Content(schema = @Schema(implementation = FoundItemResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = """
            Campo obrigatório ausente, categoria inexistente, data no futuro, risco menor que o da categoria ou \
            motivo ausente.""",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Objeto não encontrado.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = """
            O objeto não está Disponível nem Não reclamado, ou foi alterado depois da versão enviada.""",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    FoundItemResponseDTO update(@Parameter(description = "Id do objeto.") UUID id, FoundItemUpdateRequestDTO request,
                                @Parameter(hidden = true) Jwt token);

    @Operation(
            summary = "Registrar quem trouxe o objeto",
            description = """
                    Só para funcionários (RF07). Nome e matrícula de quem trouxe, ou "equipe de limpeza". Vazio apaga. \
                    Também pode ser informado no cadastro, no campo `broughtBy`. Só funcionários veem esse dado; o \
                    histórico diz que foi registrado, não quem trouxe.""")
    @ApiResponse(responseCode = "200", description = "Registrado.",
            content = @Content(schema = @Schema(implementation = FoundItemResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Texto longo demais.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Objeto não encontrado.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    FoundItemResponseDTO recordBroughtBy(@Parameter(description = "Id do objeto.") UUID id,
                                         FoundItemBroughtByRequestDTO request, @Parameter(hidden = true) Jwt token);

    @Operation(
            summary = "Marcar o objeto como identificado",
            description = """
                    Só para funcionários (RF08). O funcionário achou no objeto um nome, um e-mail ou um documento: \
                    informe pelo menos um. Vale enquanto o objeto não saiu da sala (Disponível, Reservado ou Não \
                    reclamado); marcar de novo troca o que estava. Só funcionários veem esses dados, e o histórico \
                    diz quais campos vieram, nunca o conteúdo.""")
    @ApiResponse(responseCode = "200", description = "Objeto identificado.",
            content = @Content(schema = @Schema(implementation = FoundItemResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Nenhum dos três dados, e-mail inválido ou texto longo demais.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Objeto não encontrado.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "O objeto já foi devolvido ou destinado.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    FoundItemResponseDTO identify(@Parameter(description = "Id do objeto.") UUID id,
                                  FoundItemIdentificationRequestDTO request, @Parameter(hidden = true) Jwt token);

    @Operation(
            summary = "Desfazer a marcação de identificado",
            description = "Só para funcionários. Apaga os dados de identificação, por exemplo se foram marcados no "
                    + "objeto errado. Sem marcação, não faz nada. Fica no histórico.")
    @ApiResponse(responseCode = "200", description = "Marcação desfeita.",
            content = @Content(schema = @Schema(implementation = FoundItemResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "Objeto não encontrado.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    FoundItemResponseDTO clearIdentification(@Parameter(description = "Id do objeto.") UUID id,
                                             @Parameter(hidden = true) Jwt token);

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
