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

/** OpenAPI documentation of the passwordless login (RF01, D02, D03); the routes are in {@link AuthController}. */
@Tag(name = "Authentication", description = """
        Passwordless login. The user asks for a one-time code, receives it at the institutional e-mail and \
        exchanges it for an access token (JWT). There is no sign-up: the first login creates a student account.""")
@SecurityRequirements
public interface AuthApi {

    @Operation(
            summary = "Ask for a login code",
            description = """
                    Sends a six-digit code to the e-mail. The code is valid for 10 minutes, can be used once and \
                    replaces any code sent before to the same e-mail. Only @ucsal.edu.br e-mails are accepted, and \
                    each e-mail can ask for one code per minute.

                    The e-mail is sent after the response, so `202` means the code was issued, not delivered.""")
    @ApiResponse(responseCode = "202", description = "Code issued; it will arrive at the e-mail shortly.")
    @ApiResponse(responseCode = "400", description = "E-mail missing, malformed or outside @ucsal.edu.br.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = {
                            @ExampleObject(name = "Outside the domain", value = """
                                    {
                                      "type": "about:blank",
                                      "title": "Bad Request",
                                      "status": 400,
                                      "detail": "Use o seu e-mail institucional @ucsal.edu.br.",
                                      "instance": "/auth/login-code"
                                    }"""),
                            @ExampleObject(name = "Invalid field", value = """
                                    {
                                      "type": "about:blank",
                                      "title": "Bad Request",
                                      "status": 400,
                                      "detail": "Confira os campos enviados.",
                                      "instance": "/auth/login-code",
                                      "errors": { "email": "Informe um e-mail válido." }
                                    }""")}))
    @ApiResponse(responseCode = "429", description = "A code was sent less than a minute ago.",
            headers = @Header(name = "Retry-After", description = "Seconds to wait before asking again.",
                    schema = @Schema(type = "integer", example = "42")),
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = """
                            {
                              "type": "about:blank",
                              "title": "Too Many Requests",
                              "status": 429,
                              "detail": "Um código acabou de ser enviado. Aguarde um minuto para pedir outro.",
                              "instance": "/auth/login-code"
                            }""")))
    ResponseEntity<Void> requestLoginCode(LoginCodeRequest request);

    @Operation(
            summary = "Exchange the code for an access token",
            description = """
                    Checks the code sent to the e-mail and returns a JWT valid for 8 hours, carrying the account id \
                    and role. On the first login, the account is created as a student.

                    Every code allows 5 attempts; after that, a new code must be requested. A wrong, expired, \
                    already used or locked code gets the same answer, so it is not possible to tell them apart.""")
    @ApiResponse(responseCode = "200", description = "Code accepted.",
            content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid fields, or the code is wrong, expired, used or locked.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = {
                            @ExampleObject(name = "Code refused", value = """
                                    {
                                      "type": "about:blank",
                                      "title": "Bad Request",
                                      "status": 400,
                                      "detail": "Código inválido ou expirado. Confira o código ou solicite um novo.",
                                      "instance": "/auth/token"
                                    }"""),
                            @ExampleObject(name = "Invalid field", value = """
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
