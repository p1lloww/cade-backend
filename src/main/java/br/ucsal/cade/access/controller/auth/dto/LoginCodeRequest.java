package br.ucsal.cade.access.controller.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Pedido de envio de um código de login ao e-mail institucional.")
public record LoginCodeRequest(

        @Schema(description = "E-mail institucional (@ucsal.edu.br). Maiúsculas e espaços nas pontas são ignorados.",
                example = "estudante@ucsal.edu.br", maxLength = 254)
        @NotBlank(message = "Informe o e-mail.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 254, message = "O e-mail é longo demais.")
        String email) {

    public LoginCodeRequest {
        email = email == null ? null : email.strip();
    }
}
