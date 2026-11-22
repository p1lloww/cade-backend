package br.ucsal.cade.pickup.controller.student;

import br.ucsal.cade.pickup.controller.student.dto.StudentSummonsResponseDTO;
import br.ucsal.cade.pickup.service.summons.SummonsService;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.authorization.RequiresRole;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiresRole(Role.STUDENT)
class StudentSummonsController implements StudentSummonsApi {

    private final SummonsService service;

    StudentSummonsController(SummonsService service) {
        this.service = service;
    }

    @Override
    @GetMapping("/summons/mine")
    public List<StudentSummonsResponseDTO> mine(@AuthenticationPrincipal Jwt token) {
        return service.activeOf(UUID.fromString(token.getSubject())).stream()
                .map(StudentSummonsResponseDTO::from)
                .toList();
    }
}
