package br.ucsal.cade.administration.controller.settings;

import br.ucsal.cade.administration.controller.settings.dto.DeadlineSettingsRequestDTO;
import br.ucsal.cade.administration.controller.settings.dto.DeadlineSettingsResponseDTO;
import br.ucsal.cade.administration.service.settings.DeadlineSettingsService;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.authorization.RequiresRole;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/settings/deadlines")
@RequiresRole(Role.ADMIN)
class DeadlineSettingsController implements DeadlineSettingsApi {

    private final DeadlineSettingsService service;

    DeadlineSettingsController(DeadlineSettingsService service) {
        this.service = service;
    }

    @Override
    @GetMapping
    public DeadlineSettingsResponseDTO current() {
        return DeadlineSettingsResponseDTO.from(service.current());
    }

    @Override
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public DeadlineSettingsResponseDTO update(@Valid @RequestBody DeadlineSettingsRequestDTO request,
                                              @AuthenticationPrincipal Jwt token) {
        return DeadlineSettingsResponseDTO.from(service.update(request.toValues(),
                UUID.fromString(token.getSubject())));
    }
}
