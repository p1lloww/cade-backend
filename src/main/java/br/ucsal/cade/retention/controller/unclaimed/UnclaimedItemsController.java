package br.ucsal.cade.retention.controller.unclaimed;

import br.ucsal.cade.retention.controller.unclaimed.dto.DisposalRequestDTO;
import br.ucsal.cade.retention.controller.unclaimed.dto.DisposalResponseDTO;
import br.ucsal.cade.retention.controller.unclaimed.dto.UnclaimedBoxResponseDTO;
import br.ucsal.cade.retention.service.DisposalService;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.authorization.RequiresRole;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/unclaimed-items")
class UnclaimedItemsController implements UnclaimedItemsApi {

    private final DisposalService service;

    UnclaimedItemsController(DisposalService service) {
        this.service = service;
    }

    @Override
    @GetMapping
    @RequiresRole(Role.STAFF)
    public List<UnclaimedBoxResponseDTO> byBox() {
        return service.listByBox().stream().map(UnclaimedBoxResponseDTO::from).toList();
    }

    @Override
    @PostMapping(path = "/{id}/disposal", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresRole(Role.ADMIN)
    public DisposalResponseDTO dispose(@PathVariable UUID id, @Valid @RequestBody DisposalRequestDTO request,
                                       @AuthenticationPrincipal Jwt token) {
        return DisposalResponseDTO.from(service.dispose(id, request.destination(), request.note(),
                UUID.fromString(token.getSubject())));
    }
}
