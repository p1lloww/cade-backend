package br.ucsal.cade.access.controller.auth;

import br.ucsal.cade.access.controller.auth.dto.LoginCodeRequest;
import br.ucsal.cade.access.controller.auth.dto.TokenRequest;
import br.ucsal.cade.access.controller.auth.dto.TokenResponse;
import br.ucsal.cade.access.persistence.user.User;
import br.ucsal.cade.access.service.login.LoginService;
import br.ucsal.cade.access.service.token.AccessTokenIssuer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AuthController implements AuthApi {

    private final LoginService loginService;
    private final AccessTokenIssuer tokenIssuer;

    AuthController(LoginService loginService, AccessTokenIssuer tokenIssuer) {
        this.loginService = loginService;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    public ResponseEntity<Void> requestLoginCode(LoginCodeRequest request) {
        loginService.requestCode(request.email());
        return ResponseEntity.accepted().build();
    }

    @Override
    public TokenResponse issueToken(TokenRequest request) {
        User user = loginService.exchangeCode(request.email(), request.code());
        return TokenResponse.from(tokenIssuer.issue(user));
    }
}
