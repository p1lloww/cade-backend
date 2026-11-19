package br.ucsal.cade.lossreports.controller.report;

import br.ucsal.cade.lossreports.dto.request.LossReportRequestDTO;
import br.ucsal.cade.lossreports.dto.response.LossReportResponseDTO;
import br.ucsal.cade.lossreports.service.report.LossReportService;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.authorization.RequiresRole;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/loss-reports")
@RequiresRole(Role.STUDENT)
class LossReportController implements LossReportApi {

    private final LossReportService service;

    LossReportController(LossReportService service) {
        this.service = service;
    }

    @Override
    @PostMapping
    public ResponseEntity<LossReportResponseDTO> register(@Valid @RequestBody LossReportRequestDTO request,
                                                          @AuthenticationPrincipal Jwt token) {
        LossReportResponseDTO report = service.register(request, UUID.fromString(token.getSubject()));
        return ResponseEntity.created(URI.create("/loss-reports/" + report.id())).body(report);
    }
}
