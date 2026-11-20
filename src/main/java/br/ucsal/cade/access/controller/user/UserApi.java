package br.ucsal.cade.access.controller.user;

import br.ucsal.cade.access.dto.request.RoleChangeRequestDTO;
import br.ucsal.cade.access.dto.response.UserResponseDTO;
import br.ucsal.cade.shared.security.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/** Documentação OpenAPI da gestão de funcionários (RF03); as rotas ficam no {@link UserController}. */
@Tag(name = "Contas", description = """
        Gestão dos funcionários pelo administrador. O perfil de funcionário é dado a uma conta que já existe, ou seja, \
        a pessoa precisa ter feito login uma vez.""")
public interface UserApi {

    @Operation(
            summary = "Listar contas",
            description = """
                    Contas em páginas, ordenadas por e-mail. Os filtros são opcionais e se combinam: `role` traz só \
                    um perfil, e `email` traz as contas cujo e-mail contém o trecho, sem diferenciar maiúsculas.""")
    @Parameter(name = "page", in = ParameterIn.QUERY, description = "Número da página, a partir de 0.",
            schema = @Schema(type = "integer", minimum = "0", defaultValue = "0"))
    @Parameter(name = "size", in = ParameterIn.QUERY, description = "Contas por página.",
            schema = @Schema(type = "integer", minimum = "1", defaultValue = "20"))
    @ApiResponse(responseCode = "200", description = "Uma página de contas.")
    PagedModel<UserResponseDTO> search(
            @Parameter(description = "Perfil das contas.", example = "STAFF") Role role,
            @Parameter(description = "Trecho do e-mail.", example = "fulano") String email,
            @Parameter(hidden = true) Pageable pageable);

    @Operation(
            summary = "Dar ou retirar o perfil de funcionário",
            description = """
                    `STAFF` dá o perfil de funcionário a um estudante; `STUDENT` retira o perfil de um funcionário. \
                    A conta do administrador não muda por aqui. A mudança fica no histórico com o motivo.

                    O perfil novo vale a partir do próximo login da pessoa: o token que ela já tem continua com o \
                    perfil antigo até vencer (8 horas).""")
    @ApiResponse(responseCode = "200", description = "Perfil alterado.",
            content = @Content(schema = @Schema(implementation = UserResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Perfil ausente ou `ADMIN`, ou motivo ausente ou longo demais.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Conta não encontrada.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409",
            description = "A conta já tem o perfil pedido, ou é a conta do administrador.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Já é funcionário", value = """
                            {
                              "type": "about:blank",
                              "title": "Conflict",
                              "status": 409,
                              "detail": "Esta conta já tem o perfil de funcionário.",
                              "instance": "/users/6f1c2a1e-0000-4000-8000-000000000002/role"
                            }""")))
    UserResponseDTO changeRole(UUID id, RoleChangeRequestDTO request, @Parameter(hidden = true) Jwt token);
}
