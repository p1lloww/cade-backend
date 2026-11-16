package br.ucsal.cade.access.controller.auth;

import br.ucsal.cade.access.controller.auth.dto.LoginCodeRequest;
import br.ucsal.cade.access.controller.auth.dto.TokenRequest;
import br.ucsal.cade.access.controller.auth.dto.TokenResponse;
import br.ucsal.cade.access.persistence.user.User;
import br.ucsal.cade.access.service.login.LoginService;
import br.ucsal.cade.access.service.token.AccessTokenIssuer;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
class AuthController implements AuthApi {

    private final LoginService loginService;
    private final AccessTokenIssuer tokenIssuer;

    AuthController(LoginService loginService, AccessTokenIssuer tokenIssuer) {
        this.loginService = loginService;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    @PostMapping(path = "/login-code", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> requestLoginCode(@Valid @RequestBody LoginCodeRequest request) {
        loginService.requestCode(request.email());
        return ResponseEntity.accepted().build();
    }

    @Override
    @PostMapping(path = "/token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TokenResponse issueToken(@Valid @RequestBody TokenRequest request) {
        User user = loginService.exchangeCode(request.email(), request.code());
        return TokenResponse.from(tokenIssuer.issue(user));
    }
}
