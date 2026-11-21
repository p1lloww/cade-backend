package br.ucsal.cade.pickup.controller.counter;

import br.ucsal.cade.pickup.controller.counter.dto.CounterSummonsResponseDTO;
import br.ucsal.cade.pickup.controller.counter.dto.PickupRequestDTO;
import br.ucsal.cade.pickup.controller.counter.dto.PickupResponseDTO;
import br.ucsal.cade.pickup.controller.counter.dto.RefusalRequestDTO;
import br.ucsal.cade.pickup.controller.counter.dto.RefusalResponseDTO;
import br.ucsal.cade.pickup.service.counter.CounterService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequiresRole(Role.STAFF)
class CounterController implements CounterApi {

    private final CounterService service;

    CounterController(CounterService service) {
        this.service = service;
    }

    @Override
    @GetMapping("/summons")
    public List<CounterSummonsResponseDTO> active() {
        return service.active().stream().map(CounterSummonsResponseDTO::from).toList();
    }

    @Override
    @GetMapping("/summons/{id}")
    public CounterSummonsResponseDTO find(@PathVariable UUID id) {
        return CounterSummonsResponseDTO.from(service.find(id));
    }

    @Override
    @PostMapping(path = "/summons/{id}/pickup", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PickupResponseDTO> registerPickup(@PathVariable UUID id,
                                                            @Valid @RequestBody PickupRequestDTO request,
                                                            @AuthenticationPrincipal Jwt token) {
        PickupResponseDTO pickup = PickupResponseDTO.from(service.registerPickup(id, request.toCheck(),
                UUID.fromString(token.getSubject())));
        return ResponseEntity.status(HttpStatus.CREATED).body(pickup);
    }

    @Override
    @PostMapping(path = "/summons/{id}/refusal", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RefusalResponseDTO refuse(@PathVariable UUID id, @Valid @RequestBody RefusalRequestDTO request,
                                     @AuthenticationPrincipal Jwt token) {
        return RefusalResponseDTO.from(service.refuse(id, request.reason().strip(),
                UUID.fromString(token.getSubject())));
    }
}
