package br.ucsal.cade.access.controller.auth.dto;

import br.ucsal.cade.access.service.token.AccessToken;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token de acesso a enviar como `Authorization: Bearer <accessToken>` nas rotas protegidas.")
public record TokenResponse(

        @Schema(description = "JWT assinado com o id da conta (`sub`), o e-mail e o perfil.")
        String accessToken,

        @Schema(description = "Sempre `Bearer`.", example = "Bearer")
        String tokenType,

        @Schema(description = "Segundos até o token vencer.", example = "28800")
        long expiresIn) {

    private static final String BEARER = "Bearer";

    public static TokenResponse from(AccessToken token) {
        return new TokenResponse(token.value(), BEARER, token.expiresIn().toSeconds());
    }

    @Override
    public String toString() {
        return "TokenResponse[accessToken=******, tokenType=" + tokenType + ", expiresIn=" + expiresIn + "]";
    }
}
