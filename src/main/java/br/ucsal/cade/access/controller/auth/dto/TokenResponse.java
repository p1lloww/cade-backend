package br.ucsal.cade.access.controller.auth.dto;

import br.ucsal.cade.access.service.token.AccessToken;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Access token to send as `Authorization: Bearer <accessToken>` on protected routes.")
public record TokenResponse(

        @Schema(description = "Signed JWT carrying the account id (`sub`), e-mail and role.",
                example = "<signed JWT>")
        String accessToken,

        @Schema(description = "Always `Bearer`.", example = "Bearer")
        String tokenType,

        @Schema(description = "Seconds until the token expires.", example = "28800")
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
