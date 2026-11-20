package br.ucsal.cade.access.controller.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Troca do código recebido por e-mail por um token de acesso.")
public record TokenRequestDTO(

        @Schema(description = "O mesmo e-mail usado para pedir o código.",
                example = "estudante@ucsal.edu.br", maxLength = 254)
        @NotBlank(message = "Informe o e-mail.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 254, message = "O e-mail é longo demais.")
        String email,

        @Schema(description = "O código de seis dígitos do e-mail.", example = "123456", pattern = "^\\d{6}$")
        @NotBlank(message = "Informe o código.")
        @Pattern(regexp = "\\d{6}", message = "O código tem 6 dígitos.")
        String code) {

    public TokenRequestDTO {
        email = email == null ? null : email.strip();
    }

    @Override
    public String toString() {
        return "TokenRequestDTO[email=" + email + ", code=******]";
    }
}
