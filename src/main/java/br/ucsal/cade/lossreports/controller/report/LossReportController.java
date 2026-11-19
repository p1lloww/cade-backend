package br.ucsal.cade.lossreports.controller.report;

import br.ucsal.cade.lossreports.dto.request.LossReportRequestDTO;
import br.ucsal.cade.lossreports.dto.response.LossReportResponseDTO;
import br.ucsal.cade.lossreports.service.report.LossReportService;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.authorization.RequiresRole;
import br.ucsal.cade.shared.storage.StoredPhoto;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LossReportResponseDTO> register(@Valid @RequestPart("report") LossReportRequestDTO report,
                                                          @RequestPart(name = "photo", required = false)
                                                          MultipartFile photo,
                                                          @AuthenticationPrincipal Jwt token) {
        LossReportResponseDTO registered = service.register(report, photo, UUID.fromString(token.getSubject()));
        return ResponseEntity.created(URI.create("/loss-reports/" + registered.id())).body(registered);
    }

    @Override
    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> photo(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        StoredPhoto photo = service.photoOf(id, UUID.fromString(token.getSubject()));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.type().mediaType()))
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(photo.content());
    }
}
