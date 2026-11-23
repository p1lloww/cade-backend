package br.ucsal.cade.demo.controller.login.dto;

import br.ucsal.cade.demo.service.DemoAccount;
import br.ucsal.cade.shared.security.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Uma conta fictícia do modo demonstração.")
public record DemoAccountResponseDTO(
        @Schema(description = "Perfil da conta.", example = "STUDENT")
        Role role,
        @Schema(description = "E-mail fictício.", example = "estudante.demo@ucsal.edu.br")
        String email,
        @Schema(description = "Nome para mostrar na tela de escolha.", example = "Estudante de demonstração")
        String label) {

    public static DemoAccountResponseDTO from(DemoAccount account) {
        return new DemoAccountResponseDTO(account.role(), account.email(), account.label());
    }
}
