package br.ucsal.cade.pickup.controller.summons;

import br.ucsal.cade.pickup.controller.summons.dto.SummonsResponseDTO;
import br.ucsal.cade.pickup.service.summons.SummonsService;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.authorization.RequiresRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiresRole(Role.STAFF)
class SummonsController implements SummonsApi {

    private final SummonsService service;

    SummonsController(SummonsService service) {
        this.service = service;
    }

    @Override
    @PostMapping("/suggestions/{id}/summon")
    public ResponseEntity<SummonsResponseDTO> summon(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        SummonsResponseDTO summons = SummonsResponseDTO.from(service.summon(id, UUID.fromString(token.getSubject())));
        return ResponseEntity.status(HttpStatus.CREATED).body(summons);
    }
}
