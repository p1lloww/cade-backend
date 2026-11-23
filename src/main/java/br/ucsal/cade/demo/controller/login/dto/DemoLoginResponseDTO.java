package br.ucsal.cade.demo.controller.login.dto;

import br.ucsal.cade.access.api.SignedIn;
import br.ucsal.cade.shared.security.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token de uma conta de demonstração, no mesmo formato do `POST /auth/token`.")
public record DemoLoginResponseDTO(
        @Schema(description = "JWT a enviar como `Authorization: Bearer <accessToken>`.")
        String accessToken,
        @Schema(description = "Sempre `Bearer`.", example = "Bearer")
        String tokenType,
        @Schema(description = "Segundos até o token vencer.", example = "28800")
        long expiresIn,
        @Schema(description = "E-mail fictício da conta.", example = "funcionario.demo@ucsal.edu.br")
        String email,
        @Schema(description = "Perfil da conta.", example = "STAFF")
        Role role) {

    private static final String BEARER = "Bearer";

    public static DemoLoginResponseDTO from(SignedIn signedIn) {
        return new DemoLoginResponseDTO(signedIn.token(), BEARER, signedIn.expiresIn().toSeconds(),
                signedIn.email(), signedIn.role());
    }

    @Override
    public String toString() {
        return "DemoLoginResponseDTO[accessToken=******, email=" + email + ", role=" + role + "]";
    }
}
