package br.ucsal.cade.access.controller.auth;

import br.ucsal.cade.access.controller.auth.dto.LoginCodeRequest;
import br.ucsal.cade.access.controller.auth.dto.TokenRequest;
import br.ucsal.cade.access.controller.auth.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

/** Documentação OpenAPI do login sem senha (RF01, D02, D03); as rotas ficam no {@link AuthController}. */
@Tag(name = "Autenticação", description = """
        Login sem senha. O usuário pede um código de uso único, recebe-o no e-mail institucional e o troca por um \
        token de acesso (JWT). Não há cadastro: o primeiro login cria uma conta de estudante.""")
@SecurityRequirements
public interface AuthApi {

    @Operation(
            summary = "Pedir um código de login",
            description = """
                    Envia um código de seis dígitos ao e-mail. O código vale por 10 minutos, pode ser usado uma vez \
                    e substitui qualquer código enviado antes ao mesmo e-mail. Só e-mails @ucsal.edu.br são \
                    aceitos, e cada e-mail pode pedir um código por minuto. Depois de 15 códigos errados seguidos \
                    (3 códigos travados), o e-mail fica bloqueado por 15 minutos.

                    O e-mail é enviado depois da resposta, então `202` quer dizer que o código foi gerado, não que \
                    chegou.""")
    @ApiResponse(responseCode = "202", description = "Código gerado; ele chega ao e-mail em instantes.")
    @ApiResponse(responseCode = "400", description = "E-mail ausente, mal formatado ou fora do @ucsal.edu.br.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = {
                            @ExampleObject(name = "Fora do domínio", value = """
                                    {
                                      "type": "about:blank",
                                      "title": "Bad Request",
                                      "status": 400,
                                      "detail": "Use o seu e-mail institucional @ucsal.edu.br.",
                                      "instance": "/auth/login-code"
                                    }"""),
                            @ExampleObject(name = "Campo inválido", value = """
                                    {
                                      "type": "about:blank",
                                      "title": "Bad Request",
                                      "status": 400,
                                      "detail": "Confira os campos enviados.",
                                      "instance": "/auth/login-code",
                                      "errors": { "email": "Informe um e-mail válido." }
                                    }""")}))
    @ApiResponse(responseCode = "429",
            description = "Um código foi enviado há menos de um minuto, ou o e-mail está bloqueado por códigos "
                    + "errados demais.",
            headers = @Header(name = "Retry-After", description = "Segundos a esperar antes de pedir de novo.",
                    schema = @Schema(type = "integer", example = "42")),
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = {
                            @ExampleObject(name = "Espera entre envios", value = """
                                    {
                                      "type": "about:blank",
                                      "title": "Too Many Requests",
                                      "status": 429,
                                      "detail": "Um código acabou de ser enviado. Aguarde um minuto para pedir outro.",
                                      "instance": "/auth/login-code"
                                    }"""),
                            @ExampleObject(name = "Bloqueado", value = """
                                    {
                                      "type": "about:blank",
                                      "title": "Too Many Requests",
                                      "status": 429,
                                      "detail": "Muitas tentativas com código errado. Aguarde alguns minutos para pedir um novo código.",
                                      "instance": "/auth/login-code"
                                    }""")}))
    ResponseEntity<Void> requestLoginCode(LoginCodeRequest request);

    @Operation(
            summary = "Trocar o código por um token de acesso",
            description = """
                    Confere o código enviado ao e-mail e devolve um JWT válido por 8 horas, com o id e o perfil da \
                    conta. No primeiro login, a conta é criada como estudante.

                    Cada código permite 5 tentativas; depois disso, é preciso pedir um novo. Um código errado, \
                    vencido, já usado ou travado recebe a mesma resposta, para não ser possível diferenciá-los.""")
    @ApiResponse(responseCode = "200", description = "Código aceito.",
            content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @ApiResponse(responseCode = "400",
            description = "Campos inválidos, ou o código está errado, vencido, já usado ou travado.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = {
                            @ExampleObject(name = "Código recusado", value = """
                                    {
                                      "type": "about:blank",
                                      "title": "Bad Request",
                                      "status": 400,
                                      "detail": "Código inválido ou expirado. Confira o código ou solicite um novo.",
                                      "instance": "/auth/token"
                                    }"""),
                            @ExampleObject(name = "Campo inválido", value = """
                                    {
                                      "type": "about:blank",
                                      "title": "Bad Request",
                                      "status": 400,
                                      "detail": "Confira os campos enviados.",
                                      "instance": "/auth/token",
                                      "errors": { "code": "O código tem 6 dígitos." }
                                    }""")}))
    TokenResponse issueToken(TokenRequest request);
}
