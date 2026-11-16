package br.ucsal.cade.access.controller.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Exchanges the code received by e-mail for an access token.")
public record TokenRequest(

        @Schema(description = "The same e-mail used to ask for the code.",
                example = "estudante@ucsal.edu.br", maxLength = 254)
        @NotBlank(message = "Informe o e-mail.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 254, message = "O e-mail é longo demais.")
        String email,

        @Schema(description = "The six-digit code from the e-mail.", example = "123456", pattern = "^\\d{6}$")
        @NotBlank(message = "Informe o código.")
        @Pattern(regexp = "\\d{6}", message = "O código tem 6 dígitos.")
        String code) {

    public TokenRequest {
        email = email == null ? null : email.strip();
    }

    @Override
    public String toString() {
        return "TokenRequest[email=" + email + ", code=******]";
    }
}
