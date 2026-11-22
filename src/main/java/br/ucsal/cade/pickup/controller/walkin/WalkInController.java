package br.ucsal.cade.pickup.controller.walkin;

import br.ucsal.cade.pickup.controller.walkin.dto.WalkInCheckResponseDTO;
import br.ucsal.cade.pickup.controller.walkin.dto.WalkInPickupRequestDTO;
import br.ucsal.cade.pickup.controller.walkin.dto.WalkInPickupResponseDTO;
import br.ucsal.cade.pickup.service.walkin.WalkInService;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.authorization.RequiresRole;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiresRole(Role.STAFF)
class WalkInController implements WalkInApi {

    private final WalkInService service;

    WalkInController(WalkInService service) {
        this.service = service;
    }

    @Override
    @GetMapping("/found-items/{id}/walk-in-check")
    public WalkInCheckResponseDTO check(@PathVariable UUID id) {
        return WalkInCheckResponseDTO.from(service.check(id));
    }

    @Override
    @PostMapping(path = "/walk-in-pickups", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WalkInPickupResponseDTO> register(@Valid @RequestBody WalkInPickupRequestDTO request,
                                                            @AuthenticationPrincipal Jwt token) {
        WalkInPickupResponseDTO pickup = WalkInPickupResponseDTO.from(service.register(request.foundItemId(),
                request.toHolder(), request.toCheck(), UUID.fromString(token.getSubject())));
        return ResponseEntity.status(HttpStatus.CREATED).body(pickup);
    }
}
