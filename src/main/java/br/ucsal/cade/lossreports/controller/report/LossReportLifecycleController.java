package br.ucsal.cade.lossreports.controller.report;

import br.ucsal.cade.lossreports.controller.report.dto.LossReportAnswerRequestDTO;
import br.ucsal.cade.lossreports.controller.report.dto.LossReportResponseDTO;
import br.ucsal.cade.lossreports.service.report.LossReportLifecycleService;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.authorization.RequiresRole;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/loss-reports")
@RequiresRole(Role.STUDENT)
class LossReportLifecycleController implements LossReportLifecycleApi {

    private final LossReportLifecycleService service;

    LossReportLifecycleController(LossReportLifecycleService service) {
        this.service = service;
    }

    @Override
    @PostMapping("/{id}/cancel")
    public LossReportResponseDTO cancel(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return LossReportResponseDTO.from(service.cancel(id, UUID.fromString(token.getSubject())));
    }

    @Override
    @PostMapping(path = "/{id}/info-response", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LossReportResponseDTO respond(@PathVariable UUID id, @Valid @RequestBody LossReportAnswerRequestDTO answer,
                                         @AuthenticationPrincipal Jwt token) {
        return LossReportResponseDTO.from(service.respond(id, answer.answer(), UUID.fromString(token.getSubject())));
    }

    @Override
    @PostMapping("/{id}/renew")
    public LossReportResponseDTO renew(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return LossReportResponseDTO.from(service.renew(id, UUID.fromString(token.getSubject())));
    }
}
