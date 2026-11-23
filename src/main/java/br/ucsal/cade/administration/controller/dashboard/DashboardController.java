package br.ucsal.cade.administration.controller.dashboard;

import br.ucsal.cade.administration.controller.dashboard.dto.DashboardResponseDTO;
import br.ucsal.cade.administration.service.dashboard.DashboardService;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.authorization.RequiresRole;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiresRole(Role.ADMIN)
class DashboardController implements DashboardApi {

    private final DashboardService service;

    DashboardController(DashboardService service) {
        this.service = service;
    }

    @Override
    @GetMapping
    public DashboardResponseDTO overview() {
        return DashboardResponseDTO.from(service.overview());
    }
}
