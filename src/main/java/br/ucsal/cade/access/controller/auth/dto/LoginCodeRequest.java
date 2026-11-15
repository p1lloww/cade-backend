package br.ucsal.cade.access.controller.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Asks for a login code to be sent to an institutional e-mail.")
public record LoginCodeRequest(

        @Schema(description = "Institutional e-mail (@ucsal.edu.br). Case and surrounding spaces are ignored.",
                example = "estudante@ucsal.edu.br", maxLength = 254)
        @NotBlank(message = "Informe o e-mail.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 254, message = "O e-mail é longo demais.")
        String email) {
}
