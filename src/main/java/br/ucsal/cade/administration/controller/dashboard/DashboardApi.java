package br.ucsal.cade.administration.controller.dashboard;

import br.ucsal.cade.administration.controller.dashboard.dto.DashboardResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Documentação OpenAPI do painel (RF46); a rota fica no {@link DashboardController}. */
@Tag(name = "Painel (administração)", description = """
        Contagens do serviço para o administrador: objetos por situação e categoria, devoluções no mês, tempo médio \
        até a devolução e não reclamados.""")
public interface DashboardApi {

    @Operation(
            summary = "Painel com as contagens",
            description = """
                    Só para o administrador. Só números: nenhuma foto, descrição, local nem dado de pessoa. As \
                    devoluções e o tempo médio saem do histórico (RF43): o mês é o corrente no fuso de Salvador e a \
                    média conta todas as devoluções registradas (nula enquanto não houve nenhuma).""")
    @ApiResponse(responseCode = "200", description = "As contagens de agora.",
            content = @Content(schema = @Schema(implementation = DashboardResponseDTO.class)))
    DashboardResponseDTO overview();
}
