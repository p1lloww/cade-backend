package br.ucsal.cade.demo.controller.login;

import br.ucsal.cade.demo.controller.login.dto.DemoAccountResponseDTO;
import br.ucsal.cade.demo.controller.login.dto.DemoLoginRequestDTO;
import br.ucsal.cade.demo.controller.login.dto.DemoLoginResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

import java.util.List;

/** Documentação OpenAPI do login direto do modo demonstração (RF47); as rotas ficam no {@link DemoLoginController}. */
@Tag(name = "Demonstração", description = """
        Só existe quando a aplicação sobe com o profile `demo`; fora dele, estas rotas respondem 404. Entra com uma \
        conta fictícia sem código de e-mail, para a demonstração funcionar sem internet.""")
@SecurityRequirements
public interface DemoApi {

    @Operation(
            summary = "Listar as contas de demonstração",
            description = "Uma conta fictícia por perfil, para a tela de escolha do modo demonstração.")
    @ApiResponse(responseCode = "200", description = "As contas de demonstração.")
    List<DemoAccountResponseDTO> accounts();

    @Operation(
            summary = "Entrar direto com uma conta de demonstração",
            description = """
                    Devolve o token da conta fictícia do perfil pedido, sem código de e-mail. O token é igual ao do \
                    `POST /auth/token` e vale nas mesmas rotas.""")
    @ApiResponse(responseCode = "200", description = "Token emitido.")
    @ApiResponse(responseCode = "400", description = "Perfil ausente ou inválido.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    DemoLoginResponseDTO login(DemoLoginRequestDTO request);
}
