package br.ucsal.cade.demo.controller.login.dto;

import br.ucsal.cade.shared.security.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "O perfil da conta de demonstração com que entrar.")
public record DemoLoginRequestDTO(

        @Schema(description = "`STUDENT`, `STAFF` ou `ADMIN`.", example = "STAFF")
        @NotNull(message = "Informe o perfil.")
        Role role) {
}
