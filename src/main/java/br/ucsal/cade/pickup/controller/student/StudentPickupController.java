package br.ucsal.cade.pickup.controller.student;

import br.ucsal.cade.pickup.controller.student.dto.StudentPickupResponseDTO;
import br.ucsal.cade.pickup.service.confirmation.PickupConfirmationService;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.authorization.RequiresRole;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiresRole(Role.STUDENT)
class StudentPickupController implements StudentPickupApi {

    private final PickupConfirmationService service;

    StudentPickupController(PickupConfirmationService service) {
        this.service = service;
    }

    @Override
    @GetMapping("/pickups/mine")
    public List<StudentPickupResponseDTO> mine(@AuthenticationPrincipal Jwt token) {
        return service.mine(UUID.fromString(token.getSubject())).stream().map(StudentPickupResponseDTO::from).toList();
    }

    @Override
    @PostMapping("/pickups/{id}/confirmation")
    public StudentPickupResponseDTO confirm(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return StudentPickupResponseDTO.from(service.confirm(id, UUID.fromString(token.getSubject())));
    }
}
