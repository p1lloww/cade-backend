package br.ucsal.cade.publicpage.controller.counts;

import br.ucsal.cade.publicpage.controller.counts.dto.PublicCountsResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Documentação OpenAPI das contagens públicas (RF41); a rota fica no {@link PublicCountsController}. */
@Tag(name = "Página pública", description = "Rotas sem login, para divulgar a sala de achados e perdidos.")
public interface PublicCountsApi {

    @Operation(
            summary = "Contagens dos objetos disponíveis",
            description = """
                    Quantos objetos estão na sala e podem ser retirados (Disponível ou Não reclamado), por mês em \
                    que foram achados e por categoria, como "Novembro/2026: 12 garrafas, 5 fones". Não traz foto, \
                    descrição nem local de nenhum objeto, para ninguém "pescar" um objeto pelo que viu (RN03). Não \
                    precisa de login.""")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "As contagens.")
    PublicCountsResponseDTO counts();
}
