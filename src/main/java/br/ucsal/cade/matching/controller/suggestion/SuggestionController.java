package br.ucsal.cade.matching.controller.suggestion;

import br.ucsal.cade.matching.controller.suggestion.dto.SuggestionDiscardRequestDTO;
import br.ucsal.cade.matching.controller.suggestion.dto.SuggestionResponseDTO;
import br.ucsal.cade.matching.service.suggestion.SuggestionService;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.authorization.RequiresRole;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
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
class SuggestionController implements SuggestionApi {

    private final SuggestionService service;

    SuggestionController(SuggestionService service) {
        this.service = service;
    }

    @Override
    @GetMapping("/found-items/{foundItemId}/suggestions")
    public List<SuggestionResponseDTO> forFoundItem(@PathVariable UUID foundItemId) {
        return service.forFoundItem(foundItemId).stream().map(SuggestionResponseDTO::from).toList();
    }

    @Override
    @GetMapping("/loss-reports/{lossReportId}/suggestions")
    public List<SuggestionResponseDTO> forLossReport(@PathVariable UUID lossReportId) {
        return service.forLossReport(lossReportId).stream().map(SuggestionResponseDTO::from).toList();
    }

    @Override
    @GetMapping("/suggestions/{id}")
    public SuggestionResponseDTO find(@PathVariable UUID id) {
        return SuggestionResponseDTO.from(service.find(id));
    }

    @Override
    @PostMapping(path = "/suggestions/{id}/discard", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SuggestionResponseDTO discard(@PathVariable UUID id, @Valid @RequestBody SuggestionDiscardRequestDTO request,
                                         @AuthenticationPrincipal Jwt token) {
        return SuggestionResponseDTO.from(service.discard(id, request.reason(), UUID.fromString(token.getSubject())));
    }
}
